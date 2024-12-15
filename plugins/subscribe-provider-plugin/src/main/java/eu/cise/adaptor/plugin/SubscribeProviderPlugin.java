package eu.cise.adaptor.plugin;

import eu.cise.adaptor.core.common.logging.AdaptorLogger;
import eu.cise.adaptor.core.common.logging.LogConfig;
import eu.cise.adaptor.core.servicehandler.port.in.ReceiveFromLegacySystemPort;
import eu.cise.adaptor.core.servicehandler.port.out.SendToLegacySystemPort;
import eu.cise.adaptor.core.servicehandler.port.out.ServiceHandlerManagementAPIPort;
import eu.cise.adaptor.core.servicehandler.service.ServiceHandlerMessageBuilderService;
import eu.cise.adaptor.plugin.config.SubscribeProviderPluginConfig;
import eu.cise.adaptor.plugin.spark.SubscribeProviderPluginSparkServer;
import org.aeonbits.owner.ConfigFactory;

import static eu.cise.adaptor.core.common.logging.LoggerMessage.of;
import static eu.cise.adaptor.core.common.logging.MessageRouteValue.CONTEXT;

public class SubscribeProviderPlugin extends CiseBasePlugin {

    private static final AdaptorLogger logger = LogConfig.configureLogging(SubscribeProviderPlugin.class);

    private VesselCSVFileHandler csvFileHandler;

    @Override
    public void bootStrapPlugin(ServiceHandlerMessageBuilderService serviceHandlerMessageBuilderService, ServiceHandlerManagementAPIPort serviceHandlerManagementAPIPort) {
        logger.info(of("Bootstrap Plugin called: {}.", getPluginConfiguration().pluginConfigToString()).addRoutingAttribute(CONTEXT));
        SubscribeProviderPluginSparkServer server = new SubscribeProviderPluginSparkServer((SubscribeProviderPluginConfig) getPluginConfiguration(), getReceiveFromLegacySystemPort(serviceHandlerMessageBuilderService), serviceHandlerManagementAPIPort.getSubscriptionApi());
        server.start();
        csvFileHandler = new VesselCSVFileHandler((SubscribeProviderPluginConfig) getPluginConfiguration(), getReceiveFromLegacySystemPort(serviceHandlerMessageBuilderService));
        csvFileHandler.start();
    }

    @Override
    public ReceiveFromLegacySystemPort getReceiveFromLegacySystemPort(ServiceHandlerMessageBuilderService serviceHandlerMessageBuilderService) {
        return new ReceiveFromLegacySystemAdapter(serviceHandlerMessageBuilderService, getPluginConfiguration().generateCISEServiceFromConfig(), (SubscribeProviderPluginConfig) getPluginConfiguration());
    }



    @Override
    public SendToLegacySystemPort getSendToLegacySystemPort() {
        return new SendToLegacySystemAdapter((SubscribeProviderPluginConfig)getPluginConfiguration());
    }

    @Override
    public PluginConfig getPluginConfiguration() {
        return ConfigFactory.create(SubscribeProviderPluginConfig.class);
    }
}
