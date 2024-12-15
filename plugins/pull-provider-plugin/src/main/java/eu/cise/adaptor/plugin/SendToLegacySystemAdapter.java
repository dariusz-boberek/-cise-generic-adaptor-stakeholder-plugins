package eu.cise.adaptor.plugin;

import eu.cise.adaptor.core.common.logging.AdaptorLogger;
import eu.cise.adaptor.core.common.logging.LogConfig;
import eu.cise.adaptor.core.common.message.MessageTypeEnum;
import eu.cise.adaptor.core.servicehandler.domain.RegisteredMessage;
import eu.cise.adaptor.core.servicehandler.port.out.SendToLegacySystemPort;
import eu.cise.adaptor.core.servicehandler.port.out.UpdateLegacySystemResult;
import eu.cise.adaptor.plugin.config.PullProviderPluginConfig;
import eu.cise.servicemodel.v1.message.Acknowledgement;
import eu.cise.servicemodel.v1.message.AcknowledgementType;
import eu.cise.servicemodel.v1.message.ConditionOperatorType;
import eu.cise.servicemodel.v1.message.Message;
import eu.cise.servicemodel.v1.message.PayloadSelector;
import eu.cise.servicemodel.v1.message.PullRequest;
import eu.cise.servicemodel.v1.message.SelectorCondition;
import eu.eucise.xml.DefaultXmlMapper;
import eu.eucise.xml.XmlMapper;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;

import javax.xml.xpath.XPathExpressionException;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import static eu.cise.adaptor.core.common.logging.LoggerMessage.of;
import static eu.cise.adaptor.core.common.logging.MessageRouteValue.CISE_TO_LS;
import static eu.cise.adaptor.core.common.logging.MessageRouteValue.LS_TO_CISE;
import static eu.cise.adaptor.core.common.message.MessageTypeEnum.ASYNC_ACK;
import static eu.cise.adaptor.core.common.message.MessageTypeEnum.PULL_REQUEST;
import static eu.cise.adaptor.core.common.message.MessageTypeEnum.PUSH;
import static eu.cise.adaptor.core.common.message.MessageTypeEnum.SYNC_ACK;
import static eu.cise.adaptor.plugin.xpath.CisePayloadSelectorToXPath.getCiseConditionOperatorType;
import static eu.cise.adaptor.plugin.xpath.CisePayloadSelectorToXPath.getCiseConditionValue;
import static eu.cise.adaptor.plugin.xpath.CisePayloadSelectorToXPath.getSampleXPathForCiseMessage;
import static eu.cise.adaptor.plugin.xpath.JooqUtils.createCondition;

public class SendToLegacySystemAdapter implements SendToLegacySystemPort {

    private static final AdaptorLogger log = LogConfig.configureLogging(SendToLegacySystemAdapter.class);
    private static final String CURRENT_PULL_REQUEST_MESSAGE_XML = "/tmp/currentPullRequestMessageForReferenceId.xml";

    public static final String UPDATE_LEGACY_SYSTEM_STATUS_ERROR = "error";
    public static final String UPDATE_LEGACY_SYSTEM_STATUS_OK = "ok";

    private final PullProviderPluginConfig config;
    private final XmlMapper xmlMapper;

    public SendToLegacySystemAdapter(PullProviderPluginConfig config) {
        this.config = config;
        this.xmlMapper = new DefaultXmlMapper.Pretty();
    }

    @Override
    public UpdateLegacySystemResult updateLegacySystem(RegisteredMessage newRegisteredMessage, String messageId, List<RegisteredMessage> messagesHistory, String contextId) {

        log.debug(of("pull-provider-plugin updateLegacySystem called").addRoutingAttribute(CISE_TO_LS));
        if (newRegisteredMessage == null || newRegisteredMessage.getCiseMessage() == null) {
            throw new IllegalArgumentException("message was null");
        }

        Message message = newRegisteredMessage.getCiseMessage();
        log.info(of(PULL_REQUEST, message.getMessageID(), "Translation message processed.").addRoutingAttribute(CISE_TO_LS));

        if (message instanceof PullRequest) {
            return handlePullRequest((PullRequest) message);
        } else if (message instanceof Acknowledgement) {
            return handleAcknowledgement((Acknowledgement) message);
        } else {
            log.error(of("Message Type not recognized {}", xmlMapper.toXML(message)).addRoutingAttribute(LS_TO_CISE));
            throw new IllegalArgumentException("Message class handling not implemented " + message.getClass().getCanonicalName());
        }
    }

