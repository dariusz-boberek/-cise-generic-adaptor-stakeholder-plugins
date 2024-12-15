package eu.cise.adaptor.plugin;

import eu.cise.adaptor.core.common.exceptions.CiseAdaptorValidationException;
import eu.cise.adaptor.core.common.logging.AdaptorLogger;
import eu.cise.adaptor.core.common.logging.LogConfig;
import eu.cise.adaptor.core.servicehandler.domain.context.MessageDataContext;
import eu.cise.adaptor.core.servicehandler.port.in.ReceiveFromLegacySystemPort;
import eu.cise.adaptor.core.servicehandler.service.ServiceHandlerMessageBuilderService;
import eu.cise.servicemodel.v1.service.Service;


public class ReceiveFromLegacySystemAdapter extends ReceiveFromLegacySystemPort {

    private static final AdaptorLogger logger = LogConfig.configureLogging(ReceiveFromLegacySystemAdapter.class);

    public ReceiveFromLegacySystemAdapter(ServiceHandlerMessageBuilderService serviceHandlerMessageBuilderService, Service serviceInformation) {
        super(serviceHandlerMessageBuilderService, serviceInformation);
    }

    /**
     * In the subscribe consumer plugin, the implementation of the handleCustomTranslation should not be called because the
     * {@link ReceiveFromLegacySystemPort#handleIncomingLegacyData(String, MessageDataContext.MessageDataContextManager)}
     * should be called without any LegacySystemPayload.
     *
     * @param legacySystemPayload
     * @param messageDataContextManager
     * @return
     * @throws CiseAdaptorValidationException
     */
    @Override
    public MessageDataContext handleCustomTranslation(String legacySystemPayload, MessageDataContext.MessageDataContextManager messageDataContextManager) throws CiseAdaptorValidationException {
        // This method does not need to be implemented by this plugin as the building of the payloadhappens in the PullConsumerPluginReceiverServer class
        // and the PullConsumerPluginReceiverServer class when it calls the handleIncomingLegacyData method has already added the translated CISE Payload in the
        // MessageContextManager
        return messageDataContextManager.buildContext();
    }
}
