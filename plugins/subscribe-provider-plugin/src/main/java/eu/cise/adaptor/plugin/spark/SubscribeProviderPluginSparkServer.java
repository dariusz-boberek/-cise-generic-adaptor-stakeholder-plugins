package eu.cise.adaptor.plugin.spark;

import eu.cise.adaptor.core.common.logging.AdaptorLogger;
import eu.cise.adaptor.core.common.logging.LogConfig;
import eu.cise.adaptor.core.servicehandler.port.in.ReceiveFromLegacySystemPort;
import eu.cise.adaptor.core.servicehandler.port.out.api.enums.SortBySubscription;
import eu.cise.adaptor.core.servicehandler.port.out.api.enums.SortOrder;
import eu.cise.adaptor.core.servicehandler.port.out.api.enums.SubscriptionStatus;
import eu.cise.adaptor.core.servicehandler.port.out.subscription_registry_openapi_yaml.api.SubscriptionApi;
import eu.cise.adaptor.core.servicehandler.port.out.subscription_registry_openapi_yaml.model.ResponseDTOSubscriptionDTO;
import eu.cise.adaptor.core.servicehandler.port.out.subscription_registry_openapi_yaml.model.SubscriptionDTO;
import eu.cise.adaptor.plugin.config.SubscribeProviderPluginConfig;
import eu.eucise.xml.DefaultXmlMapper;
import eu.eucise.xml.XmlMapper;
import org.apache.http.HttpHeaders;
import spark.Request;
import spark.Response;

import javax.ws.rs.core.MediaType;
import java.net.HttpURLConnection;
import java.time.LocalDate;

import static eu.cise.adaptor.core.common.logging.LoggerMessage.of;
import static eu.cise.adaptor.core.common.logging.MessageRouteValue.LS_TO_CISE;
import static spark.Spark.awaitInitialization;
import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.stop;

/**
 * This class encapsulates a Spark server that listens for incoming requests to access the Node API to get the active
 * list of subscribers
 */
public class SubscribeProviderPluginSparkServer {

    private static final AdaptorLogger logger = LogConfig.configureLogging(SubscribeProviderPluginSparkServer.class);
    private final XmlMapper xmlMapper;
    private final ReceiveFromLegacySystemPort receiveFromLegacySystemPort;
    private final SubscribeProviderPluginConfig config;

    private final SubscriptionApi subscriptionApi;

    public SubscribeProviderPluginSparkServer(SubscribeProviderPluginConfig config, ReceiveFromLegacySystemPort receiveFromLegacySystemPort, SubscriptionApi subscriptionApi) {
        this.config = config;
        this.xmlMapper = new DefaultXmlMapper();
        this.receiveFromLegacySystemPort = receiveFromLegacySystemPort;
        this.subscriptionApi = subscriptionApi;
    }

    /**
     * Start the server
     */
    public void start() {
        port(config.getHttpPort()); // defining spark port
        get(config.getHttpContext(), this::handleGet); // incoming request to subscribe to some provider
        awaitInitialization();
        logger.info(of("Http Server Spark https port[{}] context[{}]", config.getHttpPort(), config.getHttpContext()).addRoutingAttribute(LS_TO_CISE));
    }

    /**
     * Stop the server
     *
     * @throws InterruptedException Interrupt exception
     */
    public void stopServer() throws InterruptedException {
        stop();
    }

    private String handleGet(Request request, Response response) {

        try {
            LocalDate expireDateFrom = null;
            LocalDate expireDateTo = null;
            Integer limit = 100;
            Integer offset = 0;
            String providerServiceId = config.getServiceId();
            Boolean smart = null;
            String sortBy = SortBySubscription.SUBSCRIBER_SERVICE_ID.value();
            String sortOrder = SortOrder.ASC.value();
            String status = SubscriptionStatus.ACTIVE.value();
            String subscriberParticipantId = null;
            String subscriberServiceId = null;
            ResponseDTOSubscriptionDTO subscriptions = subscriptionApi.subscriptionsGet(expireDateFrom, expireDateTo, limit, offset, providerServiceId, smart, sortBy, sortOrder, status, subscriberParticipantId, subscriberServiceId);
            StringBuilder result = new StringBuilder();
            for (SubscriptionDTO subscriptionDTO : subscriptions.getData()){
                logger.info(of("Subscription for id: {} is: {}",subscriptionDTO.getSubscriberServiceId(), subscriptionDTO));
                result.append("<p>Subscription for id:").append(subscriptionDTO.getSubscriberServiceId()).append(" is: ").append(subscriptionDTO).append("</p></br>");
            }
            response.status(HttpURLConnection.HTTP_OK);
            response.header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML);
            return result.toString();
        } catch (Exception e) {
            logger.error(of("Exception occurred in Spark Server {} ", e.getMessage()), e);
            response.status(500);
            return "Error processing data. Error: " + e.getMessage();
        }
    }

}
