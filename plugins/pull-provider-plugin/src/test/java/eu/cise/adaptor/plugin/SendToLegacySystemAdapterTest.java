package eu.cise.adaptor.plugin;

import eu.cise.adaptor.core.servicehandler.domain.RegisteredMessage;
import eu.cise.adaptor.core.servicehandler.port.out.UpdateLegacySystemResult;
import eu.cise.adaptor.plugin.config.PullProviderPluginConfig;
import eu.cise.servicemodel.v1.message.Message;
import eu.eucise.xml.DefaultXmlMapper;
import eu.eucise.xml.XmlMapper;
import org.aeonbits.owner.ConfigFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;

import static eu.cise.adaptor.plugin.SendToLegacySystemAdapter.UPDATE_LEGACY_SYSTEM_STATUS_OK;
import static eu.cise.adaptor.plugin.TestHelper.readResource;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SendToLegacySystemAdapterTest {

    private XmlMapper xmlMapper;

    @BeforeEach
    public void setup() throws Exception {
        xmlMapper = new DefaultXmlMapper();
    }

    @Test
    void it_receives_a_PullRequest_with_PayloadSelector_and_builds_an_sql_query_for_LegacySystem() throws IOException, URISyntaxException {

        //given
        Message pullRequestWithPayloadSelectorIMONumber = xmlMapper.fromXML(readResource("cisemessages/PullRequest_PayloadSelector_IMO.xml"));
        PullProviderPluginConfig config = ConfigFactory.create(PullProviderPluginConfig.class);
        assertNotNull(config);
        SendToLegacySystemAdapter legacySystemAdapter = new SendToLegacySystemAdapter(config);

        //when
        UpdateLegacySystemResult updateLegacySystemResult = legacySystemAdapter.updateLegacySystem(RegisteredMessage.ofCISEMessageDateReceived(pullRequestWithPayloadSelectorIMONumber),
                pullRequestWithPayloadSelectorIMONumber.getMessageID(),
                null,
                null);

        String result = updateLegacySystemResult.getResult();

        Assertions.assertThat(result).isEqualTo(UPDATE_LEGACY_SYSTEM_STATUS_OK);
    }
}