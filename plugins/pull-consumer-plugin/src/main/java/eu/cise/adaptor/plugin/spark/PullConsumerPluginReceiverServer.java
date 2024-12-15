package eu.cise.adaptor.plugin.spark;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import eu.cise.adaptor.core.common.logging.AdaptorLogger;
import eu.cise.adaptor.core.common.logging.LogConfig;
import eu.cise.adaptor.core.servicehandler.domain.RegisteredMessage;
import eu.cise.adaptor.core.servicehandler.domain.context.MessageDataContext;
import eu.cise.adaptor.core.servicehandler.port.in.ReceiveFromLegacySystemPort;
import eu.cise.adaptor.plugin.VesselPullRequestContextBuilder;
import eu.cise.adaptor.plugin.config.PullConsumerPluginConfig;
import eu.cise.servicemodel.v1.message.Acknowledgement;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import spark.Request;
import spark.Response;

import javax.ws.rs.core.MediaType;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static eu.cise.adaptor.core.common.logging.LoggerMessage.of;
import static eu.cise.adaptor.core.common.logging.MessageRouteValue.LS_TO_CISE;
import static java.util.stream.Collectors.toList;
import static spark.Spark.awaitInitialization;
import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.stop;

/**
 * This class encapsulates a Spark server that listens for incoming messages coming from the Legacy System connected to the adaptor
 */
public class PullConsumerPluginReceiverServer {

    private static final AdaptorLogger logger = LogConfig.configureLogging(PullConsumerPluginReceiverServer.class);
    private final ObjectMapper jsonObjectMapper;
    private final ReceiveFromLegacySystemPort receiveFromLegacySystemPort;
    private final PullConsumerPluginConfig config;

    public PullConsumerPluginReceiverServer(PullConsumerPluginConfig config, ReceiveFromLegacySystemPort receiveFromLegacySystemPort) {
        this.config = config;
        this.receiveFromLegacySystemPort = receiveFromLegacySystemPort;
        this.jsonObjectMapper = createObjectMapper();
    }

    /**
     * Start the server
     */
    public void start() {
        port(config.getHttpPort());                             // defining spark port
        get(config.getKnownContext(), this::handleGetForKnownPattern);         // define known context endpoint
        get(config.getUnknownContext(), this::handleGetForUnknownPattern);       // define unknown context endpoint
        awaitInitialization();
        logger.info(of("Http Server Spark https port[{}] known-context[{}] unknown-context[{}]", config.getHttpPort(), config.getKnownContext(), config.getUnknownContext()).addRoutingAttribute(LS_TO_CISE));
    }

    /**
     * Stop the server
     *
     * @throws InterruptedException Interrupt exception
     */
    public void stopServer() throws InterruptedException {
        stop();
    }

    private String handleGetForKnownPattern(Request request, Response response) {

        String serviceIdParam = request.queryParams("service-id");
        String imoNumber = request.queryParams("imonumber");
        String isEqual = request.queryParams("equal");

        if (StringUtils.isEmpty(imoNumber) || StringUtils.isEmpty(isEqual) || StringUtils.isEmpty(serviceIdParam)) {
            response.status(HttpURLConnection.HTTP_BAD_REQUEST);
            return "Parameters are missing";
        }

        try {
            logger.info(of("Received serviceId {}, imoNumber {}, equal {}", serviceIdParam, imoNumber, isEqual));
            String decodedServiceId = URLDecoder.decode(serviceIdParam.trim(), StandardCharsets.UTF_8);

            // Is possible to have more than one serviceId, separate by comma
            List<String> recipientIdList = Arrays.asList(decodedServiceId.split(","));

            // Create the MessageDataContextManager for the PullRequest known
            MessageDataContext.MessageDataContextManager messageDataContextManager = VesselPullRequestContextBuilder
                    .forKnownPattern(config, recipientIdList)
                    .withDefaultResponseTimeout()
                    .withServiceCapabilitiesFromConfig()
                    .withVesselByIMONumber(imoNumber, "true".equalsIgnoreCase(isEqual))
                    .buildContextManager();

            // The legacySystemPayload should be null because is already contained in the messageDataContextManager
            List<Pair<RegisteredMessage, Acknowledgement>> genericAdaptorResultList = receiveFromLegacySystemPort.handleIncomingLegacyData(messageDataContextManager);

            // Prepare and send the response to the caller
            response.type(MediaType.APPLICATION_JSON);
            response.status(HttpURLConnection.HTTP_OK);
            return handleLegacySystemReply(genericAdaptorResultList);

        } catch (Exception e) {
            logger.error(of("Exception occurred in Spark Server {} ", e.getMessage()), e);
            response.type(MediaType.TEXT_PLAIN);
            response.status(HttpURLConnection.HTTP_INTERNAL_ERROR);
            return "Error processing data";
        }
    }

