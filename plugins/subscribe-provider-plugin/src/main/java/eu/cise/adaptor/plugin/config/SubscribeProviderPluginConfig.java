package eu.cise.adaptor.plugin.config;

import eu.cise.adaptor.plugin.PluginConfig;
import org.aeonbits.owner.Config;

/**
 * Configuration file for the subscribe provider plugin
 */
@SuppressWarnings("unused")

@Config.Sources({"file:${adaptor.pluginsDir}/subscribe-provider-plugin.properties",
        "classpath:config/subscribe-provider-plugin.properties"})
public interface SubscribeProviderPluginConfig extends PluginConfig {


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


}
