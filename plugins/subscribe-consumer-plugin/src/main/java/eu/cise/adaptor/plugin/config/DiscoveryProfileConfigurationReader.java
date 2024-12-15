package eu.cise.adaptor.plugin.config;


import eu.cise.servicemodel.v1.authority.CommunityType;
import eu.cise.servicemodel.v1.authority.CountryType;
import eu.cise.servicemodel.v1.authority.FunctionType;
import eu.cise.servicemodel.v1.authority.SeaBasinType;
import eu.cise.servicemodel.v1.service.DataFreshnessType;
import eu.cise.servicemodel.v1.service.ServiceOperationType;
import eu.cise.servicemodel.v1.service.ServiceProfile;
import eu.cise.servicemodel.v1.service.ServiceRoleType;
import eu.cise.servicemodel.v1.service.ServiceType;
import org.aeonbits.owner.ConfigFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonMap;

public class DiscoveryProfileConfigurationReader {


//    profile.0.community=Customs
//    profile.0.country=ES
//    profile.0.data_freshness=NearlyRealTime
//    profile.0.function=CustomsMonitoring
//    profile.0.sea_basin=ArcticOcean

    public List<ServiceProfile> getConfiguredProfiles(ServiceRoleType profileServiceRoleType) {
        List<ServiceProfile> profiles = new ArrayList<>();

        SubscribeConsumerPluginConfig config;

        for (int i = 0; isProfileNumDefined(config = createConfigNum(i)); i++) {
            profiles.add(readProfileFrom(config, profileServiceRoleType));
        }

        return profiles;
    }

    private ServiceProfile readProfileFrom(SubscribeConsumerPluginConfig config, ServiceRoleType profileServiceRoleType) {
        ServiceProfile profile = new ServiceProfile();

        if (config.getProfileCommunity() != null)
            profile.setCommunity(CommunityType.fromValue(config.getProfileCommunity()));

        if (config.getProfileCountry() != null)
            profile.setCountry(CountryType.fromValue(config.getProfileCountry()));

        if (config.getProfileDataFreshness() != null)
            profile.setDataFreshness(DataFreshnessType.fromValue(config.getProfileDataFreshness()));

        if (config.getProfileFunction() != null)
            profile.setFunction(FunctionType.fromValue(config.getProfileFunction()));

        if (config.getProfileSeaBasin() != null)
            profile.setSeaBasin(SeaBasinType.fromValue(config.getProfileSeaBasin()));


        profile.setServiceOperation(ServiceOperationType.fromValue(config.getServiceOperation()));
        profile.setServiceRole(profileServiceRoleType);
        profile.setServiceType(ServiceType.fromValue(config.getServiceType()));

        return profile;
    }

    private boolean isProfileNumDefined(SubscribeConsumerPluginConfig config) {
        return config.getProfileCommunity() != null ||
                config.getProfileCountry() != null ||
                config.getProfileDataFreshness() != null ||
                config.getProfileFunction() != null ||
                config.getProfileSeaBasin() != null;
    }

    private SubscribeConsumerPluginConfig createConfigNum(int i) {
        return ConfigFactory.create(SubscribeConsumerPluginConfig.class, profileNumberKey(i));
    }

    private Map<String, String> profileNumberKey(int i) {
        return singletonMap("profile.number", String.valueOf(i));
    }
}
