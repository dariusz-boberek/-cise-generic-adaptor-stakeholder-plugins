package eu.cise.adaptor.plugin.cisecontext;

import eu.cise.adaptor.plugin.translator.exceptions.VesselCSVTranslationException;
import eu.cise.datamodel.v1.entity.vessel.Vessel;

/**
 * This Functional Interface declares a function, translateToVessel, that creates a CISE Vessel of the CISE Data Model to be used inside the CISE Payload
 */
@FunctionalInterface
public interface VesselBuilder {
    /**
     * This function creates a CISE Vessel Data Model instance, from the vessel data.
     *
     * @param imoNumberStr The IMO number of the vessel.
     * @param mmsiStr MMSI number as defined by ITU-R M.1371
     * @param latitude Latitude position
     * @param longitude Longitude position
     * @param positionTimestampStr Time stamp of the position data, in the format yyyy-MM-dd HH:mm:ss
     * @return Vessel data model instance
     * @throws VesselCSVTranslationException Thrown in case data are not complaint to build a Vessel Instance
     */
    Vessel translateToVessel(String imoNumberStr, String mmsiStr, String latitude , String longitude , String positionTimestampStr) throws VesselCSVTranslationException;
}
