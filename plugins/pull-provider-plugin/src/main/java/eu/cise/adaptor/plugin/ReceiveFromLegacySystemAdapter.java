package eu.cise.adaptor.plugin;

import eu.cise.adaptor.core.common.exceptions.CiseAdaptorValidationException;
import eu.cise.adaptor.core.common.logging.AdaptorLogger;
import eu.cise.adaptor.core.common.logging.LogConfig;
import eu.cise.adaptor.core.servicehandler.domain.context.MessageDataContext;
import eu.cise.adaptor.core.servicehandler.port.in.ReceiveFromLegacySystemPort;
import eu.cise.adaptor.core.servicehandler.service.ServiceHandlerMessageBuilderService;
import eu.cise.servicemodel.v1.service.Service;
import eu.eucise.xml.DefaultXmlMapper;
import eu.eucise.xml.XmlMapper;

public class ReceiveFromLegacySystemAdapter extends ReceiveFromLegacySystemPort {

    private final static AdaptorLogger logger = LogConfig.configureLogging(ReceiveFromLegacySystemAdapter.class);

    private final XmlMapper xmlMapper;

    public ReceiveFromLegacySystemAdapter(ServiceHandlerMessageBuilderService serviceHandlerMessageBuilderService, Service serviceInformation) {
        super(serviceHandlerMessageBuilderService, serviceInformation);
        xmlMapper = new DefaultXmlMapper.NotValidating();
    }

    @Override
    public MessageDataContext handleCustomTranslation(String legacySystemPayload, MessageDataContext.MessageDataContextManager messageDataContextManager) throws CiseAdaptorValidationException {
        // no need to implement this method here because the PullProviderPluginReceiverServer handlePost assigns the payload to the MessageDataContextManager
        return messageDataContextManager.buildContext();
    }

}