    private String handleGetForUnknownPattern(Request request, Response response) {

        String imoNumber = request.queryParams("imonumber");
        String isEqual = request.queryParams("equal");
        String serviceProfileIndexParam = request.queryParams("service-profile-index");

        if (StringUtils.isEmpty(imoNumber) || StringUtils.isEmpty(isEqual) || StringUtils.isEmpty(serviceProfileIndexParam)) {
            response.status(HttpURLConnection.HTTP_BAD_REQUEST);
            return "Parameters are missing";
        }

        try {
            logger.info(of("Received imoNumber {}, equal {}, serviceProfileIndexes {}", imoNumber, isEqual, serviceProfileIndexParam));

            List<Integer> serviceProfileIndexes = Arrays.stream(serviceProfileIndexParam.split(","))
                    .map(String::trim)
                    .map(Integer::parseInt)
                    .collect(toList());

            // Create the MessageDataContextManager for the PullRequest unknown
            MessageDataContext.MessageDataContextManager messageDataContextManager = VesselPullRequestContextBuilder
                    .forUnknownPattern(config, serviceProfileIndexes)
                    .withDefaultResponseTimeout()
                    .withServiceCapabilitiesFromConfig()
                    .withVesselByIMONumber(imoNumber, "true".equalsIgnoreCase(isEqual))
                    .buildContextManager();

            // The legacySystemPayload should be null because is already contained in the messageDataContextManager
            List<Pair<RegisteredMessage, Acknowledgement>> genericAdaptorResultList = receiveFromLegacySystemPort.handleIncomingLegacyData(messageDataContextManager);

            // Prepare and send the response to the caller
            response.type(MediaType.APPLICATION_JSON);
            response.status(HttpURLConnection.HTTP_OK);
            return handleLegacySystemReply(genericAdaptorResultList);

        } catch (Exception e) {
            logger.error(of("Exception occurred in Spark Server {} ", e.getMessage()), e);
            response.type(MediaType.TEXT_PLAIN);
            response.status(HttpURLConnection.HTTP_INTERNAL_ERROR);
            return "Error processing data";
        }
    }

    private String handleLegacySystemReply(List<Pair<RegisteredMessage, Acknowledgement>> resultList) throws JsonProcessingException {
        List<ResultDTO> resultDTOList = new ArrayList<>();
        for (var result : resultList) {
            var registeredMessage = result.getLeft();
            var ack = result.getRight();
            String recipientId = registeredMessage.getCiseMessage() != null && registeredMessage.getCiseMessage().getRecipient() != null
                    ? registeredMessage.getCiseMessage().getRecipient().getServiceID()
                    : "no recipientId, unknown pattern";
            resultDTOList.add(new ResultDTO(registeredMessage.getMessageId(), recipientId, ack.getAckCode().value(), ack.getAckDetail()));
        }

        return jsonObjectMapper.writeValueAsString(resultDTOList);
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper objectMapperLocal = new ObjectMapper();
        objectMapperLocal.registerModule(new JavaTimeModule());
        return objectMapperLocal;
    }

    private static class ResultDTO {

        public final String messageId;
        public final String recipientId;
        public final String ackCode;
        public final String ackDetail;

        public ResultDTO(String messageId, String recipientId, String ackCode, String ackDetail) {
            this.messageId = messageId;
            this.recipientId = recipientId;
            this.ackCode = ackCode;
            this.ackDetail = ackDetail;
        }
    }
}
