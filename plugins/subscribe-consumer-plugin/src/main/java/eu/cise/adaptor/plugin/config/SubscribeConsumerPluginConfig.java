package eu.cise.adaptor.plugin.config;

import eu.cise.adaptor.plugin.PluginConfig;
import org.aeonbits.owner.Config;

@Config.Sources({"file:${adaptor.pluginsDir}/subscribe-consumer-plugin.properties",
        "classpath:config/subscribe-consumer-plugin.properties"})
public interface SubscribeConsumerPluginConfig extends PluginConfig {

    @Config.Key("adaptor-http.port")
    int getHttpPort();

    @Config.Key("adaptor-http.known-context")
    String getKnownContext();

    @Config.Key("adaptor-http.unknown-context")
    String getUnknownContext();

    @Config.Key("recipient-service-type")
    String getRecipientServiceType();

    @Config.Key("output-directory")
    String getOutputDirectory();

    /**
     * Specifies the Nth service profile parameter
     */

    /**
     * Specifies the Nth service profile parameter
     *
     * @return the service profile community
     */
    @Key("profile.${profile.number}.community")
    String getProfileCommunity();

    /**
     * Specifies the Nth service profile parameter
     *
     * @return the service profile country
     */
    @Key("profile.${profile.number}.country")
    String getProfileCountry();

    /**
     * Specifies the Nth service profile parameter
     *
     * @return the service profile data freshness
     */
    @Key("profile.${profile.number}.data_freshness")
    String getProfileDataFreshness();

    /**
     * Specifies the Nth service profile parameter
     *
     * @return the service profile function
     */
    @Key("profile.${profile.number}.function")
    String getProfileFunction();

    /**
     * Specifies the Nth service profile parameter
     *
     * @return the service profile sea basin
     */
    @Key("profile.${profile.number}.sea_basin")
    String getProfileSeaBasin();

}