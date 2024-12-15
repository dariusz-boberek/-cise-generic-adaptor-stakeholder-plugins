package eu.cise.adaptor.plugin.translator;

import eu.cise.adaptor.plugin.cisecontext.VesselBuilder;
import eu.cise.adaptor.plugin.translator.exceptions.VesselCSVTranslationException;
import eu.cise.datamodel.v1.entity.vessel.Vessel;
import eu.cise.servicemodel.v1.message.AcknowledgementType;
import eu.cise.servicemodel.v1.message.XmlEntityPayload;

/**
 * This class represents one line entry of the CSV File. It is responsible to keep all the information related to it and to
 * translate it (through the {@link eu.cise.adaptor.plugin.cisecontext.impl.VesselPayloadBuilder} ) into a CISE Vessel.
 * It also holds any translation related exceptions and it becomes enriched with the sent details information when the messages are sent
 * through the {@link eu.cise.adaptor.plugin.cisecontext.impl.VesselInformationDeliver#deliver(String, String, XmlEntityPayload)} method
 */
public class VesselCSVTranslatorEntry {
    // inputs from the CSV String line
    private String csvEntry;
    private Integer lineNumber;
    private Exception translationException;

    private String imo_numberStr;
    private String mmsiStr;

    private String positionTimestampStr;

    private String longitude;

    private String latitude;

    private String contextId;
    private String referenceMessageId;

    // Generated information from the translation / GA
    private Vessel resultVessel;

    private String generatedMessageId;

    private String generatedContextId;

    private AcknowledgementType generatedAckCode;

    private String generatedAckDetail;

    public VesselCSVTranslatorEntry(String csvEntry, Integer lineNumber, VesselBuilder vesselBuilder) {
        this.csvEntry = csvEntry;
        this.lineNumber = lineNumber;

        try {
            processCsvEntry();
            resultVessel = vesselBuilder.translateToVessel(imo_numberStr, mmsiStr, latitude, longitude, positionTimestampStr);
        } catch (VesselCSVTranslationException ex) {
            translationException = ex;
        }
    }

    private void processCsvEntry() throws VesselCSVTranslationException {

        String[] currentEntrySplit = csvEntry.split(",", -1);
        if (currentEntrySplit.length != 7) {
            throw new VesselCSVTranslationException("Unable to translate vessel input with " + currentEntrySplit.length + " fields. Lines should have 7 fields");
        }
        imo_numberStr = currentEntrySplit[0];
        mmsiStr = currentEntrySplit[1];
        longitude = currentEntrySplit[2];
        latitude = currentEntrySplit[3];
        positionTimestampStr = currentEntrySplit[4];
        contextId = currentEntrySplit[5];
        referenceMessageId = currentEntrySplit[6];
    }

    public String getCsvEntry() {
        return csvEntry;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public Exception getTranslationException() {
        return translationException;
    }

    public String getContextId() {
        return contextId;
    }

    public String getReferenceMessageId() {
        return referenceMessageId;
    }

    public Vessel getResultVessel() {
        return resultVessel;
    }

    public String getGeneratedMessageId() {
        return generatedMessageId;
    }

    public void setGeneratedMessageId(String generatedMessageId) {
        this.generatedMessageId = generatedMessageId;
    }

    public String getGeneratedContextId() {
        return generatedContextId;
    }

    public void setGeneratedContextId(String generatedContextId) {
        this.generatedContextId = generatedContextId;
    }

    public AcknowledgementType getGeneratedAckCode() {
        return generatedAckCode;
    }

    public void setGeneratedAckCode(AcknowledgementType generatedAckCode) {
        this.generatedAckCode = generatedAckCode;
    }

    public String getGeneratedAckDetail() {
        return generatedAckDetail;
    }

    public void setGeneratedAckDetail(String generatedAckDetail) {
        this.generatedAckDetail = generatedAckDetail;
    }
}
