package eu.cise.adaptor.plugin;


import eu.cise.adaptor.core.common.exceptions.CiseAdaptorRuntimeException;
import eu.cise.adaptor.core.common.logging.AdaptorLogger;
import eu.cise.adaptor.core.common.logging.LogConfig;
import eu.cise.adaptor.core.common.logging.LoggerMessage;
import eu.cise.adaptor.core.servicehandler.domain.RegisteredMessage;
import eu.cise.adaptor.core.servicehandler.port.in.ReceiveFromLegacySystemPort;
import eu.cise.adaptor.plugin.cisecontext.PushProviderDeliver;
import eu.cise.adaptor.plugin.cisecontext.impl.VesselInformationDeliver;
import eu.cise.adaptor.plugin.config.PushProviderPluginConfig;
import eu.cise.adaptor.plugin.translator.VesselCSVTranslator;
import eu.cise.servicemodel.v1.message.Acknowledgement;
import eu.cise.servicemodel.v1.message.XmlEntityPayload;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static eu.cise.adaptor.plugin.translator.VesselCSVTranslator.vesselCSVToCISE;

/**
 * This class is used to monitor an input directory for incoming CSV files that can be translated in to CISE Messages.
 * When a new file is found, it is translated using the {@see VesselCSVTranslator} class
 */
public class VesselCSVFileHandler {

    private static final AdaptorLogger logger = LogConfig.configureLogging(VesselCSVFileHandler.class);
    private final PushProviderDeliver vesselDeliver;
    private final File inputDirectory;
    private final File outputDirectory;
    private final File errorDirectory;
    private final ScheduledExecutorService executorService;
    Map<String, Long> unprocessedFiles;


    public VesselCSVFileHandler(PushProviderPluginConfig config, ReceiveFromLegacySystemPort receiveFromLegacySystemPort) {

        this.vesselDeliver = new VesselInformationDeliver(receiveFromLegacySystemPort);
        this.executorService = Executors.newScheduledThreadPool(1);
        this.unprocessedFiles = new HashMap<>();

        // create and verify the required directories objects
        // input
        this.inputDirectory = new File(config.getCSVInputDirectory());
        validateDirectory(inputDirectory);
        // output
        this.outputDirectory = new File(config.getCSVOutputDirectory());
        validateDirectory(outputDirectory);
        // error
        this.errorDirectory = new File(config.getCSVErrorDirectory());
        validateDirectory(errorDirectory);
    }

    private void validateDirectory(File inputDir) {
        if (!(inputDir.exists() && inputDir.isDirectory() && inputDir.canWrite())) {
            throw new CiseAdaptorRuntimeException("Directory does not exist or cannot be read: " + inputDir.getAbsolutePath());
        }
    }

    /**
     * Start monitoring
     */
    public void start() {
        executorService.scheduleAtFixedRate(this::monitorInputDirectory, 0, 5, TimeUnit.SECONDS);
    }

    private boolean isFileToBeProcessed(File foundFile) {
        boolean toBeProcessed = false;

        if (unprocessedFiles.containsKey(foundFile.getAbsolutePath())) {
            // check if the size has changed
            long storedSize = unprocessedFiles.get(foundFile.getAbsolutePath());
            // calculate current size
            long currentSize = getCurrentFileSize(foundFile.getAbsolutePath());
            toBeProcessed = currentSize == storedSize && currentSize > 0L;
        }
        return toBeProcessed;
    }

