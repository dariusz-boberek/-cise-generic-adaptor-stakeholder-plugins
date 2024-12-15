package eu.cise.adaptor.plugin.spark;

import eu.cise.adaptor.core.common.logging.AdaptorLogger;
import eu.cise.adaptor.core.common.logging.LogConfig;
import eu.cise.adaptor.core.servicehandler.domain.RegisteredMessage;
import eu.cise.adaptor.core.servicehandler.domain.context.MessageDataContext;
import eu.cise.adaptor.core.servicehandler.port.in.ReceiveFromLegacySystemPort;
import eu.cise.adaptor.plugin.config.DiscoveryProfileConfigurationReader;
import eu.cise.adaptor.plugin.config.SubscribeConsumerPluginConfig;
import eu.cise.servicemodel.v1.message.Acknowledgement;
import eu.cise.servicemodel.v1.message.AcknowledgementType;
import eu.cise.servicemodel.v1.message.PriorityType;
import eu.cise.servicemodel.v1.service.Service;
import eu.cise.servicemodel.v1.service.ServiceOperationType;
import eu.cise.servicemodel.v1.service.ServiceProfile;
import eu.cise.servicemodel.v1.service.ServiceRoleType;
import eu.cise.servicemodel.v1.service.ServiceType;
import eu.eucise.helpers.AckBuilder;
import eu.eucise.xml.DefaultXmlMapper;
import eu.eucise.xml.XmlMapper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpHeaders;
import spark.Request;
import spark.Response;

import javax.ws.rs.core.MediaType;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static eu.cise.adaptor.core.common.logging.LoggerMessage.of;
import static eu.cise.adaptor.core.common.logging.MessageRouteValue.LS_TO_CISE;
import static eu.eucise.helpers.AckBuilder.newAck;
import static java.util.stream.Collectors.toList;
import static spark.Spark.awaitInitialization;
import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.stop;

/**
 * This class encapsulates a Spark server that listens for incoming messages coming from the Legacy System connected to the adaptor
 */
public class SubscribeConsumerPluginReceiverServer {

    private static final AdaptorLogger logger = LogConfig.configureLogging(SubscribeConsumerPluginReceiverServer.class);
    private final XmlMapper xmlMapper;
    private final ReceiveFromLegacySystemPort receiveFromLegacySystemPort;
    private final SubscribeConsumerPluginConfig config;

    private static final List<ServiceProfile> configDiscoveryProfiles;

    static {
        DiscoveryProfileConfigurationReader discoveryProfileConfigurationReader = new DiscoveryProfileConfigurationReader();
        configDiscoveryProfiles = discoveryProfileConfigurationReader.getConfiguredProfiles(ServiceRoleType.PROVIDER);
    }

    public SubscribeConsumerPluginReceiverServer(SubscribeConsumerPluginConfig config, ReceiveFromLegacySystemPort receiveFromLegacySystemPort) {
        this.config = config;
        this.xmlMapper = new DefaultXmlMapper();
        this.receiveFromLegacySystemPort = receiveFromLegacySystemPort;
    }

    /**
     * Start the server
     */
    public void start() {
        port(config.getHttpPort()); // defining spark port
        get(config.getKnownContext(), this::handleGetForKnownPattern); // define known context endpoint
        get(config.getUnknownContext(), this::handleGetForUnknownPattern); // define unknown context endpoint
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
        if (serviceIdParam == null || serviceIdParam.trim().isEmpty()) {
            response.status(HttpURLConnection.HTTP_BAD_REQUEST);
            return "Parameter 'service-id' is missing";
        }

        String decodedServiceId = URLDecoder.decode(serviceIdParam.trim(), StandardCharsets.UTF_8);
        logger.info(of("Received serviceId: {}", serviceIdParam));

        try {
            Service recipientService = new Service();
            recipientService.setServiceID(decodedServiceId);
            recipientService.setServiceOperation(ServiceOperationType.SUBSCRIBE);
            recipientService.setServiceRole(ServiceRoleType.PROVIDER);
            recipientService.setServiceType(ServiceType.fromValue(config.getRecipientServiceType()));

            // Create the MessageDataContextManager
            MessageDataContext.MessageDataContextManager messageDataContextManager = MessageDataContext.getManager()
                    .initializeSubscribeConsumer(List.of(recipientService));

            // the legacySystemPayload should be null and no payload should be passed to the handleIncomingLegacyData method neither explicitly nor from within
            // the cisePayload method of the MessageDataContextManager
            List<Pair<RegisteredMessage, Acknowledgement>> genericAdaptorResultList = receiveFromLegacySystemPort.handleIncomingLegacyData(messageDataContextManager);
            return handleLegacySystemReply(response, genericAdaptorResultList);
        } catch (Exception e) {
            logger.error(of("Exception occurred in Spark Server {} ", e.getMessage()), e);
            response.status(500);
            return "Error processing data";
        }
    }


