package eu.cise.adaptor.plugin.cisecontext.impl;

import eu.cise.adaptor.core.common.exceptions.CiseAdaptorValidationException;
import eu.cise.adaptor.core.common.logging.AdaptorLogger;
import eu.cise.adaptor.core.common.logging.LogConfig;
import eu.cise.adaptor.core.common.logging.LoggerMessage;
import eu.cise.adaptor.core.servicehandler.domain.RegisteredMessage;
import eu.cise.adaptor.core.servicehandler.domain.context.MessageDataContext;
import eu.cise.adaptor.core.servicehandler.port.in.ReceiveFromLegacySystemPort;
import eu.cise.adaptor.plugin.cisecontext.SubscribeProviderDeliver;
import eu.cise.servicemodel.v1.message.Acknowledgement;
import eu.cise.servicemodel.v1.message.Message;
import eu.cise.servicemodel.v1.message.XmlEntityPayload;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * The {@code VesselInformationDeliver} implements the {@link SubscribeProviderDeliver} Functional interface.
 * It uses {@link ReceiveFromLegacySystemPort} functionalities to retrieve previous messages and to deliver to the Service Handler of the Generic Adaptor.
 * It also does this actions:
 * <ul>
 * <li>Checks the UUID format validity of contextId</li>
 * <li>If the contextId is invalid, trying to get it from the message identified by referenceMessageId, and if it isn't found, it will be generated</li>
 * <li>If the contextId is not found, it will be generated</li>
 * </ul>
 *
 * @see ReceiveFromLegacySystemPort
 */
public class VesselInformationDeliver implements SubscribeProviderDeliver {

    private static final Pattern UUID_REGEX =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private static final AdaptorLogger logger = LogConfig.configureLogging(VesselInformationDeliver.class);

    private final ReceiveFromLegacySystemPort receiveFromLegacySystemPort;

    public VesselInformationDeliver(ReceiveFromLegacySystemPort receiveFromLegacySystemPort) {
        this.receiveFromLegacySystemPort = receiveFromLegacySystemPort;
    }

    @Override
    public List<Pair<RegisteredMessage, Acknowledgement>> deliver(String referenceMessageId, String contextId, XmlEntityPayload payload) throws CiseAdaptorValidationException {

        // validate that contextId is valid UUID. Try to get it from the referenceMessageId
        if (!UUID_REGEX.matcher(contextId).matches()) {
            logger.info(LoggerMessage.of("Provided Context ID {}  was invalid, trying to get it from referenceMessageId {}", contextId, referenceMessageId));
            Optional<Message> foundReferenceMessage = receiveFromLegacySystemPort.getMessageById(referenceMessageId);
            if (foundReferenceMessage.isPresent()) {
                contextId = foundReferenceMessage.get().getContextID();
                logger.info(LoggerMessage.of("Reference message with ID {}  was found in the database. Used contextId: {} from reference message", referenceMessageId, contextId));

            } else {
                // if the provided contextId is not correct and the referenceMessageId cannot be found, create a new UUID
                contextId = UUID.randomUUID().toString();
                logger.info(LoggerMessage.of("Reference message with ID {}  was not found in the database. Generated random ContextID: {}", referenceMessageId, contextId));
            }
        }

        // in case the contextId is not correct for any reason, generate a new one
        if (StringUtils.isEmpty(contextId)) {
            contextId = UUID.randomUUID().toString();
            logger.info(LoggerMessage.of("Attempts to recover contextId failed. Generated new random: {}", contextId));
        }

        // Create the MessageDataContextManager
        MessageDataContext.MessageDataContextManager messageDataContextManager = MessageDataContext.getManager()
                .cisePayload(payload) // the translated payload of the message
                .contextId(contextId) // the contextId from the CSV File
                .requiresAck(false);

        return receiveFromLegacySystemPort.handleIncomingLegacyData(messageDataContextManager);
    }


}