    private void monitorInputDirectory() {
        logger.debug(LoggerMessage.of("Monitoring incoming CSV dir: {}", inputDirectory.getAbsolutePath()));
        for (File foundFile : Objects.requireNonNull(inputDirectory.listFiles((dir, name) -> name.endsWith(".csv")))) {

            if (isFileToBeProcessed(foundFile)) {
                InfoFile infoFile = new InfoFile(foundFile);

                // translate the file
                try {
                    VesselCSVTranslator vesselCSVTranslatorToCiseResult = vesselCSVToCISE(foundFile);
                    if (!vesselCSVTranslatorToCiseResult.hasErrors()) {

                        // send the generated messages
                        for (Pair<VesselCSVTranslator.MessageKey, XmlEntityPayload> messageEntry : vesselCSVTranslatorToCiseResult.getResultingMessagesPayloads()) {

                            VesselCSVTranslator.MessageKey messageKey = messageEntry.getKey();
                            List<Pair<RegisteredMessage, Acknowledgement>> sendResult = vesselDeliver.deliver(
                                    messageKey.getReferenceMessageId(),
                                    messageKey.getContextId(),
                                    messageKey.getRecipientServiceId(),
                                    messageEntry.getRight());

                            // there should be only 1 entry in the list and that entry we can use to update the results
                            vesselCSVTranslatorToCiseResult.updateEntriesWithMessageResult(messageKey, sendResult.get(0));
                        }

                        // after all the entries have been sent write outputFile with details
                        vesselCSVTranslatorToCiseResult.writeOutputFileWithGeneratedMessageDetails(infoFile.outputFileWithSentInformation);

                    } else if (!vesselCSVTranslatorToCiseResult.getErrorLines().isEmpty()) {
                        // if there were errors, write the error file with the problematic lines
                        vesselCSVTranslatorToCiseResult.writeErrorLinesToFile(infoFile.errorFile);
                        logger.warn(LoggerMessage.of("Created error file {} with {} error lines", infoFile.errorFile.getAbsolutePath(), vesselCSVTranslatorToCiseResult.getErrorLines().size()));
                    }

                    // since the file has been processed, move the original file to the output directory
                    moveFileToLocation(infoFile.inputFile, infoFile.outputFile);
                    unprocessedFiles.remove(foundFile.getAbsolutePath());

                } catch (Exception ex) {
                    logger.error(LoggerMessage.of("Exception occurred During translation. {} , stackTrace: {}", ex.getMessage(), getStackTraceAsString(ex)));
                    logger.error(LoggerMessage.of("Moving file to Error directory {} ", infoFile.errorFile.getAbsolutePath()));
                    moveFileToLocation(infoFile.inputFile, infoFile.errorFile);
                    unprocessedFiles.remove(foundFile.getAbsolutePath());
                }

            } else if (!unprocessedFiles.containsKey(foundFile.getAbsolutePath())) {
                unprocessedFiles.put(foundFile.getAbsolutePath(), getCurrentFileSize(foundFile.getAbsolutePath()));
            }
        }
    }

    private static void moveFileToLocation(File inputFile, File outputFile) {
        try {
            Files.move(inputFile.toPath(), outputFile.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            logger.info(LoggerMessage.of("Moved file {} to output {}", inputFile.getAbsolutePath(), outputFile.getAbsolutePath()));
        } catch (IOException ex) {
            logger.error(LoggerMessage.of("Unable to move input file: {}, to output directory {}. Exception Message: ", inputFile.getAbsolutePath(), outputFile.getAbsolutePath(), ex.getMessage()));
        }
    }


    private long getCurrentFileSize(String absolutePath) {
        long result = 0L;
        try {
            result = Files.size(Path.of(absolutePath));
        } catch (Exception ex) {
            logger.error(LoggerMessage.of("Unable to calculate size of {} . Exception message: {}", absolutePath, ex.getMessage()));
        }
        return result;
    }

    /**
     * Stop the CSVFileHandler Executor Service
     *
     * @throws InterruptedException Interrupt exception
     */
    public void stopServer() throws InterruptedException {
        executorService.shutdownNow();
    }

    private static String getStackTraceAsString(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

    private class InfoFile {

        public final File inputFile;
        public final File outputFile;
        public final File errorFile;
        public final File outputFileWithSentInformation;

        public InfoFile(File foundFile) {

            // process the file
            inputFile = new File(foundFile.getAbsolutePath());
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            String processedTimestamp = sdf.format(Date.from(Calendar.getInstance().toInstant()));
            String constructedFilename = inputFile.getName().substring(0, inputFile.getName().lastIndexOf(".")) + "_" + processedTimestamp + ".csv";
            outputFile = new File(outputDirectory, constructedFilename); // the output file
            errorFile = new File(errorDirectory, constructedFilename); // the error file (if errors exist)

            // calculate output with sent details file name
            String outputFileWithDetails = inputFile.getName().substring(0, inputFile.getName().lastIndexOf(".")) + "_" + processedTimestamp + "_sentDetails.csv";
            outputFileWithSentInformation = new File(outputDirectory, outputFileWithDetails); // the output file with sent details
        }

    }

}
