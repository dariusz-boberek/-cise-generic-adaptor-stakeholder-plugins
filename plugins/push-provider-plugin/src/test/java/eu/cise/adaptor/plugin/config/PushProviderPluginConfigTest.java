package eu.cise.adaptor.plugin.config;


import eu.cise.servicemodel.v1.authority.CommunityType;
import eu.cise.servicemodel.v1.authority.CountryType;
import eu.cise.servicemodel.v1.authority.FunctionType;
import eu.cise.servicemodel.v1.authority.SeaBasinType;
import eu.cise.servicemodel.v1.service.DataFreshnessType;
import eu.cise.servicemodel.v1.service.ServiceProfile;
import eu.cise.servicemodel.v1.service.ServiceRoleType;
import org.aeonbits.owner.ConfigFactory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PushProviderPluginConfigTest {

    @Test
    public void it_tests_valid_configuration() {
        PushProviderPluginConfig adaptorConfig = ConfigFactory.create(PushProviderPluginConfig.class);
        assertNotNull(adaptorConfig.getServiceId());
        assertNotNull(adaptorConfig.getServiceType());
        assertNotNull(adaptorConfig.getServiceRole());
        assertNotNull(adaptorConfig.getServiceOperation());
        assertNotNull(adaptorConfig.getCSVInputDirectory());
        assertNotNull(adaptorConfig.getCSVOutputDirectory());
        assertNotNull(adaptorConfig.getCSVErrorDirectory());
        assertNotNull(adaptorConfig.getHttpContext());
        assertNotNull(adaptorConfig.getHttpPort());
    }

    @Test
    public void it_tests_multiple_profiles(){
        DiscoveryProfileConfigurationReader discoveryProfileConfigurationReader = new DiscoveryProfileConfigurationReader();
        List<ServiceProfile> configuredProfiles = discoveryProfileConfigurationReader.getConfiguredProfiles(ServiceRoleType.CONSUMER);

        assertEquals(3, configuredProfiles.size());
        // profile 0
        ServiceProfile firstProfile = configuredProfiles.get(0);
        assertEquals(CommunityType.CUSTOMS, firstProfile.getCommunity());
        assertEquals(CountryType.ES, firstProfile.getCountry());
        assertEquals(DataFreshnessType.NEARLY_REAL_TIME, firstProfile.getDataFreshness());
        assertEquals(FunctionType.CUSTOMS_MONITORING, firstProfile.getFunction());
        assertEquals(SeaBasinType.ARCTIC_OCEAN, firstProfile.getSeaBasin());

        // profile 1
        ServiceProfile secondProfile = configuredProfiles.get(1);
        assertEquals(CommunityType.FISHERIES_CONTROL, secondProfile.getCommunity());
        assertEquals(CountryType.IT, secondProfile.getCountry());
        assertEquals(DataFreshnessType.NEARLY_REAL_TIME, secondProfile.getDataFreshness());
        assertEquals(FunctionType.FISHERIES_MONITORING, secondProfile.getFunction());
        assertEquals(SeaBasinType.BALTIC_SEA, secondProfile.getSeaBasin());

        // profile 2
        ServiceProfile thirdProfile = configuredProfiles.get(2);

        assertEquals(CountryType.FR, thirdProfile.getCountry());

    }

}