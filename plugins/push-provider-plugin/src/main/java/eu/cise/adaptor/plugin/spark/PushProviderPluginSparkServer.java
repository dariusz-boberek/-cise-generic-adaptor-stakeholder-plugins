package eu.cise.adaptor.plugin.spark;

import eu.cise.adaptor.core.common.logging.AdaptorLogger;
import eu.cise.adaptor.core.common.logging.LogConfig;
import eu.cise.adaptor.core.servicehandler.port.in.ReceiveFromLegacySystemPort;
import eu.cise.adaptor.core.servicehandler.port.out.api.enums.ServiceRole;
import eu.cise.adaptor.core.servicehandler.port.out.api.enums.ServiceStatus;
import eu.cise.adaptor.core.servicehandler.port.out.api.enums.SortByService;
import eu.cise.adaptor.core.servicehandler.port.out.api.enums.SortOrder;
import eu.cise.adaptor.core.servicehandler.port.out.service_registry_openapi_yaml.api.ServiceApi;
import eu.cise.adaptor.core.servicehandler.port.out.service_registry_openapi_yaml.model.ResponseDTOServiceDTO;
import eu.cise.adaptor.core.servicehandler.port.out.service_registry_openapi_yaml.model.ServiceDTO;
import eu.cise.adaptor.plugin.config.PushProviderPluginConfig;
import eu.eucise.xml.DefaultXmlMapper;
import eu.eucise.xml.XmlMapper;
import org.apache.http.HttpHeaders;
import spark.Request;
import spark.Response;

import javax.ws.rs.core.MediaType;
import java.net.HttpURLConnection;
import java.util.List;

import static eu.cise.adaptor.core.common.logging.LoggerMessage.of;
import static eu.cise.adaptor.core.common.logging.MessageRouteValue.LS_TO_CISE;
import static spark.Spark.awaitInitialization;
import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.stop;

/**
 * This class encapsulates a Spark server that listens for incoming requests to access the Node API to get the active
 * list of matching services
 */
public class PushProviderPluginSparkServer {

    private static final AdaptorLogger logger = LogConfig.configureLogging(PushProviderPluginSparkServer.class);
    private final XmlMapper xmlMapper;
    private final ReceiveFromLegacySystemPort receiveFromLegacySystemPort;
    private final PushProviderPluginConfig config;

    private final ServiceApi serviceApi;

    public PushProviderPluginSparkServer(PushProviderPluginConfig config, ReceiveFromLegacySystemPort receiveFromLegacySystemPort, ServiceApi serviceApi) {
        this.config = config;
        this.xmlMapper = new DefaultXmlMapper();
        this.receiveFromLegacySystemPort = receiveFromLegacySystemPort;
        this.serviceApi = serviceApi;
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
            Integer limit = 100;
            Integer offset = 0;
            String serviceId = null;
            String serviceOperation = config.getServiceOperation();
            String serviceRole = ServiceRole.CONSUMER.value();
            List<String> serviceStatusList = List.of(ServiceStatus.ONLINE.value());
            String serviceType = config.getServiceType();
            Boolean smart = null;
            String sortBy = SortByService.SERVICE_ID.value();
            String sortOrder = SortOrder.ASC.value();
            ResponseDTOServiceDTO services = serviceApi.servicesGet(
                    limit,
                    offset,
                    serviceId,
                    serviceOperation,
                    serviceRole,
                    serviceStatusList,
                    serviceType,
                    smart,
                    sortBy,
                    sortOrder
            );
            StringBuilder result = new StringBuilder();
            for (ServiceDTO serviceDTO : services.getData()) {
                logger.info(of("Service found with id: {} is: {}", serviceDTO.getServiceId(), serviceDTO));
                result.append("<p>Service found with: <br>")
                        .append("Service Id: ").append(serviceDTO.getServiceId()).append("</br>")
                        .append("Service Status: "). append(serviceDTO.getServiceStatus()).append("</br>")
                        .append("Service Role: "). append(serviceDTO.getServiceRole()).append("</br>")
                        .append("Service Type: "). append(serviceDTO.getServiceType()).append("</br>")
                        .append("</p></br>");
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
