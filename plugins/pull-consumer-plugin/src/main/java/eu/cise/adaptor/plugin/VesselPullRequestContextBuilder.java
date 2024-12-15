package eu.cise.adaptor.plugin;

import eu.cise.adaptor.core.common.exceptions.CiseAdaptorRuntimeException;
import eu.cise.adaptor.core.common.message.MessageTypeEnum;
import eu.cise.adaptor.core.servicehandler.domain.context.MessageDataContext;
import eu.cise.adaptor.plugin.config.DiscoveryProfileConfigurationReader;
import eu.cise.adaptor.plugin.config.PullConsumerPluginConfig;
import eu.cise.datamodel.v1.entity.vessel.Vessel;
import eu.cise.servicemodel.v1.message.ConditionOperatorType;
import eu.cise.servicemodel.v1.message.InformationSecurityLevelType;
import eu.cise.servicemodel.v1.message.InformationSensitivityType;
import eu.cise.servicemodel.v1.message.PayloadSelector;
import eu.cise.servicemodel.v1.message.PriorityType;
import eu.cise.servicemodel.v1.message.PurposeType;
import eu.cise.servicemodel.v1.message.RetryStrategyType;
import eu.cise.servicemodel.v1.message.SelectorCondition;
import eu.cise.servicemodel.v1.message.XmlEntityPayload;
import eu.cise.servicemodel.v1.service.QueryByExampleType;
import eu.cise.servicemodel.v1.service.Service;
import eu.cise.servicemodel.v1.service.ServiceCapability;
import eu.cise.servicemodel.v1.service.ServiceOperationType;
import eu.cise.servicemodel.v1.service.ServiceProfile;
import eu.cise.servicemodel.v1.service.ServiceRoleType;
import eu.cise.servicemodel.v1.service.ServiceType;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * Builder class for creating a MessageDataContextManager specifically for vessel pull requests.
 * It supports both known and unknown request patterns, allowing customization of response timeouts,
 * service capabilities, and vessel information.
 */
public class VesselPullRequestContextBuilder {

    public static final int DEFAULT_RESPONSE_TIME_OUT = 1000;
    private static final List<ServiceProfile> configDiscoveryProfiles;

    static {
        DiscoveryProfileConfigurationReader discoveryProfileConfigurationReader = new DiscoveryProfileConfigurationReader();
        configDiscoveryProfiles = discoveryProfileConfigurationReader.getConfiguredProfiles(ServiceRoleType.PROVIDER);
    }

    private final PullConsumerPluginConfig config;
    private final MessageTypeEnum requestType;
    private final List<Service> recipientList;
    private List<ServiceProfile> requestDiscoveryProfiles;
    private Integer responseTimeOut;
    private ServiceCapability requests;
    private XmlEntityPayload payload;
    private PayloadSelector payloadSelector;

    private VesselPullRequestContextBuilder(PullConsumerPluginConfig config, MessageTypeEnum requestType, List<String> recipientIdList, List<Integer> requestDiscoveryProfilesIndexes) {
        this.config = config;
        this.requestType = requestType;
        this.recipientList = new ArrayList<>();
        this.responseTimeOut = null;
        this.requests = null;
        this.payload = basePayload();
        this.payloadSelector = new PayloadSelector();
        if (requestType == MessageTypeEnum.PULL_REQUEST) {
            if (recipientIdList == null || recipientIdList.isEmpty()) {
                throw new IllegalArgumentException("recipientIdList cannot be null or empty for known request");
            }
            recipientIdList.forEach(this::addRecipient);
        } else {
            if (requestDiscoveryProfilesIndexes == null || requestDiscoveryProfilesIndexes.isEmpty()) {
                throw new IllegalArgumentException("requestDiscoveryProfilesIndexes cannot be null or empty for unknown request");
            }
            this.requestDiscoveryProfiles = requestDiscoveryProfilesIndexes
                    .stream()
                    .map(configDiscoveryProfiles::get)
                    .collect(toList());
        }
    }

    /**
     * Creates a builder for known request patterns with the specified configuration and recipients Id list.
     *
     * @param config          the configuration settings for the pull consumer plugin
     * @param recipientIdList the list of recipient IDs for the known request pattern
     * @return a new instance of VesselPullRequestContextBuilder configured for known request patterns
     */
    public static VesselPullRequestContextBuilder forKnownPattern(PullConsumerPluginConfig config, List<String> recipientIdList) {
        return new VesselPullRequestContextBuilder(config, MessageTypeEnum.PULL_REQUEST, recipientIdList, null);
    }

    /**
     * Creates a builder for unknown request patterns with the specified configuration and discovery profile indexes.
     *
     * @param config                          the configuration settings for the pull consumer plugin
     * @param requestDiscoveryProfilesIndexes the list of discovery profile indexes for the unknown request pattern
     * @return a new instance of VesselPullRequestContextBuilder configured for unknown request patterns
     */
    public static VesselPullRequestContextBuilder forUnknownPattern(PullConsumerPluginConfig config, List<Integer> requestDiscoveryProfilesIndexes) {
        return new VesselPullRequestContextBuilder(config, MessageTypeEnum.PULL_UNKNOWN, null, requestDiscoveryProfilesIndexes);
    }

