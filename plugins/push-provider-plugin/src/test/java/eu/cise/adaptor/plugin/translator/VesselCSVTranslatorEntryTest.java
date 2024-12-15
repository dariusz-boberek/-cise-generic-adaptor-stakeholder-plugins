package eu.cise.adaptor.plugin.translator;


import eu.cise.adaptor.plugin.cisecontext.impl.VesselPayloadBuilder;
import eu.cise.adaptor.plugin.translator.exceptions.VesselCSVTranslationException;
import eu.cise.datamodel.v1.entity.location.Geometry;
import eu.cise.datamodel.v1.entity.period.Period;
import eu.cise.datamodel.v1.entity.vessel.Vessel;
import eu.eucise.xml.DefaultXmlMapper;
import eu.eucise.xml.XmlMapper;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

class VesselCSVTranslatorEntryTest {

    private XmlMapper xmlMapper = new DefaultXmlMapper.Pretty();
    private String imoNumber = "7710525";
    private String mmsi = "232000000";
    private String longitude = "22.253667";
    private String latitude = "34.438333";
    private String positionTimestamp = "2023-09-27 05:42:44";
    private String recipientServiceID = "push.consumer.id";
    private String contextId = "f648cfd1-aade-41b9-a29e-17a37de2c09b";
    private String referenceMessageId = "h648cfd1-aade-41b9-a29e-17a37de2c09d";

    private String CSVExample_1 = imoNumber + "," + mmsi + "," + longitude + "," + latitude + "," + positionTimestamp + "," + recipientServiceID + "," + contextId + "," + referenceMessageId;

    // Incorrect examples with intentional errors
    private String wrong_CSVExample_1 = "A710525,32000000," + longitude + "," + latitude + "," + positionTimestamp + "," + recipientServiceID + "," + contextId + "," + contextId;

    private String wrong_CSVExample_2 = "710525,T32000000," + longitude + "," + latitude + "," + positionTimestamp + "," + recipientServiceID + "," + contextId + "," + contextId;

    private String wrong_CSVExample_3 = imoNumber + ",T32000000," + longitude + "," + latitude + "," + positionTimestamp + "," + recipientServiceID + "," + contextId + "," + referenceMessageId;

    private String wrong_CSVExample_4 = imoNumber + ",2320000001," + longitude + "," + latitude + "," + positionTimestamp + "," + recipientServiceID + "," + contextId + "," + referenceMessageId;

    private String wrong_CSVExample_5 = imoNumber + ",2320000001," + longitude + "," + latitude + "," + positionTimestamp + "," + contextId + "," + referenceMessageId;

    private String CSVExample_2 = "," + mmsi + "," + longitude + "," + latitude + "," + positionTimestamp + "," + recipientServiceID + "," + contextId + "," + referenceMessageId;

    private String CSVExample_3 = imoNumber + ",," + longitude + "," + latitude + "," + positionTimestamp + "," + recipientServiceID + "," + contextId + "," + referenceMessageId;

    private String CSVExample_4 = imoNumber + "," + mmsi +",,,," + recipientServiceID + "," + contextId + "," + referenceMessageId;

    private String CSVExample_5 = ",,,,," + recipientServiceID + "," + contextId + "," + referenceMessageId;

    private String wrong_CSVExample_timestamp_missing = imoNumber + "," + mmsi + "," + longitude + "," + latitude + ",," + recipientServiceID + "," + contextId + "," + referenceMessageId;

    private String wrong_CSVExample_latitude_missing = imoNumber + "," + mmsi + ",," + latitude + "," + positionTimestamp + "," + recipientServiceID + "," + contextId + "," + referenceMessageId;

    private String wrong_CSVExample_longitude_missing = imoNumber + "," + mmsi + "," + longitude + ",," + positionTimestamp + "," + recipientServiceID + "," + contextId + "," + referenceMessageId;

