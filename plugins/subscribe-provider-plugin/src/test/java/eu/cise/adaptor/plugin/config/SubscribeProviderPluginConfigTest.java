package eu.cise.adaptor.plugin.config;


import org.aeonbits.owner.ConfigFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class SubscribeProviderPluginConfigTest {

    @Test
    public void it_tests_valid_configuration() {
        SubscribeProviderPluginConfig adaptorConfig = ConfigFactory.create(SubscribeProviderPluginConfig.class);
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

}