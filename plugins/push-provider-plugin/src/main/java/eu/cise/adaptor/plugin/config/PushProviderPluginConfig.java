package eu.cise.adaptor.plugin.config;

import eu.cise.adaptor.plugin.PluginConfig;
import org.aeonbits.owner.Config;

/**
 * Configuration file for the push provider plugin
 */
@SuppressWarnings("unused")

@Config.Sources({"file:${adaptor.pluginsDir}/push-provider-plugin.properties",
        "classpath:config/push-provider-plugin.properties"})
public interface PushProviderPluginConfig extends PluginConfig {


    @Key("csv-input-directory")
    String getCSVInputDirectory();

    @Key("csv-output-directory")
    String getCSVOutputDirectory();

    @Key("csv-error-directory")
    String getCSVErrorDirectory();

    @Config.Key("adaptor-http.port")
    int getHttpPort();

    @Config.Key("adaptor-http.context")
    String getHttpContext();


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