    @Test
    public void it_throws_exception_when_longitude_missing() {
        VesselCSVTranslatorEntry entry = compareCSVEntryAndGeneratedVessel(wrong_CSVExample_longitude_missing);
        assertNotNull(entry.getTranslationException());
        assertEquals("Both Latitude and Longitude must be provided or both must be omitted.", entry.getTranslationException().getMessage());
    }

    @Test
    public void it_throws_exception_when_latitude_missing() {
        VesselCSVTranslatorEntry entry = compareCSVEntryAndGeneratedVessel(wrong_CSVExample_latitude_missing);
        assertNotNull(entry.getTranslationException());
        assertEquals("Both Latitude and Longitude must be provided or both must be omitted.", entry.getTranslationException().getMessage());
    }

    @Test
    public void it_throws_exception_when_timestamp_missing() {
        VesselCSVTranslatorEntry entry = compareCSVEntryAndGeneratedVessel(wrong_CSVExample_timestamp_missing);
        assertNotNull(entry.getTranslationException());
        assertEquals("Position timestamp is missing while Latitude and Longitude are provided.", entry.getTranslationException().getMessage());
    }

    @Test
    public void it_transforms_into_vessel_when_IMO_missing() {
        VesselCSVTranslatorEntry entry = compareCSVEntryAndGeneratedVessel(CSVExample_2);
        assertNull(entry.getTranslationException());
    }

    @Test
    public void it_transforms_into_vessel_when_MMSI_missing() {
        VesselCSVTranslatorEntry entry = compareCSVEntryAndGeneratedVessel(CSVExample_3);
        assertNull(entry.getTranslationException());
    }

    @Test
    public void it_transforms_into_vessel_when_longitude_latitude_and_timestamp_missing() throws VesselCSVTranslationException {
        VesselCSVTranslatorEntry entry = compareCSVEntryAndGeneratedVessel(CSVExample_4);
        assertNull(entry.getTranslationException());
        System.out.println(xmlMapper.toXML(entry.getResultVessel()));
    }

    @Test
    public void it_transforms_into_vessel_when_imo_mmsi_longitude_latitude_and_timestamp_missing() throws VesselCSVTranslationException {
        VesselCSVTranslatorEntry entry = compareCSVEntryAndGeneratedVessel(CSVExample_5);
        assertNull(entry.getTranslationException());
        System.out.println(xmlMapper.toXML(entry.getResultVessel()));
    }

    @Test
    public void it_throws_exception_when_string_has_less_fields_than_8() {
        VesselCSVTranslatorEntry entry = compareCSVEntryAndGeneratedVessel(wrong_CSVExample_5);
        assertEquals("Unable to translate vessel input with 7 fields. Lines should have 8 fields", entry.getTranslationException().getMessage());
    }

    @Test
    public void it_transforms_CSV_example1_into_vessel() throws VesselCSVTranslationException {
        VesselCSVTranslatorEntry entry = compareCSVEntryAndGeneratedVessel(CSVExample_1);
        assertNull(entry.getTranslationException());
    }


    @Test
    public void it_creates_exception_when_imo_is_not_numeric() {
        VesselCSVTranslatorEntry entry = compareCSVEntryAndGeneratedVessel(wrong_CSVExample_1);
        assertEquals("Unable to construct numeric value for field: IMO_number with value: A710525", entry.getTranslationException().getMessage());
    }

    @Test
    public void it_creates_exception_when_imo_has_wrong_num_of_digits() {
        VesselCSVTranslatorEntry entry = compareCSVEntryAndGeneratedVessel(wrong_CSVExample_2);
        assertEquals("IMO_number does not have correct number of characters. Found: 6 but expected: 7", entry.getTranslationException().getMessage());
    }

    @Test
    public void it_creates_exception_when_mmsi_is_not_numeric() {
        VesselCSVTranslatorEntry entry = compareCSVEntryAndGeneratedVessel(wrong_CSVExample_3);
        assertEquals("Unable to construct numeric value for field: MMSI with value: T32000000", entry.getTranslationException().getMessage());
    }

