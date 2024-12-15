package eu.cise.adaptor.plugin;

import eu.cise.adaptor.core.common.exceptions.CiseAdaptorRuntimeException;
import eu.cise.adaptor.core.common.logging.AdaptorLogger;
import eu.cise.adaptor.core.common.logging.LogConfig;
import eu.cise.adaptor.core.servicehandler.domain.RegisteredMessage;
import eu.cise.adaptor.core.servicehandler.port.out.SendToLegacySystemPort;
import eu.cise.adaptor.core.servicehandler.port.out.UpdateLegacySystemResult;
import eu.cise.adaptor.plugin.config.SubscribeConsumerPluginConfig;
import eu.cise.servicemodel.v1.message.Message;
import eu.eucise.xml.DefaultXmlMapper;
import eu.eucise.xml.XmlMapper;
import eu.eucise.xml.XmlNotParsableException;

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
import static eu.cise.adaptor.core.common.message.MessageTypeEnum.PUSH;

public class SendToLegacySystemAdapter implements SendToLegacySystemPort {

    private final XmlMapper xmlMapper;

    private final SubscribeConsumerPluginConfig config;

    public static final String UPDATE_LEGACY_SYSTEM_STATUS_OK = "ok";
    public static final String UPDATE_LEGACY_SYSTEM_STATUS_ERROR = "error";

    private final static AdaptorLogger log = LogConfig.configureLogging(SendToLegacySystemAdapter.class);

    public SendToLegacySystemAdapter(SubscribeConsumerPluginConfig config) {
        xmlMapper = new DefaultXmlMapper.Pretty();
        this.config = config;
    }

    @Override
    public UpdateLegacySystemResult updateLegacySystem(RegisteredMessage newRegisteredMessage, String messageId, List<RegisteredMessage> messagesHistory, String contextId) {
        log.debug(of("subscribe-consumer-plugin updateLegacySystem called").addRoutingAttribute(CISE_TO_LS));
        if (newRegisteredMessage == null || newRegisteredMessage.getCiseMessage() == null) {
            throw new IllegalArgumentException("message was null");
        }
        Message message = newRegisteredMessage.getCiseMessage();
        String pushMessageXML;
        try {
            pushMessageXML = xmlMapper.toXML(message);
        } catch (XmlNotParsableException | CiseAdaptorRuntimeException exception) {
            log.error(of(PUSH, message.getMessageID(), "Error while converting message to XML").addRoutingAttribute(CISE_TO_LS), exception);
            return new UpdateLegacySystemResult(UPDATE_LEGACY_SYSTEM_STATUS_ERROR);
        }
        log.debug(of("contextId: {}", contextId).addRoutingAttribute(CISE_TO_LS));
        String resultAckCode = saveAsFile(message.getMessageID(), pushMessageXML);
        log.debug(of(PUSH, message.getMessageID(), "Message successfully processed and written to file.").addRoutingAttribute(CISE_TO_LS));
        return new UpdateLegacySystemResult(resultAckCode);
    }

    private String saveAsFile(String messageId, String pushMessageXML) {
        String resultAckCode;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("YYYYMMddHHmmss");
        String processedTimestamp = simpleDateFormat.format(Date.from(Calendar.getInstance().toInstant()));
        String constructedFilename ="messageXML_" + processedTimestamp + ".xml";
        File outputFile = new File(config.getOutputDirectory(), constructedFilename);
        try (FileOutputStream fos = new FileOutputStream(outputFile); DataOutputStream outStream = new DataOutputStream(new BufferedOutputStream(fos))) {
            outStream.writeBytes(pushMessageXML);
            resultAckCode = UPDATE_LEGACY_SYSTEM_STATUS_OK;
        } catch (IOException exception) {
            log.error(of(PUSH, messageId, "Error while writing to file: " + exception.getMessage()).addRoutingAttribute(CISE_TO_LS), exception);
            resultAckCode = UPDATE_LEGACY_SYSTEM_STATUS_ERROR;
        }
        return resultAckCode;
    }
}
