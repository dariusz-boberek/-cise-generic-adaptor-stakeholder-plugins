package eu.cise.adaptor.plugin.spark;

import eu.cise.servicemodel.v1.message.Acknowledgement;
import eu.cise.servicemodel.v1.message.AcknowledgementType;
import eu.cise.servicemodel.v1.message.PriorityType;
import eu.eucise.helpers.AckBuilder;
import eu.eucise.xml.DefaultXmlMapper;
import eu.eucise.xml.XmlMapper;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.UUID;

import static eu.eucise.helpers.AckBuilder.newAck;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class PullConsumerPluginReceiverServerTest {

    XmlMapper xmlMapper = new DefaultXmlMapper();

    @Test
    public void it_tests_correct_generation_of_Ack() {
        AckBuilder ackBuilder = newAck()
                .id(UUID.randomUUID().toString())
                .creationDateTime(new Date())
                .priority(PriorityType.HIGH)
                .isRequiresAck(false)
                .ackCode(AcknowledgementType.BAD_REQUEST)
                .ackDetail("Ack Detail")
                .xmlPayload();

        Acknowledgement generatedAck = ackBuilder.build();
        generatedAck.setPayload(null);
        assertDoesNotThrow(() -> xmlMapper.toXML(generatedAck));
    }

}