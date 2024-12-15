package eu.cise.adaptor.plugin;

import eu.cise.adaptor.core.common.exceptions.CiseAdaptorRuntimeException;
import eu.cise.adaptor.core.common.exceptions.CiseAdaptorValidationException;
import eu.cise.adaptor.core.common.logging.AdaptorLogger;
import eu.cise.adaptor.core.common.logging.LogConfig;
import eu.cise.adaptor.core.servicehandler.domain.context.MessageDataContext;
import eu.cise.adaptor.core.servicehandler.port.in.ReceiveFromLegacySystemPort;
import eu.cise.adaptor.core.servicehandler.service.ServiceHandlerMessageBuilderService;
import eu.cise.servicemodel.v1.service.Service;

import static eu.cise.adaptor.core.common.logging.LoggerMessage.of;
import static eu.cise.adaptor.core.common.logging.MessageRouteValue.LS_TO_CISE;


public class ReceiveFromLegacySystemAdapter extends ReceiveFromLegacySystemPort {

    private final static AdaptorLogger logger = LogConfig.configureLogging(ReceiveFromLegacySystemAdapter.class);

    public ReceiveFromLegacySystemAdapter(ServiceHandlerMessageBuilderService serviceHandlerMessageBuilderService, Service serviceInformation) {
        super(serviceHandlerMessageBuilderService, serviceInformation);
    }

    /**
     * In the subscribe consumer plugin, the implementation of the handleCustomTranslation should not be called because the
     * {@link ReceiveFromLegacySystemPort#handleIncomingLegacyData(MessageDataContext.MessageDataContextManager)}
     * should be called without any LegacySystemPayload.
     * @param legacySystemPayload
     * @param messageDataContextManager
     * @return
     * @throws CiseAdaptorValidationException
     */
    @Override
    public MessageDataContext handleCustomTranslation(String legacySystemPayload, MessageDataContext.MessageDataContextManager messageDataContextManager) throws CiseAdaptorValidationException {
        String errorMsg = "legacySystemPayload should not be present in the subscribe consumer pattern";
        logger.error(of(errorMsg).addRoutingAttribute(LS_TO_CISE));
        throw new CiseAdaptorRuntimeException(errorMsg);
    }


}

