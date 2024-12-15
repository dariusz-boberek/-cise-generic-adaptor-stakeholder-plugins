package eu.cise.adaptor.plugin;

import eu.cise.adaptor.core.common.logging.AdaptorLogger;
import eu.cise.adaptor.core.common.logging.LogConfig;
import eu.cise.adaptor.core.servicehandler.port.in.ReceiveFromLegacySystemPort;
import eu.cise.adaptor.core.servicehandler.port.out.SendToLegacySystemPort;
import eu.cise.adaptor.core.servicehandler.port.out.ServiceHandlerManagementAPIPort;
import eu.cise.adaptor.core.servicehandler.service.ServiceHandlerMessageBuilderService;
import eu.cise.adaptor.plugin.config.PushProviderPluginConfig;
import eu.cise.adaptor.plugin.spark.PushProviderPluginSparkServer;
import org.aeonbits.owner.ConfigFactory;

import static eu.cise.adaptor.core.common.logging.LoggerMessage.of;
import static eu.cise.adaptor.core.common.logging.MessageRouteValue.CONTEXT;

public class PushProviderPlugin extends CiseBasePlugin {

    private final static AdaptorLogger logger = LogConfig.configureLogging(PushProviderPlugin.class);

    private VesselCSVFileHandler csvFileHandler;

    @Override
    public void bootStrapPlugin(ServiceHandlerMessageBuilderService serviceHandlerMessageBuilderService, ServiceHandlerManagementAPIPort serviceHandlerManagementAPIPort) {
        logger.info(of("Bootstrap Plugin called: {}.", getPluginConfiguration().pluginConfigToString()).addRoutingAttribute(CONTEXT));
        PushProviderPluginSparkServer server = new PushProviderPluginSparkServer((PushProviderPluginConfig) getPluginConfiguration(), getReceiveFromLegacySystemPort(serviceHandlerMessageBuilderService), serviceHandlerManagementAPIPort.getServiceApi());
        server.start();
        csvFileHandler = new VesselCSVFileHandler((PushProviderPluginConfig) getPluginConfiguration(), getReceiveFromLegacySystemPort(serviceHandlerMessageBuilderService));
        csvFileHandler.start();
    }

    @Override
    public ReceiveFromLegacySystemPort getReceiveFromLegacySystemPort(ServiceHandlerMessageBuilderService serviceHandlerMessageBuilderService) {
        return new ReceiveFromLegacySystemAdapter(serviceHandlerMessageBuilderService, getPluginConfiguration().generateCISEServiceFromConfig(), (PushProviderPluginConfig) getPluginConfiguration());
    }



    @Override
    public SendToLegacySystemPort getSendToLegacySystemPort() {
        return new SendToLegacySystemAdapter((PushProviderPluginConfig)getPluginConfiguration());
    }

    @Override
    public PluginConfig getPluginConfiguration() {
        PushProviderPluginConfig adaptorConfig = ConfigFactory.create(PushProviderPluginConfig.class);
        return adaptorConfig;
    }
}
