package eu.cise.adaptor.plugin.cisecontext.impl;

import eu.cise.adaptor.plugin.cisecontext.CISEPayloadBuilder;
import eu.cise.adaptor.plugin.cisecontext.VesselBuilder;
import eu.cise.adaptor.plugin.translator.TimeUtils;
import eu.cise.adaptor.plugin.translator.VesselCSVTranslatorEntry;
import eu.cise.adaptor.plugin.translator.exceptions.VesselCSVTranslationException;
import eu.cise.datamodel.v1.entity.location.Geometry;
import eu.cise.datamodel.v1.entity.location.Location;
import eu.cise.datamodel.v1.entity.object.Objet;
import eu.cise.datamodel.v1.entity.period.Period;
import eu.cise.datamodel.v1.entity.vessel.Vessel;
import eu.cise.servicemodel.v1.message.InformationSecurityLevelType;
import eu.cise.servicemodel.v1.message.InformationSensitivityType;
import eu.cise.servicemodel.v1.message.PurposeType;
import eu.cise.servicemodel.v1.message.XmlEntityPayload;
import org.apache.commons.lang3.StringUtils;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.List;

/**
 * This class manages all the aspect about the creation of the CISE Payload to be sent to the CISE Network
 * It contains two methods:<li>The implementation of Functional Interface @link {@link VesselBuilder} to build the CISE Vessel class from the data provided by the csv file</li><li>The building of the CISE payload using multiple instances of CISE Vessel through the implementation of the @link {@link CISEPayloadBuilder} functional interface</li>
 */
public class VesselPayloadBuilder implements VesselBuilder, CISEPayloadBuilder {


    /**
     * Implementation of the Functional Interface  {@link CISEPayloadBuilder}.
     *
     * @param entityList List of VesselCSVTranslatorEntry
     * @return The CISE Payload with all the Vessels inside
     */
    public XmlEntityPayload build(List<VesselCSVTranslatorEntry> entityList) {

        // create the CISE payload
        XmlEntityPayload resultPayload = new XmlEntityPayload();
        // add the basics
        resultPayload.setInformationSecurityLevel(InformationSecurityLevelType.NON_SPECIFIED);
        resultPayload.setInformationSensitivity(InformationSensitivityType.NON_SPECIFIED);
        resultPayload.setPurpose(PurposeType.NON_SPECIFIED);
        resultPayload.setEnsureEncryption(false);

        for (VesselCSVTranslatorEntry currentEntry : entityList) {
            resultPayload.getAnies().add(currentEntry.getResultVessel());
        }

        return resultPayload;
    }

    /**
     * Implementation of Functional Interface {@link VesselBuilder}.
     *
     * @param imoNumberStr         The IMO number of the vessel.
     * @param mmsiStr              MMSI number as defined by ITU-R M.1371
     * @param latitude             Latitude position
     * @param longitude            Longitude position
     * @param positionTimestampStr Time stamp of the position data, in the format yyyy-MM-dd HH:mm:ss
     * @return Vessel data model instance
     * @throws VesselCSVTranslationException Thrown in case data are not complaint to build a Vessel Instance
     */
    public Vessel translateToVessel(String imoNumberStr, String mmsiStr, String latitude, String longitude, String positionTimestampStr) throws VesselCSVTranslationException {

        // validate imo_number and transform to Long
        Long imoNumber = validateStringAndTransformToLong(imoNumberStr, 7, "IMO_number");
        Long mmsiNumber = validateStringAndTransformToLong(mmsiStr, 9, "MMSI");
        // event type and date converter
        Vessel vessel = createCISEVessel(imoNumber, mmsiNumber);
        // validate longitude and latitude values
        // Latitude must be a number between -90 and 90
        validateNumericWithinBounds(latitude, -90, 90, "Latitude");
        //Longitude must a number between -180 and 180
        validateNumericWithinBounds(longitude, -180, 180, "Longitude");
        validateLatitudeAndLongitude(latitude, longitude);
        validatePositionTimestampPresenceWhenCoordinatesProvided(latitude,longitude, positionTimestampStr);
        // location converter
        convertLocationToCISE(longitude, latitude, vessel);
        // created timestamp
        convertPositionTimeStampToCISE(positionTimestampStr, vessel);

        return vessel;
    }

