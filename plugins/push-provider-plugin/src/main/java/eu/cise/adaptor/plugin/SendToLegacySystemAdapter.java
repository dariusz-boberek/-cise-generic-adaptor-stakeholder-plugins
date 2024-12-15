package eu.cise.adaptor.plugin;

import eu.cise.adaptor.core.common.logging.AdaptorLogger;
import eu.cise.adaptor.core.common.logging.LogConfig;
import eu.cise.adaptor.core.common.message.MessageTypeEnum;
import eu.cise.adaptor.core.servicehandler.domain.RegisteredMessage;
import eu.cise.adaptor.core.servicehandler.port.out.SendToLegacySystemPort;
import eu.cise.adaptor.core.servicehandler.port.out.UpdateLegacySystemResult;
import eu.cise.adaptor.plugin.config.PushProviderPluginConfig;
import eu.cise.servicemodel.v1.message.Acknowledgement;
import eu.cise.servicemodel.v1.message.AcknowledgementType;
import eu.cise.servicemodel.v1.message.Message;
import eu.cise.servicemodel.v1.message.XmlEntityPayload;
import eu.eucise.xml.DefaultXmlMapper;
import eu.eucise.xml.XmlMapper;

import java.util.List;

import static eu.cise.adaptor.core.common.logging.LoggerMessage.of;
import static eu.cise.adaptor.core.common.logging.MessageRouteValue.CISE_TO_LS;
import static eu.cise.adaptor.core.common.logging.MessageRouteValue.LS_TO_CISE;
import static eu.cise.adaptor.core.common.message.MessageTypeEnum.ASYNC_ACK;
import static eu.cise.adaptor.core.common.message.MessageTypeEnum.SYNC_ACK;

/**
 * This class is implemented in the Push Provider plugin so that if the Asycnk Ack is required from the sent messages created through
 * the {@link eu.cise.adaptor.plugin.cisecontext.impl.VesselInformationDeliver#deliver(String, String, String, XmlEntityPayload)} it
 * will provide a log of the received Acks
 */
public class SendToLegacySystemAdapter implements SendToLegacySystemPort {

    public static final String UPDATE_LEGACY_SYSTEM_STATUS_OK = "ok";
    private static final AdaptorLogger log = LogConfig.configureLogging(SendToLegacySystemAdapter.class);

    private final XmlMapper xmlMapper;

    public SendToLegacySystemAdapter(PushProviderPluginConfig config) {
        this.xmlMapper = new DefaultXmlMapper.Pretty();
    }

    @Override
    public UpdateLegacySystemResult updateLegacySystem(RegisteredMessage newRegisteredMessage, String messageId, List<RegisteredMessage> messagesHistory, String contextId) {
        log.debug(of("push-provider-plugin updateLegacySystem called").addRoutingAttribute(CISE_TO_LS));
        if (newRegisteredMessage == null || newRegisteredMessage.getCiseMessage() == null) {
            throw new IllegalArgumentException("message was null");
        }

        Message message = newRegisteredMessage.getCiseMessage();
        if (message instanceof Acknowledgement) {
            Acknowledgement receivedAck = (Acknowledgement) message;
            MessageTypeEnum ackTypeEnum = identifyTypeOfAck(receivedAck);
            if (receivedAck.getAckCode().equals(AcknowledgementType.SUCCESS)) {
                log.info(of(ackTypeEnum, messageId, "{} message received from serviceId {}. correlationId {}",
                        ackTypeEnum, receivedAck.getSender().getServiceID(), receivedAck.getCorrelationID()).addRoutingAttribute(CISE_TO_LS));
                if (log.isDebugEnabled()) {
                    log.debug(of(ackTypeEnum, messageId, "{} message received. Message {}", ackTypeEnum, xmlMapper.toXML(receivedAck)).addRoutingAttribute(CISE_TO_LS));
                }
            } else {
                log.error(of(ackTypeEnum, messageId, "{} message received with errors from serviceId {}. correlationId {}, AckError {} , AckDetail {}, Message {}",
                        ackTypeEnum, receivedAck.getSender().getServiceID(), receivedAck.getCorrelationID(), receivedAck.getAckCode(),
                        receivedAck.getAckDetail(), xmlMapper.toXML(receivedAck)).addRoutingAttribute(CISE_TO_LS));
            }
            return new UpdateLegacySystemResult(UPDATE_LEGACY_SYSTEM_STATUS_OK);
        } else {
            log.error(of("Message Type not recognized {}", xmlMapper.toXML(message)).addRoutingAttribute(LS_TO_CISE));
            throw new IllegalArgumentException("Message class handling not implemented " + message.getClass().getCanonicalName());
        }
    }

    private MessageTypeEnum identifyTypeOfAck(Acknowledgement message) {
        if (message.getAny() == null || message.getAny().getElementsByTagName("Signature") == null) {
            return SYNC_ACK;
        } else {
            return ASYNC_ACK;
        }
    }
}