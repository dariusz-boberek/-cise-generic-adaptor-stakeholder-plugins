package eu.cise.adaptor.plugin.cisecontext.impl;

import eu.cise.adaptor.core.common.exceptions.CiseAdaptorValidationException;
import eu.cise.adaptor.core.common.logging.AdaptorLogger;
import eu.cise.adaptor.core.common.logging.LogConfig;
import eu.cise.adaptor.core.common.logging.LoggerMessage;
import eu.cise.adaptor.core.servicehandler.domain.RegisteredMessage;
import eu.cise.adaptor.core.servicehandler.domain.context.MessageDataContext;
import eu.cise.adaptor.core.servicehandler.port.in.ReceiveFromLegacySystemPort;
import eu.cise.adaptor.plugin.cisecontext.PushProviderDeliver;
import eu.cise.adaptor.plugin.config.DiscoveryProfileConfigurationReader;
import eu.cise.servicemodel.v1.message.Acknowledgement;
import eu.cise.servicemodel.v1.message.Message;
import eu.cise.servicemodel.v1.message.XmlEntityPayload;
import eu.cise.servicemodel.v1.service.Service;
import eu.cise.servicemodel.v1.service.ServiceOperationType;
import eu.cise.servicemodel.v1.service.ServiceProfile;
import eu.cise.servicemodel.v1.service.ServiceRoleType;
import eu.cise.servicemodel.v1.service.ServiceType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * The {@code VesselInformationDeliver} implements the {@link PushProviderDeliver} Functional interface.
 * It uses {@link ReceiveFromLegacySystemPort} functionalities to retrieve previous messages and to deliver to the Service Handler of the Generic Adaptor.
 * It also does these actions:
 * <ul>
 * <li>Checks the UUID format validity of contextId and ensures the recipientId is not null or empty</li>
 * <li>If the contextId is invalid, it tries to get it from the message identified by referenceMessageId, and if it isn't found, it will be generated</li>
 * <li>If the contextId is not found, it will be generated</li>
 * <li>If the recipientId is null or empty, it will be taken from the message identified by referenceMessageId</li>
 * </ul>
 *
 * @see ReceiveFromLegacySystemPort
 */
public class VesselInformationDeliver implements PushProviderDeliver {

    private static final Pattern UUID_REGEX =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private static final AdaptorLogger logger = LogConfig.configureLogging(VesselInformationDeliver.class);

    private final ReceiveFromLegacySystemPort receiveFromLegacySystemPort;
    private static final List<ServiceProfile> configuredProfiles;

    static{
        DiscoveryProfileConfigurationReader discoveryProfileConfigurationReader = new DiscoveryProfileConfigurationReader();
        configuredProfiles = discoveryProfileConfigurationReader.getConfiguredProfiles(ServiceRoleType.CONSUMER);
    }

    public VesselInformationDeliver(ReceiveFromLegacySystemPort receiveFromLegacySystemPort) {
        this.receiveFromLegacySystemPort = receiveFromLegacySystemPort;

    }

    @Override
    public List<Pair<RegisteredMessage, Acknowledgement>> deliver(String referenceMessageId, String contextId, String recipientId, XmlEntityPayload payload) throws CiseAdaptorValidationException {

        // validate that contextId is valid UUID or recipientId is empty. Try to get them from the referenceMessageId
        if (!UUID_REGEX.matcher(contextId).matches() || StringUtils.isEmpty(recipientId)) {
            logger.info(LoggerMessage.of("Provided Context Id {}  was invalid, trying to get it from referenceMessageId {}", contextId, referenceMessageId));
            Optional<Message> foundReferenceMessage = receiveFromLegacySystemPort.getMessageById(referenceMessageId);
            if (foundReferenceMessage.isPresent()) {
                contextId = foundReferenceMessage.get().getContextID();
                logger.info(LoggerMessage.of("Reference message with ID {}  was found in the database. Used contextId: {} from reference message",referenceMessageId, contextId));
                // check recipients also. If they were not provided, get the sender of the referenceMessage
                if (StringUtils.isEmpty(recipientId)) {
                    recipientId = foundReferenceMessage.get().getSender().getServiceID();
                    logger.info(LoggerMessage.of("Provided recipient String was empty. Discovered recipient {} from reference message", recipientId));
                }
            } else {
                // if the provided contextId is not correct and the referenceMessageId cannot be found, create a new UUID
                contextId = UUID.randomUUID().toString();
                logger.info(LoggerMessage.of("Reference message with ID {}  was not found in the database. Generated random ContextId: {}", referenceMessageId, contextId));
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
                .requiresAck(true);

        calculateDestination(messageDataContextManager, recipientId);

        return receiveFromLegacySystemPort.handleIncomingLegacyData(messageDataContextManager);
    }

    private void calculateDestination(MessageDataContext.MessageDataContextManager messageDataContextManager, String recipientId) throws CiseAdaptorValidationException {
        if (recipientId.startsWith("unknown")){
            // split the recipient id provided to find the name of the profile to use pattern unknown_1 unknown_2
            int profileNumber = Integer.parseInt(recipientId.split("_")[1]);
            if (profileNumber > configuredProfiles.size()){
                throw new CiseAdaptorValidationException("Profile specified: " + profileNumber + " cannot be found in the configured profiles");
            }
            ServiceProfile serviceProfile = configuredProfiles.get(profileNumber); // get it from the configuration
            messageDataContextManager.initializePushUnknown(Collections.singletonList(serviceProfile));
        }
        else {
            messageDataContextManager.initializePushKnown(generateRecipientServiceFromId(recipientId)); // the recipients
        }
    }

    private List<Service> generateRecipientServiceFromId(String recipientServiceId) {
        List<Service> services = new ArrayList<>();
        if (!(recipientServiceId == null || recipientServiceId.trim().isEmpty())) {
            Service service = new Service();
            service.setServiceID(recipientServiceId);
            service.setServiceOperation(ServiceOperationType.PUSH);
            service.setServiceRole(ServiceRoleType.CONSUMER);
            service.setServiceType(ServiceType.VESSEL_SERVICE);
            services.add(service);
        }
        return services;
    }

}
