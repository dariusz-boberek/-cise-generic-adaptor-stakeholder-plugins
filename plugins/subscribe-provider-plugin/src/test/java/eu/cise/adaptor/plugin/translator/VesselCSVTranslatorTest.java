package eu.cise.adaptor.plugin.translator;


import eu.cise.adaptor.plugin.TestHelper;
import eu.cise.datamodel.v1.entity.vessel.Vessel;
import eu.cise.servicemodel.v1.message.XmlEntityPayload;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class VesselCSVTranslatorTest {

    @Test
    void it_tests_conversion_of_correct_vesselCSV_with_one_resulting_messages() throws IOException, URISyntaxException {
        File csvFile = TestHelper.resourceToFile("vesselscsv/vessels_csv_example_one_message.csv");
        AtomicReference<VesselCSVTranslator> result = new AtomicReference<>();
        assertDoesNotThrow(() -> result.set(VesselCSVTranslator.vesselCSVToCISE(csvFile)));
        List<VesselCSVTranslatorEntry> errorResults = result.get().getErrorLines();
        assertEquals(0, errorResults.size());
        List<Pair<VesselCSVTranslator.MessageKey, XmlEntityPayload>> correctResults = result.get().getResultingMessagesPayloads();
        assertEquals(1, correctResults.size()); // expected to get 1 message from this file
        VesselCSVTranslator.MessageKey messageKey = correctResults.get(0).getLeft();
        assertEquals("f648cfd1-aade-41b9-a29e-17a37de2c09b", messageKey.getContextId());
        assertEquals("h648cfd1-aade-41b9-a29e-17a37de2c09d", messageKey.getReferenceMessageId());
        // test the generated payload
        Vessel generatedVessel = (Vessel) correctResults.get(0).getRight().getAnies().get(0);
        assertEquals(Long.valueOf("7710525"), generatedVessel.getIMONumber());
        assertEquals(Long.valueOf("232000000"), generatedVessel.getMMSI());
        assertEquals("22.253667", generatedVessel.getLocationRels().get(0).getLocation().getGeometries().get(0).getLongitude());
        assertEquals("34.438333", generatedVessel.getLocationRels().get(0).getLocation().getGeometries().get(0).getLatitude());
    }

    @Test
    void it_tests_conversion_of_correct_vesselCSV_with_two_resulting_messages() throws IOException, URISyntaxException {
        File csvFile = TestHelper.resourceToFile("vesselscsv/vessels_csv_example.csv");
        AtomicReference<VesselCSVTranslator> result = new AtomicReference<>();
        assertDoesNotThrow(() -> result.set(VesselCSVTranslator.vesselCSVToCISE(csvFile)));
        List<VesselCSVTranslatorEntry> errorResults = result.get().getErrorLines();
        assertEquals(0, errorResults.size());
        List<Pair<VesselCSVTranslator.MessageKey, XmlEntityPayload>> correctResults = result.get().getResultingMessagesPayloads();
        assertEquals(2, correctResults.size()); // expected to get 2 messages from this file
    }

    @Test
    void it_tests_conversion_of_correct_vesselCSV_with_four_resulting_messages() throws IOException, URISyntaxException {
        File csvFile = TestHelper.resourceToFile("vesselscsv/vessels_csv_example_four_messages.csv");
        AtomicReference<VesselCSVTranslator> result = new AtomicReference<>();
        assertDoesNotThrow(() -> result.set(VesselCSVTranslator.vesselCSVToCISE(csvFile)));
        List<VesselCSVTranslatorEntry> errorResults = result.get().getErrorLines();
        assertEquals(0, errorResults.size());
        assertFalse(result.get().hasErrors());
        List<Pair<VesselCSVTranslator.MessageKey, XmlEntityPayload>> correctResults = result.get().getResultingMessagesPayloads();
        assertEquals(4, correctResults.size()); // expected to get 4 messages from this file
        // the order in the list should follow the order that the keys have been found in the file. So, it should be:
        // f648cfd1-aade-41b9-a29e-17a37de2c09b,h648cfd1-aade-41b9-a29e-17a37de2c09d -> 2 vessels
        // k648cfd1-aade-41b9-a29e-17a37de2c09b,l648cfd1-aade-41b9-a29e-17a37de2c09d -> 2 vessels
        // k648cfd1-aade-41b9-a29e-17a37de2c09b,l648cfd1-aade-41b9-a29e-17a37de2c09g -> 1 vessel
        // k648cfd1-aade-41b9-a29e-17a37de2c09g,l648cfd1-aade-41b9-a29e-17a37de2c09d -> 1 vessel

        // test first entry
        Pair<VesselCSVTranslator.MessageKey, XmlEntityPayload> firstResult = correctResults.get(0);
        VesselCSVTranslator.MessageKey firstMessageKey = firstResult.getLeft();
        XmlEntityPayload firstPayload = firstResult.getRight();
        assertEquals("f648cfd1-aade-41b9-a29e-17a37de2c09b", firstMessageKey.getContextId());
        assertEquals("h648cfd1-aade-41b9-a29e-17a37de2c09d", firstMessageKey.getReferenceMessageId());
        assertEquals(2, firstPayload.getAnies().size());

        // test third entry
        Pair<VesselCSVTranslator.MessageKey, XmlEntityPayload> thirdResult = correctResults.get(2);
        VesselCSVTranslator.MessageKey thirdMessageKey = thirdResult.getLeft();
        XmlEntityPayload thirdPayload = thirdResult.getRight();
        assertEquals("k648cfd1-aade-41b9-a29e-17a37de2c09b", thirdMessageKey.getContextId());
        assertEquals("l648cfd1-aade-41b9-a29e-17a37de2c09g", thirdMessageKey.getReferenceMessageId());
        assertEquals(1, thirdPayload.getAnies().size());
    }

    @Test
    void it_tests_conversion_of_wrong_vesselCSV() throws IOException, URISyntaxException {
        File csvFile = TestHelper.resourceToFile("vesselscsv/vessels_csv_example_wrong.csv");
        AtomicReference<VesselCSVTranslator> result = new AtomicReference<>();
        assertDoesNotThrow(() -> result.set(VesselCSVTranslator.vesselCSVToCISE(csvFile)));
        List<VesselCSVTranslatorEntry> errorResults = result.get().getErrorLines();
        assertEquals(3, errorResults.size());
        List<Pair<VesselCSVTranslator.MessageKey, XmlEntityPayload>> correctResults = result.get().getResultingMessagesPayloads();
        assertEquals(2, correctResults.size());
    }

}