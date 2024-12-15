package eu.cise.adaptor.plugin;

import eu.cise.adaptor.core.common.exceptions.CiseAdaptorRuntimeException;
import eu.cise.adaptor.core.common.logging.AdaptorLogger;
import eu.cise.adaptor.core.common.logging.LogConfig;
import eu.cise.adaptor.core.servicehandler.port.in.ReceiveFromLegacySystemPort;
import eu.cise.adaptor.core.servicehandler.port.out.SendToLegacySystemPort;
import eu.cise.adaptor.core.servicehandler.port.out.ServiceHandlerManagementAPIPort;
import eu.cise.adaptor.core.servicehandler.service.ServiceHandlerMessageBuilderService;
import eu.cise.adaptor.plugin.config.SubscribeConsumerPluginConfig;
import eu.cise.adaptor.plugin.spark.SubscribeConsumerPluginReceiverServer;
import org.aeonbits.owner.ConfigFactory;

import java.io.File;

import static eu.cise.adaptor.core.common.logging.LoggerMessage.of;
import static eu.cise.adaptor.core.common.logging.MessageRouteValue.CONTEXT;
import static eu.cise.adaptor.core.common.logging.MessageRouteValue.LS_TO_CISE;

public class SubscribeConsumerPlugin extends CiseBasePlugin {

    private final static AdaptorLogger logger = LogConfig.configureLogging(SubscribeConsumerPlugin.class);

    @Override
    public void bootStrapPlugin(ServiceHandlerMessageBuilderService serviceHandlerMessageBuilderService, ServiceHandlerManagementAPIPort serviceHandlerManagementAPIPort) {
        logger.info(of("Bootstrap Plugin called: {}.", getPluginConfiguration().pluginConfigToString()).addRoutingAttribute(CONTEXT));

        SubscribeConsumerPluginReceiverServer server = new SubscribeConsumerPluginReceiverServer((SubscribeConsumerPluginConfig) getPluginConfiguration(), getReceiveFromLegacySystemPort(serviceHandlerMessageBuilderService));
        server.start();

        // validate output directory
        File outputDirectory = new File(((SubscribeConsumerPluginConfig) getPluginConfiguration()).getOutputDirectory());
        if (!(outputDirectory.exists() && outputDirectory.isDirectory() && outputDirectory.canWrite())) {
            String errorMsg = "Directory does not exist or cannot be read: " + outputDirectory.getAbsolutePath();
            logger.error(of(errorMsg).addRoutingAttribute(LS_TO_CISE));
            throw new CiseAdaptorRuntimeException(errorMsg);
        }
    }

    @Override
    public ReceiveFromLegacySystemPort getReceiveFromLegacySystemPort(ServiceHandlerMessageBuilderService serviceHandlerMessageBuilderService) {
        return new ReceiveFromLegacySystemAdapter(serviceHandlerMessageBuilderService, getPluginConfiguration().generateCISEServiceFromConfig());
    }

    @Override
    public SendToLegacySystemPort getSendToLegacySystemPort() {
        return new SendToLegacySystemAdapter((SubscribeConsumerPluginConfig) getPluginConfiguration());
    }

    @Override
    public PluginConfig getPluginConfiguration() {
        return ConfigFactory.create(SubscribeConsumerPluginConfig.class);
    }

}