    private String handleGetForUnknownPattern(Request request, Response response) {
        String serviceProfileIndexParam = request.queryParams("service-profile-index");

        if (StringUtils.isEmpty(serviceProfileIndexParam)) {
            response.status(HttpURLConnection.HTTP_BAD_REQUEST);
            return "Parameter 'service-profile-index' is missing";
        }

        try {
            logger.info(of("Received serviceProfileIndexes {}", serviceProfileIndexParam));

            List<Integer> serviceProfileIndexes = Arrays.stream(serviceProfileIndexParam.split(","))
                    .map(String::trim)
                    .map(Integer::parseInt)
                    .collect(toList());

            if (serviceProfileIndexes == null || serviceProfileIndexes.isEmpty()) {
                throw new IllegalArgumentException("requestDiscoveryProfilesIndexes cannot be null or empty for unknown request");
            }

            List<ServiceProfile> discoveryProfiles = serviceProfileIndexes.stream()
                    .map(configDiscoveryProfiles::get)
                    .collect(toList());

            // Create the MessageDataContextManager for the unknown pattern
            MessageDataContext.MessageDataContextManager messageDataContextManager = MessageDataContext.getManager()
                    .initializeSubscribeConsumerUnknown(discoveryProfiles);

            // Handle incoming legacy data
            List<Pair<RegisteredMessage, Acknowledgement>> genericAdaptorResultList = receiveFromLegacySystemPort.handleIncomingLegacyData(messageDataContextManager);

            // Prepare and send the response to the caller
            response.type(MediaType.APPLICATION_JSON);
            response.status(HttpURLConnection.HTTP_OK);
            return handleLegacySystemReply(response, genericAdaptorResultList);

        } catch (Exception e) {
            logger.error(of("Exception occurred in Spark Server {} ", e.getMessage()), e);
            response.type(MediaType.TEXT_PLAIN);
            response.status(HttpURLConnection.HTTP_INTERNAL_ERROR);
            return "Error processing data";
        }
    }

    private String handleLegacySystemReply(Response response, List<Pair<RegisteredMessage, Acknowledgement>> genericAdaptorResultList) {

        AckBuilder ackBuilder = newAck()
                .id(UUID.randomUUID().toString())
                .creationDateTime(new Date())
                .priority(PriorityType.HIGH)
                .isRequiresAck(false);

        if (CollectionUtils.isNotEmpty(genericAdaptorResultList)) {
            // Check if all acknowledgments are SUCCESS
            boolean allSuccess = genericAdaptorResultList.stream()
                    .allMatch(pair -> AcknowledgementType.SUCCESS.equals(pair.getValue().getAckCode()));

            if (allSuccess) {
                ackBuilder.ackCode(AcknowledgementType.SUCCESS)
                        .ackDetail("All acknowledgements succeeded.");
                response.status(HttpURLConnection.HTTP_OK);
            } else {
                ackBuilder.ackCode(AcknowledgementType.SERVER_ERROR)
                        .ackDetail("One or more acknowledgement was not successful");
                response.status(HttpURLConnection.HTTP_SERVER_ERROR);
            }
        } else {
            ackBuilder.ackCode(AcknowledgementType.BAD_REQUEST)
                    .ackDetail("No acknowledgement received");
            response.status(HttpURLConnection.HTTP_BAD_REQUEST);
        }

        response.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML);
        Acknowledgement generatedAck = ackBuilder.build();
        // explicitly set the payload to null for the xmlMapper validation to work
        generatedAck.setPayload(null);
        return xmlMapper.toXML(generatedAck);
    }

}