    @Test
    public void it_creates_exception_when_mmsi_has_wrong_num_of_digits() {
        VesselCSVTranslatorEntry entry = compareCSVEntryAndGeneratedVessel(wrong_CSVExample_4);
        assertEquals("MMSI does not have correct number of characters. Found: 10 but expected: 9", entry.getTranslationException().getMessage());
    }


    private VesselCSVTranslatorEntry compareCSVEntryAndGeneratedVessel(String csvInput) {
        String[] currentEntrySplit = csvInput.split(",");

        // get the fields
        String imo_number = currentEntrySplit[0];
        String mmsi = currentEntrySplit[1];
        String longitude = currentEntrySplit[2];
        String latitude = currentEntrySplit[3];
        String position_timestamp = currentEntrySplit[4];
        XMLGregorianCalendar position_timestamp_Calendar = null;
        try {
            position_timestamp_Calendar = TimeUtils.calculateXMLGregorianCalendarWithPattern(position_timestamp, "yyyy-MM-dd HH:mm:ss");
        } catch (Exception ex) {
            // do nothing
        }
        VesselCSVTranslatorEntry entry = new VesselCSVTranslatorEntry(csvInput, 1, new VesselPayloadBuilder()::translateToVessel);

        if (entry.getTranslationException() == null) {
            Vessel generatedVessel = entry.getResultVessel();

            if (StringUtils.isNotBlank(imo_number)) {
                assertEquals(Long.valueOf(imo_number), generatedVessel.getIMONumber());
            } else {
                assertNull(generatedVessel.getIMONumber());
            }
            if (StringUtils.isNotBlank(mmsi)) {
                assertEquals(Long.valueOf(mmsi), generatedVessel.getMMSI());
            } else {
                assertNull(generatedVessel.getMMSI());
            }
            boolean bothLongAndLatExist = StringUtils.isNotBlank(longitude) && StringUtils.isNotBlank(latitude);
            boolean eitherLongOrLatExist = StringUtils.isNotBlank(longitude) || StringUtils.isNotBlank(latitude);
            if(bothLongAndLatExist) {
                Geometry locationGeometry = generatedVessel.getLocationRels().get(0).getLocation().getGeometries().get(0);
                assertEquals(longitude, locationGeometry.getLongitude());
                assertEquals(latitude, locationGeometry.getLatitude());
            }
            else if (eitherLongOrLatExist){
                fail("Either longitude: " + longitude + " or latitude: " + latitude + " exist. There should have been a translation error");
            }

            // check created time
            boolean position_timestampExists= StringUtils.isNotBlank(position_timestamp);
            if (!bothLongAndLatExist && position_timestampExists ){
                fail("Both longitude: " + longitude + " and latitude: " + latitude + " do exist but there exists position_timestamp: " + position_timestamp +". There should have been a translation error");

            } else if (bothLongAndLatExist) {
                assertEquals(position_timestamp_Calendar, combineDateAndTime(generatedVessel.getLocationRels().get(0).getPeriodOfTime()));
            }
        }
        return entry;

    }

    public static XMLGregorianCalendar combineDateAndTime(Period cisePeriod) {
        XMLGregorianCalendar dateOnly = cisePeriod.getStartDate();
        XMLGregorianCalendar timeOnly = cisePeriod.getStartTime();
        DatatypeFactory factory = null;
        try {
            factory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException ex) {
            ex.printStackTrace();
        }

        XMLGregorianCalendar combinedDateTime = factory.newXMLGregorianCalendar();
        combinedDateTime.setYear(dateOnly.getYear());
        combinedDateTime.setMonth(dateOnly.getMonth());
        combinedDateTime.setDay(dateOnly.getDay());
        combinedDateTime.setHour(timeOnly.getHour());
        combinedDateTime.setMinute(timeOnly.getMinute());
        combinedDateTime.setSecond(timeOnly.getSecond());
        combinedDateTime.setMillisecond(timeOnly.getMillisecond());
        combinedDateTime.setTimezone(timeOnly.getTimezone());

        return combinedDateTime;
    }
}