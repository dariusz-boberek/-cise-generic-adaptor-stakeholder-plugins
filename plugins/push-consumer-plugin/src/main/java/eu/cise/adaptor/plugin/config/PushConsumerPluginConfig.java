package eu.cise.adaptor.plugin.config;

import eu.cise.adaptor.plugin.PluginConfig;
import org.aeonbits.owner.Config;

/**
 * Configuration file for the push-consumer-plugin
 */
@SuppressWarnings("unused")
@Config.Sources({"file:${adaptor.pluginsDir}/push-consumer-plugin.properties",
        "classpath:config/push-consumer-plugin.properties"})
public interface PushConsumerPluginConfig extends PluginConfig {
    @Key("legacy-system.url")
    String getLegacySystemURL();

    @Key("received-message-save-file")
    String getReceivedMessageSaveFile();

}
