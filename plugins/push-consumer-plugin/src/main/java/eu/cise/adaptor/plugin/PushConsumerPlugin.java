package eu.cise.adaptor.plugin;

import eu.cise.adaptor.core.common.logging.AdaptorLogger;
import eu.cise.adaptor.core.common.logging.LogConfig;
import eu.cise.adaptor.core.servicehandler.port.in.ReceiveFromLegacySystemPort;
import eu.cise.adaptor.core.servicehandler.port.out.SendToLegacySystemPort;
import eu.cise.adaptor.core.servicehandler.port.out.ServiceHandlerManagementAPIPort;
import eu.cise.adaptor.core.servicehandler.service.ServiceHandlerMessageBuilderService;
import eu.cise.adaptor.plugin.config.PushConsumerPluginConfig;
import org.aeonbits.owner.ConfigFactory;

import static eu.cise.adaptor.core.common.logging.LoggerMessage.of;
import static eu.cise.adaptor.core.common.logging.MessageRouteValue.CONTEXT;


/**
 * This class is the main connection point between the Generic Adaptor and the plugins. It extends the CiseBasePlugin.
 * All relevant methods should be implemented for the plugin to work correctly with the Generic Adaptor
 */
public class PushConsumerPlugin extends CiseBasePlugin {

    /* The Generic Adaptor uses its own logging subsystem so this AdaptorLogger is required so that logs are uniform between the
    Generic Adaptor and the plugins
    */
    private final static AdaptorLogger logger = LogConfig.configureLogging(PushConsumerPlugin.class);

    /**
     * This function is called when the plugin is initialized through the Generic Adaptor
     */
    @Override
    public void bootStrapPlugin(ServiceHandlerMessageBuilderService serviceHandlerMessageBuilderService, ServiceHandlerManagementAPIPort serviceHandlerManagementAPIPort) {
        logger.info(of("Bootstrap Plugin called: {}.", getPluginConfiguration().pluginConfigToString()).addRoutingAttribute(CONTEXT));
    }

    @Override
    public ReceiveFromLegacySystemPort getReceiveFromLegacySystemPort(ServiceHandlerMessageBuilderService serviceHandlerMessageBuilderService) {
        return null;
    }

    @Override
    public SendToLegacySystemPort getSendToLegacySystemPort() {
        return new SendToLegacySystemAdapter((PushConsumerPluginConfig) getPluginConfiguration());
    }

    @Override
    public PluginConfig getPluginConfiguration() {
        PushConsumerPluginConfig adaptorConfig = ConfigFactory.create(PushConsumerPluginConfig.class);
        return adaptorConfig;
    }
}
