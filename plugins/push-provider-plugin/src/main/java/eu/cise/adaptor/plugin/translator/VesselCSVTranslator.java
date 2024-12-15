package eu.cise.adaptor.plugin.translator;


import eu.cise.adaptor.core.common.logging.AdaptorLogger;
import eu.cise.adaptor.core.common.logging.LogConfig;
import eu.cise.adaptor.core.common.logging.LoggerMessage;
import eu.cise.adaptor.core.servicehandler.domain.RegisteredMessage;
import eu.cise.adaptor.plugin.cisecontext.CISEPayloadBuilder;
import eu.cise.adaptor.plugin.cisecontext.VesselBuilder;
import eu.cise.adaptor.plugin.cisecontext.impl.VesselPayloadBuilder;
import eu.cise.servicemodel.v1.message.Acknowledgement;
import eu.cise.servicemodel.v1.message.XmlEntityPayload;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;


/**
 * This class provides functionality over CSV file, in particular:
 * <ul>
 * <li>Read a csv file with the following header: IMO_Number, MMSI, longitude, latitude, position_timestamp, recipientServiceID, contextId, referenceMessageId</li>
 * <li>It stores the line data using the class {@link VesselCSVTranslatorEntry}</li>
 * <li>Checks if all the lines have the correct information to build a CISE Vessel for that line</li>
 * <li>Write a csv file with the original data and CISE information about the transmission, adding these information in the header: generatedMessageId, generatedContextId, ackStatus, ackDetail</li>
 * <li>If there are errors, write a csv file with the original data and the error detail, adding these information in the header: translation_error</li>
 * </ul>
 */
public class VesselCSVTranslator {

    private static final AdaptorLogger logger = LogConfig.configureLogging(VesselCSVTranslator.class);
    public static final String csvHeaderRow = "IMO_Number,MMSI,longitude,latitude,position_timestamp,recipientServiceId,contextId,referenceMessageId";
    private final Map<MessageKey, List<VesselCSVTranslatorEntry>> csvEntries; // keeps the entries grouped by message key in the order they appear
    private final List<VesselCSVTranslatorEntry> errorEntries; // keeps the entries with errors to create the error file
    private final List<VesselCSVTranslatorEntry> originalOrder; // keeps the original order of entries to create output file
    private final VesselBuilder vesselPayloadBuilder;
    private final CISEPayloadBuilder cisePayloadBuilder;

    public VesselCSVTranslator() {
        csvEntries = new LinkedHashMap<>();
        errorEntries = new ArrayList<>();
        originalOrder = new ArrayList<>();
        // Provide the VesselBuilder and the CISEPayloadBuilder. In our example they are implemented by the same class VesselPayloadBuilder
        vesselPayloadBuilder = new VesselPayloadBuilder();
        cisePayloadBuilder = new VesselPayloadBuilder();
    }

    /**
     * Static constructor of the VesselCSVTranslator instance, that will work on the csv input file
     *
     * @param vesselCSVFile Csv file
     * @return instance of VesselCSVTranslator class
     * @throws FileNotFoundException The file was not found
     */
    public static VesselCSVTranslator vesselCSVToCISE(File vesselCSVFile) throws FileNotFoundException {
        logger.info(LoggerMessage.of("Translating inputFile found: {}", vesselCSVFile.getAbsolutePath()));
        VesselCSVTranslator result = new VesselCSVTranslator();
        int csvLineNumber = 0;
        String currentEntry;

        Scanner scanner = new Scanner(vesselCSVFile);
        while (scanner.hasNextLine()) {
            currentEntry = scanner.nextLine();
            csvLineNumber++;
            if (!currentEntry.startsWith("IMO_Number") && !currentEntry.isBlank()) { // skip the file header line
                result.addCSVEntry(currentEntry, csvLineNumber); // accumulate lines while translating them along the way
            }
        }
        scanner.close();

        return result;
    }

    private void addCSVEntry(String csvEntry, Integer lineNumber) {

        VesselCSVTranslatorEntry entry = new VesselCSVTranslatorEntry(csvEntry, lineNumber, vesselPayloadBuilder::translateToVessel);

        // Add as List to maintain the original order
        originalOrder.add(entry);

        if (entry.getTranslationException() == null) {
            // Add in a map to  associate key -> Entry
            MessageKey messageKey = new MessageKey(entry.getContextId(), entry.getReferenceMessageId(), entry.getRecipientServiceId());
            List<VesselCSVTranslatorEntry> existingEntries = csvEntries.computeIfAbsent(messageKey, k -> new ArrayList<>());
            existingEntries.add(entry);
        } else {
            // Add as List of Entry with errors
            errorEntries.add(entry);
        }
    }

    /**
     * Give back the entries that aren't correct
     *
     * @return List of VesselCSVTranslatorEntry containing the error lines
     */
    public List<VesselCSVTranslatorEntry> getErrorLines() {
        return errorEntries;
    }

