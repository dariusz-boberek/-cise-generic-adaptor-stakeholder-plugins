package eu.cise.adaptor.plugin;

import eu.cise.adaptor.core.common.logging.AdaptorLogger;
import eu.cise.adaptor.core.common.logging.LogConfig;
import eu.cise.adaptor.core.servicehandler.port.in.ReceiveFromLegacySystemPort;
import eu.cise.adaptor.core.servicehandler.port.out.SendToLegacySystemPort;
import eu.cise.adaptor.core.servicehandler.port.out.ServiceHandlerManagementAPIPort;
import eu.cise.adaptor.core.servicehandler.service.ServiceHandlerMessageBuilderService;
import eu.cise.adaptor.plugin.config.PullProviderPluginConfig;
import eu.cise.adaptor.plugin.spark.PullProviderPluginReceiverServer;
import eu.cise.servicemodel.v1.service.Service;
import org.aeonbits.owner.ConfigFactory;

import static eu.cise.adaptor.core.common.logging.LoggerMessage.of;
import static eu.cise.adaptor.core.common.logging.MessageRouteValue.CONTEXT;

/**
 * This class is the main connection point between the Generic Adaptor and the plugins. It extends the CiseBasePlugin.
 * All relevant methods should be implemented for the plugin to work correctly with the Generic Adaptor
 */
public class PullProviderPlugin extends CiseBasePlugin {

    /* The Generic Adaptor uses its own logging subsystem so this AdaptorLogger is required so that logs are uniform between the
Generic Adaptor and the plugins
*/
    private final static AdaptorLogger logger = LogConfig.configureLogging(PullProviderPlugin.class);

    private ReceiveFromLegacySystemPort receiveFromLegacySystemPort;
    /**
     * This function is called when the plugin is initialized through the Generic Adaptor
     * In this example it starts a server for receiving data from a legacy system using a specified configuration.
     * This method is responsible for starting the internal REST server
     * and autowiring the `ReceiveFromLegacySystemPort` instance for the  listener.
     */
    @Override
    public void bootStrapPlugin(ServiceHandlerMessageBuilderService serviceHandlerMessageBuilderService, ServiceHandlerManagementAPIPort serviceHandlerManagementAPIPort) {
        logger.info(of("Bootstrap Plugin called: {}.", getPluginConfiguration().pluginConfigToString()).addRoutingAttribute(CONTEXT));
        PullProviderPluginReceiverServer server = new PullProviderPluginReceiverServer((PullProviderPluginConfig) getPluginConfiguration(), getReceiveFromLegacySystemPort(serviceHandlerMessageBuilderService));
        server.start();
    }

    @Override
    public ReceiveFromLegacySystemPort getReceiveFromLegacySystemPort(ServiceHandlerMessageBuilderService serviceHandlerMessageBuilderService) {
        if(receiveFromLegacySystemPort == null) {
            Service serviceInformation = getPluginConfiguration().generateCISEServiceFromConfig();
            receiveFromLegacySystemPort = new ReceiveFromLegacySystemAdapter(serviceHandlerMessageBuilderService, serviceInformation);
        }
        return receiveFromLegacySystemPort;
    }

    @Override
    public SendToLegacySystemPort getSendToLegacySystemPort() {
        return new SendToLegacySystemAdapter((PullProviderPluginConfig) getPluginConfiguration());
    }

    @Override
    public PluginConfig getPluginConfiguration() {
        PullProviderPluginConfig adaptorConfig = ConfigFactory.create(PullProviderPluginConfig.class);
        return adaptorConfig;
    }
}
