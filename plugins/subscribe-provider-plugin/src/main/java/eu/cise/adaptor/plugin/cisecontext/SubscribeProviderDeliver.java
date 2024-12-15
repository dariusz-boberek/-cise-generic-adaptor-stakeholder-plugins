package eu.cise.adaptor.plugin.cisecontext;

import eu.cise.adaptor.core.common.exceptions.CiseAdaptorValidationException;
import eu.cise.adaptor.core.servicehandler.domain.RegisteredMessage;
import eu.cise.servicemodel.v1.message.Acknowledgement;
import eu.cise.servicemodel.v1.message.XmlEntityPayload;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

/**
 * This Functional Interface declares a function, deliver, that gathers all the needed CISE data to build a CISE message and deliver it to the Service Handler of the Generic Adaptor
 */
@FunctionalInterface
public interface SubscribeProviderDeliver {

    /**
     * This function creates a CISE Message from the input parameters and forward it the Service Handler of the Generic Adaptor
     *
     * @param referenceMessageId The messageId of the previous CISE Message received.
     * @param contextId The contextId associated with the message
     * @param payload The cisePayload
     * @return List of couple RegisteredMessage (the CISE message sent) and Acknowledgement (the Acknowledgement result)
     * @throws CiseAdaptorValidationException It will be thrown if some problem is found in the input data.
     */
    List<Pair<RegisteredMessage, Acknowledgement>> deliver(String referenceMessageId, String contextId, XmlEntityPayload payload) throws CiseAdaptorValidationException;
}