    private XmlEntityPayload basePayload() {
        // create the CISE payload
        XmlEntityPayload resultPayload = new XmlEntityPayload();
        // add the basics
        resultPayload.setInformationSecurityLevel(InformationSecurityLevelType.NON_SPECIFIED);
        resultPayload.setInformationSensitivity(InformationSensitivityType.NON_SPECIFIED);
        resultPayload.setPurpose(PurposeType.NON_SPECIFIED);
        resultPayload.setEnsureEncryption(false);
        return resultPayload;
    }

    public VesselPullRequestContextBuilder withResponseTimeout(Integer timeout) {
        this.responseTimeOut = timeout;
        return this;
    }

    public VesselPullRequestContextBuilder withDefaultResponseTimeout() {
        this.responseTimeOut = DEFAULT_RESPONSE_TIME_OUT;
        return this;
    }

    public VesselPullRequestContextBuilder withServiceCapabilities(Integer responseTime, Integer maxRequests, Integer maxEntities, boolean isBestEffort) {
        this.requests = new ServiceCapability();
        this.requests.setExpectedResponseTime(responseTime);
        this.requests.setMaxNumberOfRequests(maxRequests);
        this.requests.setMaxEntitiesPerMsg(maxEntities);
        this.requests.setQueryByExampleType(isBestEffort ? QueryByExampleType.BEST_EFFORT : QueryByExampleType.EXACT_SEARCH);
        return this;
    }

    public VesselPullRequestContextBuilder withServiceCapabilitiesFromConfig() {
        this.requests = new ServiceCapability();
        this.requests.setExpectedResponseTime(config.getServiceCapResponseTime());
        this.requests.setMaxNumberOfRequests(config.getServiceCapMaxNumberOfRequests());
        this.requests.setMaxEntitiesPerMsg(config.getServiceCapMaxEntitiesPerMsg());
        this.requests.setQueryByExampleType(config.getServiceCapIsBestEffort() ? QueryByExampleType.BEST_EFFORT : QueryByExampleType.EXACT_SEARCH);
        return this;
    }

    public VesselPullRequestContextBuilder withVesselByIMONumber(String imoNumber, boolean isEqual) {
        if (!CollectionUtils.isEmpty(this.payloadSelector.getSelectors())) {
            throw new CiseAdaptorRuntimeException("Only one selector is allowed");
        }
        // Create a Vessel Entity and add it to the payload
        Vessel vessel = new Vessel();
        vessel.setIMONumber(Long.valueOf(imoNumber));
        payload.getAnies().add(vessel);
        // Create Selector Condition and add it to the payload selector
        String selector = "//Vessel[1]/IMONumber";
        SelectorCondition sc = new SelectorCondition();
        sc.setSelector(selector);
        sc.setOperator(isEqual ? ConditionOperatorType.EQUAL : ConditionOperatorType.NOT_EQUAL);
        payloadSelector.getSelectors().add(sc);
        return this;
    }

    public MessageDataContext.MessageDataContextManager buildContextManager() {
        MessageDataContext.MessageDataContextManager msgCtx = MessageDataContext.getManager();
        msgCtx.initializeMessageBehaviour(PriorityType.HIGH, true, RetryStrategyType.NO_RETRY);
        Integer currenResponseTimeOut = responseTimeOut != null ? responseTimeOut : msgCtx.getPullRequestResponseTimeOut();
        if (requestType == MessageTypeEnum.PULL_REQUEST) {
            msgCtx.initializePullRequest(recipientList, currenResponseTimeOut, requests);
        } else {
            msgCtx.initializePullRequestUnknown(requestDiscoveryProfiles, currenResponseTimeOut, requests);
        }
        return finalizeRequest(msgCtx);
    }

    private MessageDataContext.MessageDataContextManager finalizeRequest(MessageDataContext.MessageDataContextManager msgCtx) {
        if (CollectionUtils.isEmpty(this.payload.getAnies())) {
            this.payload.getAnies().add(new Vessel());
        }
        msgCtx.cisePayload(this.payload);
        if (CollectionUtils.isEmpty(this.payloadSelector.getSelectors())) {
            this.payloadSelector = null;
        }
        msgCtx.pullRequestPayloadSelector(this.payloadSelector);
        return msgCtx;
    }

    private void addRecipient(String decodedServiceId) {
        Service recipientService = new Service();
        recipientService.setServiceID(decodedServiceId);
        recipientService.setServiceOperation(ServiceOperationType.PULL);
        recipientService.setServiceRole(ServiceRoleType.PROVIDER);
        recipientService.setServiceType(ServiceType.VESSEL_SERVICE);

        recipientList.add(recipientService);
    }

}
