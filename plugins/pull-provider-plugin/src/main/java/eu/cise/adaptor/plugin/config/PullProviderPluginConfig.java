package eu.cise.adaptor.plugin.config;

import eu.cise.adaptor.plugin.PluginConfig;
import org.aeonbits.owner.Config;

/**
 * Configuration file for the pull-provider-plugin
 */
@SuppressWarnings("unused")
@Config.Sources({"file:${adaptor.pluginsDir}/pull-provider-plugin.properties",
        "classpath:config/pull-provider-plugin.properties"})
public interface PullProviderPluginConfig extends PluginConfig {

    @Key("adaptor-http.context")
    String getHttpContext();
    @Key("adaptor-http.port")
    int getHttpPort();

    @Key("legacy-http.port")
    int getLegacySystemPort();

}
