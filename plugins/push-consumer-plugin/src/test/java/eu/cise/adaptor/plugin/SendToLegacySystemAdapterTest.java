package eu.cise.adaptor.plugin;

import eu.cise.adaptor.core.servicehandler.domain.RegisteredMessage;
import eu.cise.adaptor.core.servicehandler.port.out.UpdateLegacySystemResult;
import eu.cise.adaptor.plugin.config.PushConsumerPluginConfig;
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


class SendToLegacySystemAdapterTest {

    private XmlMapper xmlMapper;

    @BeforeEach
    public void setup() throws Exception {
        xmlMapper = new DefaultXmlMapper();
    }

    @Test
    public void it_receives_a_push_known_message() throws IOException, URISyntaxException {

        //given
        Message pushMessage_withVessels = xmlMapper.fromXML(readResource("cisemessages/PushMessage_withVessels.xml"));
        PushConsumerPluginConfig config = ConfigFactory.create(PushConsumerPluginConfig.class);
        SendToLegacySystemAdapter legacySystemAdapter = new SendToLegacySystemAdapter(config);

        //when
        UpdateLegacySystemResult updateLegacySystemResult = legacySystemAdapter.updateLegacySystem(RegisteredMessage.ofCISEMessageDateReceived(pushMessage_withVessels),
                pushMessage_withVessels.getMessageID(),
                null,
                null);

        //then
        String result = updateLegacySystemResult.getResult();
        Assertions.assertThat(result).isEqualTo(UPDATE_LEGACY_SYSTEM_STATUS_OK);
    }

}