    /**
     * Validity of the VesselCSVTranslator instance.
     * @return true if there are lines with errors
     */
    public boolean hasErrors() {
        return !getErrorLines().isEmpty();
    }

    /**
     * Write a csv file with the lines with error. The header is like the original file with one more field( translation_error) with the cause of the error
     *
     * @param errorFile Destination csv file
     * @throws IOException In case of IO Problem during the writing of the file
     */
    public void writeErrorLinesToFile(File errorFile) throws IOException {
        List<VesselCSVTranslatorEntry> errorLines = getErrorLines();
        if (!getErrorLines().isEmpty()) {
            String newLine = System.lineSeparator();
            StringBuilder result = new StringBuilder();
            result.append(csvHeaderRow).append(",translation_error");
            for (VesselCSVTranslatorEntry entry : errorLines) {
                result.append(newLine).append(entry.getCsvEntry()).append(",").append(entry.getTranslationException().getMessage());
            }
            writeStringBuilderToFile(result, errorFile);
        }
    }

    /**
     * Write a csv file with like the original, adding in the end information about the message created and the sending result.
     * To the original header, the following information wil be added: generatedMessageId,generatedContextId,ackStatus,ackDetail
     *
     * @param outputFile Destination csv file
     * @throws IOException n case of IO Problem during the writing of the file
     */
    public void writeOutputFileWithGeneratedMessageDetails(File outputFile) throws IOException {
        if (!originalOrder.isEmpty()) {
            String newLine = System.lineSeparator();
            StringBuilder result = new StringBuilder();
            result.append(csvHeaderRow).append(",generatedMessageId,generatedContextId,ackStatus,ackDetail");
            for (VesselCSVTranslatorEntry entry : originalOrder) {
                result.append(newLine).append(entry.getCsvEntry()).append(",")
                        .append(entry.getGeneratedMessageId()).append(",")
                        .append(entry.getGeneratedContextId()).append(",")
                        .append(entry.getGeneratedAckCode() != null ? entry.getGeneratedAckCode().value() : "Unknown").append(",")
                        .append(entry.getGeneratedAckDetail());
            }
            writeStringBuilderToFile(result, outputFile);
        }
    }

    /**
     * Retrieve all the VesselCSVTranslatorEntry entries payloads, grouped by {@link MessageKey}
     *
     * @return List of couple MessageKey, Payload
     */
    public List<Pair<MessageKey, XmlEntityPayload>> getResultingMessagesPayloads() {
        List<Pair<MessageKey, XmlEntityPayload>> result = new ArrayList<>();

        for (MessageKey messageKey : csvEntries.keySet()) {
            // create the CISE payload
            XmlEntityPayload resultPayload = cisePayloadBuilder.build(csvEntries.get(messageKey));
            Pair<MessageKey, XmlEntityPayload> resultPair = new ImmutablePair<>(messageKey, resultPayload);
            result.add(resultPair);
        }
        return result;
    }

    private void writeStringBuilderToFile(StringBuilder stringBuilder, File outputFile) throws IOException {
        // write output to file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write(stringBuilder.toString());
        }
    }

    /**
     * Update the VesselCSVTranslatorEntry grouped by MessageKey, with the result of CISE message creation and sending
     *
     * @param messageKey MessageKey related to the updated
     * @param messageResult Couple of RegisteredMessage, Acknowledgement result of CISE processing
     */
    public void updateEntriesWithMessageResult(MessageKey messageKey, Pair<RegisteredMessage, Acknowledgement> messageResult) {

        for (VesselCSVTranslatorEntry entry : csvEntries.get(messageKey)) {
            entry.setGeneratedMessageId(messageResult.getLeft().getMessageId());
            entry.setGeneratedContextId(messageResult.getLeft().getContextId());
            entry.setGeneratedAckDetail(messageResult.getRight().getAckDetail());
            entry.setGeneratedAckCode(messageResult.getRight().getAckCode());
        }
    }

    /**
     * Key factor to group the VesselCSVTranslatorEntry
     * Entries with the same contextId and referenceMessageId and recipientServiceID are part of the same CISE Message
     */
    public static class MessageKey {
        private final String contextId;
        private final String referenceMessageId;
        private final String recipientServiceId;

        public MessageKey(String contextId, String referenceMessageId, String recipientServiceId) {
            this.contextId = contextId;
            this.referenceMessageId = referenceMessageId;
            this.recipientServiceId = recipientServiceId;
        }

        public String getContextId() {
            return contextId;
        }

        public String getReferenceMessageId() {
            return referenceMessageId;
        }

        public String getRecipientServiceId() {
            return recipientServiceId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MessageKey that = (MessageKey) o;
            return Objects.equals(contextId, that.contextId) && Objects.equals(referenceMessageId, that.referenceMessageId) && Objects.equals(recipientServiceId, that.recipientServiceId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(contextId, referenceMessageId, recipientServiceId);
        }
    }
}
