package eu.cise.adaptor.plugin;

import eu.cise.adaptor.core.common.exceptions.CiseAdaptorRuntimeException;
import eu.cise.adaptor.core.common.logging.AdaptorLogger;
import eu.cise.adaptor.core.common.logging.LogConfig;
import eu.cise.adaptor.core.common.message.MessageTypeEnum;
import eu.cise.adaptor.core.servicehandler.domain.RegisteredMessage;
import eu.cise.adaptor.core.servicehandler.port.out.SendToLegacySystemPort;
import eu.cise.adaptor.core.servicehandler.port.out.UpdateLegacySystemResult;
import eu.cise.adaptor.plugin.config.PushConsumerPluginConfig;
import eu.cise.servicemodel.v1.message.Message;
import eu.cise.servicemodel.v1.message.Push;
import eu.eucise.xml.DefaultXmlMapper;
import eu.eucise.xml.XmlMapper;
import eu.eucise.xml.XmlNotParsableException;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import static eu.cise.adaptor.core.common.logging.LoggerMessage.of;
import static eu.cise.adaptor.core.common.logging.MessageRouteValue.CISE_TO_LS;
import static eu.cise.adaptor.core.common.message.MessageTypeEnum.PUSH;
import static eu.cise.adaptor.core.common.message.MessageTypeEnum.UNDEFINED;

public class SendToLegacySystemAdapter implements SendToLegacySystemPort {

    public static final String UPDATE_LEGACY_SYSTEM_STATUS_OK = "ok";
    public static final String UPDATE_LEGACY_SYSTEM_STATUS_ERROR = "error";
    private final static AdaptorLogger log = LogConfig.configureLogging(SendToLegacySystemAdapter.class);
    private final XmlMapper xmlMapper;

    private final PushConsumerPluginConfig config;

    public SendToLegacySystemAdapter(PushConsumerPluginConfig config) {
        xmlMapper = new DefaultXmlMapper.Pretty();
        this.config = config;
    }

    @Override
    public UpdateLegacySystemResult updateLegacySystem(RegisteredMessage newRegisteredMessage, String messageId, List<RegisteredMessage> messagesHistory, String contextId) {
        log.debug(of("push-consumer-plugin updateLegacySystem called").addRoutingAttribute(CISE_TO_LS));
        if (newRegisteredMessage == null || newRegisteredMessage.getCiseMessage() == null) {
            throw new IllegalArgumentException("message was null");
        }
        Message message = newRegisteredMessage.getCiseMessage();
        log.info(of(UNDEFINED, message.getMessageID(), "Translation message processed.").addRoutingAttribute(CISE_TO_LS));
        if (message instanceof Push) {
            return handlePush(message, messagesHistory);
        }
        throw new IllegalArgumentException("Message class handling not implemented " + message.getClass().getCanonicalName());
    }

    private UpdateLegacySystemResult handlePush(Message message, List<RegisteredMessage> messagesHistory) {
        String resultAckCode;
        String pushMessageString;
        try {
            pushMessageString = xmlMapper.toXML(message);
        } catch (XmlNotParsableException | CiseAdaptorRuntimeException exception) {
            log.error(of(PUSH, message.getMessageID(), "Error while converting message to XML").addRoutingAttribute(CISE_TO_LS), exception);
            return new UpdateLegacySystemResult(UPDATE_LEGACY_SYSTEM_STATUS_ERROR);
        }
        resultAckCode = saveAsFile(message.getMessageID(), pushMessageString);
        log.debug(of(PUSH, message.getMessageID(), "Message successfully processed and written to file.").addRoutingAttribute(CISE_TO_LS));
        // print the history information
        if (messagesHistory != null) {
            for (RegisteredMessage registeredMessage : messagesHistory) {
                log.info(of(MessageTypeEnum.messageTypeOf(registeredMessage.getCiseMessage()), registeredMessage.getMessageId()
                        , "Retrieved Message From History based on the contextId {} or the correlationId {} of the received Message ID{}"
                        , message.getContextID()
                        , message.getCorrelationID()
                        , message.getMessageID()));
            }
        }
        return new UpdateLegacySystemResult(resultAckCode);
    }

    private String saveAsFile(String messageId, String pushMessage) {
        String resultAckCode;
        try (FileOutputStream fos = new FileOutputStream(config.getReceivedMessageSaveFile()); DataOutputStream outStream = new DataOutputStream(new BufferedOutputStream(fos))) {
            outStream.writeBytes(pushMessage);
            resultAckCode = UPDATE_LEGACY_SYSTEM_STATUS_OK;
        } catch (IOException exception) {
            log.error(of(PUSH, messageId, "Error while writing to file: " + exception.getMessage()).addRoutingAttribute(CISE_TO_LS), exception);
            resultAckCode = UPDATE_LEGACY_SYSTEM_STATUS_ERROR;
        }
        return resultAckCode;
    }

}