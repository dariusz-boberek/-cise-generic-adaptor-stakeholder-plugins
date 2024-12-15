package eu.cise.adaptor.plugin;

import eu.cise.adaptor.core.common.logging.AdaptorLogger;
import eu.cise.adaptor.core.common.logging.LogConfig;
import eu.cise.adaptor.core.common.message.MessageTypeEnum;
import eu.cise.adaptor.core.servicehandler.domain.RegisteredMessage;
import eu.cise.adaptor.core.servicehandler.port.out.SendToLegacySystemPort;
import eu.cise.adaptor.core.servicehandler.port.out.UpdateLegacySystemResult;
import eu.cise.adaptor.plugin.config.PullConsumerPluginConfig;
import eu.cise.servicemodel.v1.message.Acknowledgement;
import eu.cise.servicemodel.v1.message.AcknowledgementType;
import eu.cise.servicemodel.v1.message.Message;
import eu.cise.servicemodel.v1.message.PullResponse;
import eu.eucise.xml.DefaultXmlMapper;
import eu.eucise.xml.XmlMapper;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static eu.cise.adaptor.core.common.logging.LoggerMessage.of;
import static eu.cise.adaptor.core.common.logging.MessageRouteValue.CISE_TO_LS;
import static eu.cise.adaptor.core.common.logging.MessageRouteValue.LS_TO_CISE;
import static eu.cise.adaptor.core.common.message.MessageTypeEnum.ASYNC_ACK;
import static eu.cise.adaptor.core.common.message.MessageTypeEnum.PULL_RESPONSE;
import static eu.cise.adaptor.core.common.message.MessageTypeEnum.PUSH;
import static eu.cise.adaptor.core.common.message.MessageTypeEnum.SYNC_ACK;

public class SendToLegacySystemAdapter implements SendToLegacySystemPort {

    public static final String UPDATE_LEGACY_SYSTEM_STATUS_OK = "ok";
    public static final String UPDATE_LEGACY_SYSTEM_STATUS_ERROR = "error";
    private static final AdaptorLogger log = LogConfig.configureLogging(SendToLegacySystemAdapter.class);
    private final XmlMapper xmlMapper;
    private final PullConsumerPluginConfig config;

    public SendToLegacySystemAdapter(PullConsumerPluginConfig config) {
        this.xmlMapper = new DefaultXmlMapper.PrettyNotValidating();
        this.config = config;
    }

    @Override
    public UpdateLegacySystemResult updateLegacySystem(RegisteredMessage newRegisteredMessage, String messageId, List<RegisteredMessage> messagesHistory, String contextId) {

        log.debug(of("pull-consumer-plugin updateLegacySystem called").addRoutingAttribute(CISE_TO_LS));
        if (newRegisteredMessage == null || newRegisteredMessage.getCiseMessage() == null) {
            throw new IllegalArgumentException("message was null");
        }

        Message message = newRegisteredMessage.getCiseMessage();
        if (message instanceof PullResponse) {
            return handlePullResponse((PullResponse) message);
        } else if (message instanceof Acknowledgement) {
            return handleAcknowledgement((Acknowledgement) message);
        } else {
            log.error(of("Message Type not recognized {}", xmlMapper.toXML(message)).addRoutingAttribute(LS_TO_CISE));
            throw new IllegalArgumentException("Message class handling not implemented " + message.getClass().getCanonicalName());
        }
    }

    private UpdateLegacySystemResult handlePullResponse(PullResponse pullResponse) {

        log.debug(of("contextId: {}", pullResponse.getContextID()).addRoutingAttribute(CISE_TO_LS));
        String resultAckCode = saveAsFile(pullResponse.getMessageID(), xmlMapper.toXML(pullResponse));

        return new UpdateLegacySystemResult(resultAckCode);
    }

    private UpdateLegacySystemResult handleAcknowledgement(Acknowledgement receivedAck) {
        String messageId = receivedAck.getMessageID();
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
    }

    private String saveAsFile(String messageId, String pullResponseXML) {
        String resultAckCode;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String processedTimestamp = simpleDateFormat.format(Date.from(Calendar.getInstance().toInstant()));
        String constructedFilename = "messageXML_" + processedTimestamp + ".xml";
        File outputFile = new File(config.getOutputDirectory(), constructedFilename);
        try (FileOutputStream fos = new FileOutputStream(outputFile); DataOutputStream outStream = new DataOutputStream(new BufferedOutputStream(fos))) {
            outStream.writeBytes(pullResponseXML);
            resultAckCode = UPDATE_LEGACY_SYSTEM_STATUS_OK;
            log.info(of(PULL_RESPONSE, messageId, "Message successfully processed and written to file {}.", constructedFilename).addRoutingAttribute(CISE_TO_LS));

        } catch (IOException exception) {
            log.error(of(PUSH, messageId, "Error while writing to file: " + exception.getMessage()).addRoutingAttribute(CISE_TO_LS), exception);
            resultAckCode = UPDATE_LEGACY_SYSTEM_STATUS_ERROR;
        }
        return resultAckCode;
    }

    private MessageTypeEnum identifyTypeOfAck(Acknowledgement message) {
        if (message.getAny() == null || message.getAny().getElementsByTagName("Signature") == null) {
            return SYNC_ACK;
        } else {
            return ASYNC_ACK;
        }
    }
}
