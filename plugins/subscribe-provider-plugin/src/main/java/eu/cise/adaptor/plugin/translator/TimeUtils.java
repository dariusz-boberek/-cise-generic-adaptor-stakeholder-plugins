package eu.cise.adaptor.plugin.translator;

import eu.cise.adaptor.plugin.translator.exceptions.VesselCSVTranslationException;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class TimeUtils {

    public static XMLGregorianCalendar calculateXMLGregorianCalendarWithPattern(String dayTimeString, String pattern) throws VesselCSVTranslationException {

        XMLGregorianCalendar result = null;
        try {
            SimpleDateFormat dtf = new SimpleDateFormat(pattern);
            Date givenDateAndTime = dtf.parse(dayTimeString);

            ZonedDateTime ldt = ZonedDateTime.ofInstant(givenDateAndTime.toInstant(), ZoneId.systemDefault());
            ZonedDateTime setTime = ldt
                    .withNano(0)
                    .withZoneSameLocal(ZoneId.of("Z"));

            result = zonedDateTimeToGregorianCalendar(setTime);
        } catch (Exception ex) {
            throw new VesselCSVTranslationException("Unable to calculate time object from given string: " + dayTimeString, ex);
        }

        return result;
    }

    private static XMLGregorianCalendar zonedDateTimeToGregorianCalendar(ZonedDateTime zonedDateTime) throws DatatypeConfigurationException {
        GregorianCalendar gregCal = new GregorianCalendar();
        gregCal.setTime(Date.from(zonedDateTime.toInstant()));
        gregCal.setTimeZone(TimeZone.getTimeZone(ZoneId.of("Z")));
        XMLGregorianCalendar result = null;
        result = DatatypeFactory.newInstance()
                .newXMLGregorianCalendar(gregCal);
        result.setMillisecond(DatatypeConstants.FIELD_UNDEFINED);

        return result;
    }
}
