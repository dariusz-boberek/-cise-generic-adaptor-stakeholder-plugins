package eu.cise.adaptor.plugin.config;

import org.aeonbits.owner.ConfigFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PullProviderPluginConfigTest {

    @Test
    public void it_tests_configuration_plugin_loads_properties_ok() {
        PullProviderPluginConfig config = ConfigFactory.create(PullProviderPluginConfig.class);
        assertNotNull(config);
        assertNotNull(config.getLegacySystemPort());
        assertNotNull(config.getHttpContext());
        assertNotNull(config.getHttpPort());
        assertNotNull(config.getServiceId());
        assertNotNull(config.getServiceType());
        assertNotNull(config.getServiceOperation());
        assertNotNull(config.getServiceRole());
    }

}