    private Vessel createCISEVessel(Long imoNumber, Long mmsi) {
        Vessel vessel = new Vessel();
        if (imoNumber != null) {
            vessel.setIMONumber(imoNumber);
        }
        if (mmsi != null) {
            vessel.setMMSI(mmsi);
        }
        return vessel;
    }

    private void convertLocationToCISE(String longitude, String latitude, Vessel vessel) {
        if (!StringUtils.isEmpty(longitude) && !StringUtils.isEmpty(latitude)) {
            Objet.LocationRel locationRel = new Objet.LocationRel();
            vessel.getLocationRels().add(locationRel);
            Location location = new Location();
            locationRel.setLocation(location);
            Geometry geometry = new Geometry();
            location.getGeometries().add(geometry);
            geometry.setLongitude(longitude);
            geometry.setLatitude(latitude);
        }
    }


    private void convertPositionTimeStampToCISE(String positionTimestampStr, Vessel vessel) throws VesselCSVTranslationException {
        if (!StringUtils.isEmpty(positionTimestampStr) && vessel.getLocationRels().get(0) != null ) {
            XMLGregorianCalendar createdTime = TimeUtils.calculateXMLGregorianCalendarWithPattern(positionTimestampStr, "yyyy-MM-dd HH:mm:ss");

            Period period = new Period();
            period.setStartDate(createdTime);
            period.setStartTime(createdTime);
            vessel.getLocationRels().get(0).setPeriodOfTime(period);
        }
    }

    private Long validateStringAndTransformToLong(String numberString, int noOfDigits, String fieldDescription) throws VesselCSVTranslationException {
        if (StringUtils.isEmpty(numberString)) {
            return null;
        }
        if (numberString.length() != noOfDigits) {
            throw new VesselCSVTranslationException(fieldDescription + " does not have correct number of characters. Found: " + numberString.length() + " but expected: " + noOfDigits);
        }
        Long result;
        try {
            result = Long.valueOf(numberString);
        } catch (Exception ex) {
            throw new VesselCSVTranslationException("Unable to construct numeric value for field: " + fieldDescription + " with value: " + numberString);
        }
        return result;
    }

    private void validateNumericWithinBounds(String valueToCheck, double lowerBound, double upperBound, String description) throws VesselCSVTranslationException {
        if (StringUtils.isEmpty(valueToCheck)) {
            return;
        }
        Double doubleValue = lowerBound - 10; // so that the check fails if it is not parsed
        try {
            doubleValue = Double.valueOf(valueToCheck);

        } catch (Exception ex) {
            throw new VesselCSVTranslationException(description + "  value of " + valueToCheck + " could not be parsed as numeric.");
        }
        if (doubleValue < lowerBound || doubleValue > upperBound) {
            throw new VesselCSVTranslationException(description + "  value of " + valueToCheck + " is not correct. Appropriate bounds: [" + lowerBound + "," + upperBound + "]");
        }
    }

    private void validatePositionTimestampPresenceWhenCoordinatesProvided(String latitude, String longitude, String positionTimestamp) throws VesselCSVTranslationException {
        boolean isLatitudeProvided = !StringUtils.isEmpty(latitude);
        boolean isLongitudeProvided = !StringUtils.isEmpty(longitude);
        boolean isPositionTimestampMissing = StringUtils.isEmpty(positionTimestamp);

        if (isLatitudeProvided && isLongitudeProvided && isPositionTimestampMissing) {
            throw new VesselCSVTranslationException("Position timestamp is missing while Latitude and Longitude are provided.");
        }
    }

    private void validateLatitudeAndLongitude(String latitude, String longitude) throws VesselCSVTranslationException {
        boolean isLatitudeEmpty = StringUtils.isEmpty(latitude);
        boolean isLongitudeEmpty = StringUtils.isEmpty(longitude);

        if (isLatitudeEmpty != isLongitudeEmpty) {
            throw new VesselCSVTranslationException("Both Latitude and Longitude must be provided or both must be omitted.");
        }
    }
}
