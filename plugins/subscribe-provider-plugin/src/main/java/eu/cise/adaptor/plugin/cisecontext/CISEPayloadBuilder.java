package eu.cise.adaptor.plugin.cisecontext;

import eu.cise.adaptor.plugin.translator.VesselCSVTranslatorEntry;
import eu.cise.servicemodel.v1.message.XmlEntityPayload;

import java.util.List;

@FunctionalInterface
public interface CISEPayloadBuilder {
    /**
     * This method builds the CISE Payload, using the Vessel instances contained in the list of {@link VesselCSVTranslatorEntry}.
     *
     * @param entityList List of VesselCSVTranslatorEntry
     * @return The CISE Payload with all the Vessels inside
     */
    XmlEntityPayload build(List<VesselCSVTranslatorEntry> entityList);
}
