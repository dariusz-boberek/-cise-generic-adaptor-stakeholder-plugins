package eu.cise.adaptor.plugin.spark;


import eu.cise.adaptor.core.common.logging.AdaptorLogger;
import eu.cise.adaptor.core.common.logging.LogConfig;
import eu.cise.adaptor.core.servicehandler.domain.RegisteredMessage;
import eu.cise.adaptor.core.servicehandler.domain.context.MessageDataContext;
import eu.cise.adaptor.core.servicehandler.port.in.ReceiveFromLegacySystemPort;
import eu.cise.adaptor.plugin.config.PullProviderPluginConfig;
import eu.cise.servicemodel.v1.message.Acknowledgement;
import eu.cise.servicemodel.v1.message.AcknowledgementType;
import eu.cise.servicemodel.v1.message.Message;
import eu.cise.servicemodel.v1.message.PriorityType;
import eu.cise.servicemodel.v1.message.Push;
import eu.eucise.xml.DefaultXmlMapper;
import eu.eucise.xml.XmlMapper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpHeaders;
import spark.Request;
import spark.Response;

import javax.ws.rs.core.MediaType;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static eu.cise.adaptor.core.common.logging.LoggerMessage.of;
import static eu.eucise.helpers.AckBuilder.newAck;
import static spark.Spark.awaitInitialization;
import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.stop;


/**
 * This class encapsulates a Spark server that listens for incoming messages coming from the Legacy System connected to the adaptor
 */
public class PullProviderPluginReceiverServer {

    private final static AdaptorLogger logger = LogConfig.configureLogging(PullProviderPluginReceiverServer.class);
    private final XmlMapper xmlMapper;
    private final ReceiveFromLegacySystemPort receiveFromLegacySystemPort;
    private final PullProviderPluginConfig config;

    private static final String CURRENT_PUSH_MESSAGE_XML = "/tmp/currentPushMessage.xml";

    public PullProviderPluginReceiverServer(PullProviderPluginConfig config, ReceiveFromLegacySystemPort receiveFromLegacySystemPort) {
        this.config = config;
        this.xmlMapper = new DefaultXmlMapper();
        this.receiveFromLegacySystemPort = receiveFromLegacySystemPort;
    }

    /**
     * Start the server
     */
    public void start() {

        // Endpoint configuration
        port(config.getHttpPort()); // TODO: we need to find how we can configure this through property files
        post(config.getHttpContext(), this::handlePost); // incoming from legacy system

        awaitInitialization();

        logger.info(of("Http Server Spark https port[" + config.getHttpPort() + "] context[" + config.getHttpContext() + "]"));
    }

    /**
     * Stop the server
     *
     * @throws InterruptedException Interrupt exception
     */
    public void stopServer() throws InterruptedException {
        stop();
    }

    /**
     * Callback for post request received by the endpoint
     *
     * @param request
     * @param response
     * @return
     */
    private String handlePost(Request request, Response response) {
        try {
            response.type(MediaType.APPLICATION_JSON);
            String savedPushMessageXML = Files.readString(Paths.get(CURRENT_PUSH_MESSAGE_XML));
            Push savedPushMessage = xmlMapper.fromXML(savedPushMessageXML);
            String referencePullRequestXML = Files.readString(Paths.get("/tmp/currentPullRequestMessageForReferenceId.xml"));
            Message referencePullRequest = xmlMapper.fromXML(referencePullRequestXML);
            String referenceMessageIdFromPullRequest = referencePullRequest.getMessageID();

            MessageDataContext.MessageDataContextManager messageDataContextManager = MessageDataContext.getManager()
                    .cisePayload(savedPushMessage.getPayload())
                    .referenceMessageId(referenceMessageIdFromPullRequest)
                    .referenceMessage(receiveFromLegacySystemPort.getMessageById(referenceMessageIdFromPullRequest).orElse(null));

            List<Pair<RegisteredMessage, Acknowledgement>> genericAdaptorResultList = receiveFromLegacySystemPort.handleIncomingLegacyData(messageDataContextManager);
            return handleLegacySystemReply(response, referenceMessageIdFromPullRequest, genericAdaptorResultList);
        } catch (Exception e) {
            logger.error(of("exception : {}", e.getMessage()));
            response.status(500);
            return "Error processing JSON data";
        }
    }

    /**
     * Handle the response message received. It's a list, but because it is a PullResponse, the list contains only one message
     *
     * @param response                 Object used for the http communication
     * @param messageId                The messageId received from the LegacySystem
     * @param genericAdaptorResultList The generic adaptor results from the message elaboration in the domain.
     * @return The xml acknowledgment message
     */
    private String handleLegacySystemReply(Response response, String messageId, List<Pair<RegisteredMessage, Acknowledgement>> genericAdaptorResultList) {
        String xmlAckMessage;
        if (!CollectionUtils.isEmpty(genericAdaptorResultList) && genericAdaptorResultList.size() == 1) {
            Acknowledgement ackResult = genericAdaptorResultList.get(0).getValue();
            xmlAckMessage = getStandardAck(messageId, ackResult.getAckCode(), ackResult.getAckDetail());
        } else {
            xmlAckMessage = getStandardAck(messageId, AcknowledgementType.BAD_REQUEST, "Incorrect result received");
        }

        // Send back the Acknowledgment message
        response.status(HttpURLConnection.HTTP_OK);
        response.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML);
        return xmlAckMessage;
    }

    private String getStandardAck(String messageId, AcknowledgementType ackCode, String ackDetail) {
        Acknowledgement ack = newAck()
                .id(messageId + "_" + UUID.randomUUID())
                .correlationId(messageId)
                .creationDateTime(new Date())
                .priority(PriorityType.HIGH)
                .isRequiresAck(false)
                .ackCode(ackCode)
                .ackDetail(ackDetail)
                .build();
        ack.setPayload(null);
        return xmlMapper.toXML(ack);
    }
}
