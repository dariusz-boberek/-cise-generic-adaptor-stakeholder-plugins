package eu.cise.adaptor.plugin.config;

import org.aeonbits.owner.ConfigFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class PushConsumerPluginConfigTest {

    @Test
    public void it_tests_configuration_plugin_loads_properties_ok() {
        PushConsumerPluginConfig config = ConfigFactory.create(PushConsumerPluginConfig.class);
        assertNotNull(config);
        assertNotNull(config.getLegacySystemURL());
        assertNotNull(config.getServiceId());
        assertNotNull(config.getServiceType());
        assertNotNull(config.getServiceOperation());
        assertNotNull(config.getServiceRole());
    }

}