    private UpdateLegacySystemResult handlePullRequest(PullRequest pullRequest) {
        // Handle the PayloadSelector if present
        String ackCode = issueRequestToLegacySystem(pullRequest);

        //backup for pull response data (since there is no legacy system that could store reference message ID)
        saveAsCiseFile(pullRequest);

        return new UpdateLegacySystemResult(ackCode);
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

    private String issueRequestToLegacySystem(PullRequest pullRequest) {
        String messageId = pullRequest.getMessageID();

        try {
            PayloadSelector cisePayloadSelector = pullRequest.getPayloadSelector();
            if (cisePayloadSelector != null) {
                for (SelectorCondition selectorCondition : cisePayloadSelector.getSelectors()) {

                    // gets expression operator (for example: equal, not equal)
                    ConditionOperatorType conditionOperatorType = getCiseConditionOperatorType(selectorCondition);

                    // gets value that selector is pointing to
                    String conditionValue = getCiseConditionValue(selectorCondition, pullRequest);

                    // showcasing possible way to create xPath expression to obtain some data from larger XML
                    String sampleXPathForCiseMessage = getSampleXPathForCiseMessage(selectorCondition, conditionOperatorType, conditionValue);
                    log.info(of(PULL_REQUEST, messageId, "sampleXPathForCiseMessage: {}", sampleXPathForCiseMessage).addRoutingAttribute(CISE_TO_LS));

                    // showcasing possible way to create SQL query to obtain some data - this query could be executed against legacy system for example
                    String sampleSQLQueryForLegacySystemCall = getSampleSQLQueryForLegacySystemCall(conditionOperatorType, conditionValue);
                    log.info(of(PULL_REQUEST, messageId, "sampleSQLQueryForLegacySystemCall: {}", sampleSQLQueryForLegacySystemCall).addRoutingAttribute(CISE_TO_LS));
                }
            }
        } catch (XPathExpressionException | IOException exception) {
            log.error(of(PULL_REQUEST, messageId, "Error while digesting pullRequest").addRoutingAttribute(CISE_TO_LS), exception);
            return UPDATE_LEGACY_SYSTEM_STATUS_ERROR;
        }
        return UPDATE_LEGACY_SYSTEM_STATUS_OK;
    }

    private String getSampleSQLQueryForLegacySystemCall(ConditionOperatorType conditionOperatorType, String conditionValue) throws IOException, XPathExpressionException {
        String selectField = "vessel_name";
        String conditionField = "imo_number";
        String tableName = "vessel";

        Condition condition = createCondition(conditionOperatorType, DSL.field(conditionField), conditionValue);
        DSLContext tempContext = DSL.using(SQLDialect.H2);
        return tempContext.select(DSL.field(selectField))
                .from(DSL.table(tableName))
                .where(condition)
                .getSQL(ParamType.INLINED);
    }

    private MessageTypeEnum identifyTypeOfAck(Acknowledgement message) {
        if (message.getAny() == null || message.getAny().getElementsByTagName("Signature") == null) {
            return SYNC_ACK;
        } else {
            return ASYNC_ACK;
        }
    }

    private void saveAsCiseFile(PullRequest pullRequest) {
        XmlMapper xmlMapperNotValidating = new DefaultXmlMapper.PrettyNotValidating();
        try (FileOutputStream fos = new FileOutputStream(CURRENT_PULL_REQUEST_MESSAGE_XML); DataOutputStream outStream = new DataOutputStream(new BufferedOutputStream(fos))) {
            String ciseMessageXml = xmlMapperNotValidating.toXML(pullRequest);
            outStream.writeBytes(ciseMessageXml);
        } catch (IOException exception) {
            log.error(of(PUSH, pullRequest.getMessageID(), "Error while writing to file: " + exception.getMessage()).addRoutingAttribute(CISE_TO_LS), exception);
        }
    }


}