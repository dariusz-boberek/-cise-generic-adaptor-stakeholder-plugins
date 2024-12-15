package eu.cise.adaptor.plugin;

import eu.cise.adaptor.core.common.exceptions.CiseAdaptorValidationException;
import eu.cise.adaptor.core.common.logging.AdaptorLogger;
import eu.cise.adaptor.core.common.logging.LogConfig;
import eu.cise.adaptor.core.servicehandler.domain.context.MessageDataContext;
import eu.cise.adaptor.core.servicehandler.port.in.ReceiveFromLegacySystemPort;
import eu.cise.adaptor.core.servicehandler.service.ServiceHandlerMessageBuilderService;
import eu.cise.adaptor.plugin.config.PushProviderPluginConfig;
import eu.cise.servicemodel.v1.service.Service;
import eu.eucise.xml.DefaultXmlMapper;
import eu.eucise.xml.XmlMapper;

public class ReceiveFromLegacySystemAdapter extends ReceiveFromLegacySystemPort {
    PushProviderPluginConfig pluginConfig;
    private final static AdaptorLogger logger = LogConfig.configureLogging(ReceiveFromLegacySystemAdapter.class);

    private XmlMapper xmlMapper;


    public ReceiveFromLegacySystemAdapter(ServiceHandlerMessageBuilderService serviceHandlerMessageBuilderService, Service serviceFromConfiguration, PushProviderPluginConfig pluginConfig) {
        super(serviceHandlerMessageBuilderService, serviceFromConfiguration);
        this.pluginConfig = pluginConfig;
        this.xmlMapper = new DefaultXmlMapper.NotValidating();
    }


    @Override
    public MessageDataContext handleCustomTranslation(String s, MessageDataContext.MessageDataContextManager messageDataContextManager) throws CiseAdaptorValidationException {
        // this method does not need to be implemented by this plugin as the translation of the messages happens in the VesselCSVTranslator class
        // and the VesselInformationDeliver class when it calls the handleIncomingLegacyData method has already added the translated CISE Payload in the
        // MessageContextManager
        return messageDataContextManager.buildContext();
    }
}
