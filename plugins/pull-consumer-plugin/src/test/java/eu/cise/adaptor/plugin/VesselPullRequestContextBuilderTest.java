package eu.cise.adaptor.plugin;

import eu.cise.adaptor.core.common.exceptions.CiseAdaptorValidationException;
import eu.cise.adaptor.core.servicehandler.domain.context.MessageDataContext;
import eu.cise.adaptor.core.servicehandler.domain.message.pattern.PullRequestHelper;
import eu.cise.adaptor.plugin.config.PullConsumerPluginConfig;
import eu.cise.servicemodel.v1.authority.CommunityType;
import eu.cise.servicemodel.v1.authority.CountryType;
import eu.cise.servicemodel.v1.authority.FunctionType;
import eu.cise.servicemodel.v1.authority.SeaBasinType;
import eu.cise.servicemodel.v1.message.ConditionOperatorType;
import eu.cise.servicemodel.v1.message.Message;
import eu.cise.servicemodel.v1.message.PullRequest;
import eu.cise.servicemodel.v1.message.SelectorCondition;
import eu.cise.servicemodel.v1.service.ServiceOperationType;
import eu.cise.servicemodel.v1.service.ServiceProfile;
import eu.cise.servicemodel.v1.service.ServiceRoleType;
import eu.cise.servicemodel.v1.service.ServiceType;
import eu.eucise.helpers.ServiceBuilder;
import eu.eucise.xml.DefaultXmlMapper;
import eu.eucise.xml.XmlMapper;
import org.aeonbits.owner.ConfigFactory;
import org.apache.commons.collections.CollectionUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VesselPullRequestContextBuilderTest {
    static PullConsumerPluginConfig config;

    @BeforeAll
    static void setup() {
        config = ConfigFactory.create(PullConsumerPluginConfig.class);
    }

    @Test
    void it_create_pull_request_known_calling_each_method() throws CiseAdaptorValidationException {
        String serviceIdParam = "somenode.pull.vessel.provider";
        String imoNumber = "1234567";
        boolean isEqual = true;
        Integer responseTime = 32;
        Integer maxNumberOfRequests = 1;
        Integer maxEntitiesPerMsg = 100;
        boolean isBestEffort = true;

        VesselPullRequestContextBuilder builder = VesselPullRequestContextBuilder
                .forKnownPattern(config, List.of(serviceIdParam))
                .withDefaultResponseTimeout()
                .withServiceCapabilities(responseTime, maxNumberOfRequests, maxEntitiesPerMsg, isBestEffort)
                .withVesselByIMONumber(imoNumber, isEqual);

        // Create the MessageDataContextManager for the PullRequest
        MessageDataContext.MessageDataContextManager messageDataContextManager = builder.buildContextManager();

        // The Current Service is added by the Generic Adaptor
        messageDataContextManager.currentService(ServiceBuilder.newService()
                .serviceRole(ServiceRoleType.CONSUMER)
                .operation(ServiceOperationType.PULL)
                .type(ServiceType.VESSEL_SERVICE)
                .id("pull.consumer.service.id")
                .build());

        // From now on, this part is done by the Generic Adaptor
        assertDoesNotThrow(messageDataContextManager::buildContext);

        MessageDataContext context = messageDataContextManager.buildContext();

        List<Message> pullRequestList = PullRequestHelper.generateListOfPullRequestMessage(context);
        assertTrue(CollectionUtils.isNotEmpty(pullRequestList));
        PullRequest pullRequest = (PullRequest) pullRequestList.get(0);

        // Visually Check if the Message contains payload and payload selectors
        XmlMapper xmlMapper = new DefaultXmlMapper.PrettyNotValidating();
        System.out.println(xmlMapper.toXML(pullRequest));
    }

    @Test
    void it_create_pull_request_known_calling_one_method_with_more_recipients() throws CiseAdaptorValidationException {
        String serviceIdParam = "somenode1.pull.vessel.provider,somenode2.pull.vessel.provider,somenode3.pull.vessel.provider";
        String imoNumber = "1234567";
        boolean isEqual = false;

        List<String> recipientIdList = Arrays.asList(serviceIdParam.split(","));
        VesselPullRequestContextBuilder builder = VesselPullRequestContextBuilder
                .forKnownPattern(config, recipientIdList)
                .withDefaultResponseTimeout()
                .withServiceCapabilitiesFromConfig()
                .withVesselByIMONumber(imoNumber, isEqual);

        MessageDataContext.MessageDataContextManager messageDataContextManager = builder.buildContextManager();

        // The Current Service is added by the Generic Adaptor
        messageDataContextManager.currentService(ServiceBuilder.newService()
                .serviceRole(ServiceRoleType.CONSUMER)
                .operation(ServiceOperationType.PULL)
                .type(ServiceType.VESSEL_SERVICE)
                .id("pull.consumer.service.id")
                .build());

        // From now on this part is done by the Generic Adaptor
        assertDoesNotThrow(messageDataContextManager::buildContext);

        MessageDataContext context = messageDataContextManager.buildContext();

        List<Message> pullRequestList = PullRequestHelper.generateListOfPullRequestMessage(context);
        assertEquals(3, pullRequestList.size());

        assertEquals("somenode1.pull.vessel.provider", pullRequestList.get(0).getRecipient().getServiceID());
        assertEquals("somenode2.pull.vessel.provider", pullRequestList.get(1).getRecipient().getServiceID());
        assertEquals("somenode3.pull.vessel.provider", pullRequestList.get(2).getRecipient().getServiceID());

        // Visually Check if the Message contains payload and payload selectors
        pullRequestList.forEach(message -> {
            PullRequest pullRequest = (PullRequest) message;
            SelectorCondition selectorCondition = pullRequest.getPayloadSelector().getSelectors().get(0);
            assertTrue(selectorCondition.getSelector().endsWith("IMONumber"));
            assertEquals(ConditionOperatorType.NOT_EQUAL, selectorCondition.getOperator());
        });
    }

    @Test
    void it_create_pull_request_known_without_optional_parameters() throws CiseAdaptorValidationException {

        String serviceIdParam = "somenode.pull.vessel.provider";

        VesselPullRequestContextBuilder builder = VesselPullRequestContextBuilder
                .forKnownPattern(config, List.of(serviceIdParam));

        MessageDataContext.MessageDataContextManager messageDataContextManager = builder.buildContextManager();

        // The Current Service is added by the Generic Adaptor
        messageDataContextManager.currentService(ServiceBuilder.newService()
                .serviceRole(ServiceRoleType.CONSUMER)
                .operation(ServiceOperationType.PULL)
                .type(ServiceType.VESSEL_SERVICE)
                .id("pull.consumer.service.id")
                .build());

        // From now on, this part is done by the Generic Adaptor
        assertDoesNotThrow(messageDataContextManager::buildContext);

        MessageDataContext context = messageDataContextManager.buildContext();

        List<Message> pullRequestList = PullRequestHelper.generateListOfPullRequestMessage(context);
        assertTrue(CollectionUtils.isNotEmpty(pullRequestList));
        PullRequest pullRequest = (PullRequest) pullRequestList.get(0);
        assertNull(pullRequest.getRequests());
        assertNull(pullRequest.getPayloadSelector());


        // Visually Check if the Message contains payload and payload selectors
        XmlMapper xmlMapper = new DefaultXmlMapper.PrettyNotValidating();
        System.out.println(xmlMapper.toXML(pullRequest));
    }

    @Test
    void it_create_pull_request_unknown_calling_each_method() throws CiseAdaptorValidationException {
        String imoNumber = "1234567";
        boolean isEqual = true;
        // Add service capabilities
        Integer responseTime = 32;
        Integer maxNumberOfRequests = 1;
        Integer maxEntitiesPerMsg = 100;
        boolean isBestEffort = true;

        VesselPullRequestContextBuilder builder = VesselPullRequestContextBuilder.forUnknownPattern(config, asList(0, 1, 2))
                .withDefaultResponseTimeout()
                .withServiceCapabilities(responseTime, maxNumberOfRequests, maxEntitiesPerMsg, isBestEffort)
                .withVesselByIMONumber(imoNumber, isEqual);

        // Create the MessageDataContextManager for the PullRequest
        MessageDataContext.MessageDataContextManager messageDataContextManager = builder.buildContextManager();

        // The Current Service is added by the Generic Adaptor
        messageDataContextManager.currentService(ServiceBuilder.newService()
                .serviceRole(ServiceRoleType.CONSUMER)
                .operation(ServiceOperationType.PULL)
                .type(ServiceType.VESSEL_SERVICE)
                .id("pull.consumer.service.id")
                .build());

        // From now on, this part is done by the Generic Adaptor
        assertDoesNotThrow(messageDataContextManager::buildContext);

        MessageDataContext context = messageDataContextManager.buildContext();

        List<Message> pullRequestList = PullRequestHelper.generateListOfPullRequestMessage(context);
        assertTrue(CollectionUtils.isNotEmpty(pullRequestList));
        PullRequest pullRequest = (PullRequest) pullRequestList.get(0);

        verifyDiscoveryProfile(pullRequest);

        // Visually Check if the Message contains payload and payload selectors
        XmlMapper xmlMapper = new DefaultXmlMapper.PrettyNotValidating();
        System.out.println(xmlMapper.toXML(pullRequest));
    }

    @Test
    void it_create_pull_request_unknown_calling_one_method() throws CiseAdaptorValidationException {
        String imoNumber = "1234567";
        boolean isEqual = false;

        VesselPullRequestContextBuilder builder = VesselPullRequestContextBuilder.forUnknownPattern(config, asList(0, 1, 2))
                .withVesselByIMONumber(imoNumber, isEqual);

        MessageDataContext.MessageDataContextManager messageDataContextManager = builder.buildContextManager();

        // The Current Service is added by the Generic Adaptor
        messageDataContextManager.currentService(ServiceBuilder.newService()
                .serviceRole(ServiceRoleType.CONSUMER)
                .operation(ServiceOperationType.PULL)
                .type(ServiceType.VESSEL_SERVICE)
                .id("pull.consumer.service.id")
                .build());

        // From now on, this part is done by the Generic Adaptor
        assertDoesNotThrow(messageDataContextManager::buildContext);

        MessageDataContext context = messageDataContextManager.buildContext();

        List<Message> pullRequestList = PullRequestHelper.generateListOfPullRequestMessage(context);
        assertTrue(CollectionUtils.isNotEmpty(pullRequestList));
        PullRequest pullRequest = (PullRequest) pullRequestList.get(0);

        verifyDiscoveryProfile(pullRequest);

        // Visually Check if the Message contains payload and payload selectors
        XmlMapper xmlMapper = new DefaultXmlMapper.PrettyNotValidating();
        System.out.println(xmlMapper.toXML(pullRequest));
    }

    @Test
    void it_create_pull_request_unknown_without_optional_parameters() throws CiseAdaptorValidationException {
        VesselPullRequestContextBuilder builder = VesselPullRequestContextBuilder.forUnknownPattern(config, asList(0, 1, 2));

        // Create the MessageDataContextManager for the PullRequest
        MessageDataContext.MessageDataContextManager messageDataContextManager = builder.buildContextManager();

        // The Current Service is added by the Generic Adaptor
        messageDataContextManager.currentService(ServiceBuilder.newService()
                .serviceRole(ServiceRoleType.CONSUMER)
                .operation(ServiceOperationType.PULL)
                .type(ServiceType.VESSEL_SERVICE)
                .id("pull.consumer.service.id")
                .build());

        // From now on, this part is done by the Generic Adaptor
        assertDoesNotThrow(messageDataContextManager::buildContext);

        MessageDataContext context = messageDataContextManager.buildContext();

        List<Message> pullRequestList = PullRequestHelper.generateListOfPullRequestMessage(context);
        assertTrue(CollectionUtils.isNotEmpty(pullRequestList));
        PullRequest pullRequest = (PullRequest) pullRequestList.get(0);
        assertNull(pullRequest.getRequests());
        assertNull(pullRequest.getPayloadSelector());

        verifyDiscoveryProfile(pullRequest);

        // Visually Check if the Message contains payload and payload selectors
        XmlMapper xmlMapper = new DefaultXmlMapper.PrettyNotValidating();
        System.out.println(xmlMapper.toXML(pullRequest));
    }

    @Test
    void it_create_pull_request_unknown_with_one_discovery_profile() throws CiseAdaptorValidationException {

        // Arrange
        VesselPullRequestContextBuilder builder = VesselPullRequestContextBuilder.forUnknownPattern(config, List.of(1));

        MessageDataContext.MessageDataContextManager messageDataContextManager = builder.buildContextManager();
        messageDataContextManager.currentService(ServiceBuilder.newService()
                .serviceRole(ServiceRoleType.CONSUMER)
                .operation(ServiceOperationType.PULL)
                .type(ServiceType.VESSEL_SERVICE)
                .id("pull.consumer.service.id")
                .build());

        // Act
        MessageDataContext context = messageDataContextManager.buildContext();

        // Assert
        List<Message> pullRequestList = PullRequestHelper.generateListOfPullRequestMessage(context);
        assertTrue(CollectionUtils.isNotEmpty(pullRequestList));
        PullRequest pullRequest = (PullRequest) pullRequestList.get(0);
        assertNull(pullRequest.getRequests());
        assertNull(pullRequest.getPayloadSelector());

        assertEquals(1, pullRequest.getDiscoveryProfiles().size());
        ServiceProfile serviceProfileIndexZero = pullRequest.getDiscoveryProfiles().get(0);
        assertEquals(CountryType.IT, serviceProfileIndexZero.getCountry());

        // Visually Check if the Message contains payload and payload selectors
        XmlMapper xmlMapper = new DefaultXmlMapper.PrettyNotValidating();
        System.out.println(xmlMapper.toXML(pullRequest));
    }

    void verifyDiscoveryProfile(PullRequest pullRequest) {
        assertEquals(3, pullRequest.getDiscoveryProfiles().size());
        ServiceProfile serviceProfileIndexZero = pullRequest.getDiscoveryProfiles().get(0);
        assertEquals(ServiceRoleType.PROVIDER, serviceProfileIndexZero.getServiceRole());
        assertEquals(ServiceOperationType.PULL, serviceProfileIndexZero.getServiceOperation());
        assertEquals(ServiceType.VESSEL_SERVICE, serviceProfileIndexZero.getServiceType());
        assertEquals(SeaBasinType.ARCTIC_OCEAN, serviceProfileIndexZero.getSeaBasin());
        assertEquals(FunctionType.CUSTOMS_MONITORING, serviceProfileIndexZero.getFunction());
        assertEquals(CommunityType.CUSTOMS, serviceProfileIndexZero.getCommunity());
        assertEquals(CountryType.ES, serviceProfileIndexZero.getCountry());
    }
}