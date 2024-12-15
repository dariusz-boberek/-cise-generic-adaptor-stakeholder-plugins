package eu.cise.adaptor.plugin;

import eu.cise.adaptor.core.servicehandler.domain.RegisteredMessage;
import eu.cise.adaptor.core.servicehandler.port.out.UpdateLegacySystemResult;
import eu.cise.adaptor.plugin.config.PullConsumerPluginConfig;
import eu.cise.servicemodel.v1.message.Message;
import eu.eucise.xml.DefaultXmlMapper;
import eu.eucise.xml.XmlMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static eu.cise.adaptor.plugin.SendToLegacySystemAdapter.UPDATE_LEGACY_SYSTEM_STATUS_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class SendToLegacySystemAdapterTest {

    private XmlMapper xmlMapper;

    public static String readResource(String resourcePath) throws IOException, URISyntaxException {
        Path path = Paths.get(SendToLegacySystemAdapterTest.class.getClassLoader().getResource(resourcePath).toURI());
        return Files.readString(path);
    }

    public static String readResource(Path filePath) throws IOException {
        return Files.readString(filePath);
    }

    @BeforeEach
    public void setup() {
        xmlMapper = new DefaultXmlMapper();
    }

    @Test
    public void it_receives_a_PullConsumerMessage_sent_from_PullProvider_and_forwards_it_to_legacy_system(@TempDir Path tempDir) throws IOException, URISyntaxException, InterruptedException {
        // Arrange
        Message PullResponseMessage = xmlMapper.fromXML(readResource("cisemessages/PullResponseMessageFromProvider_withVessels.xml"));
        PullConsumerPluginConfig mockConfig = mock(PullConsumerPluginConfig.class);
        when(mockConfig.getOutputDirectory()).thenReturn(tempDir.toString());

        SendToLegacySystemAdapter legacySystemAdapter = new SendToLegacySystemAdapter(mockConfig);

        // Act
        UpdateLegacySystemResult updateLegacySystemResult = legacySystemAdapter.updateLegacySystem(RegisteredMessage.ofCISEMessageDateReceived(PullResponseMessage),
                PullResponseMessage.getMessageID(),
                null, PullResponseMessage.getContextID());

        // Assert
        String result = updateLegacySystemResult.getResult();
        assertThat(result).isEqualTo(UPDATE_LEGACY_SYSTEM_STATUS_OK);

        File outputDir = new File(tempDir.toString());
        String[] files = outputDir.list();
        assertNotNull(files);
        assertTrue(files.length > 0, "File was not created in the temp directory");

        File generatedFile = new File(outputDir, files[0]);
        assertTrue(generatedFile.exists(), "Generated XML file does not exist");

        Message resultPullConsumerMessage = xmlMapper.fromXML(readResource(generatedFile.toPath()));
        assertEquals(PullResponseMessage.getContextID(), resultPullConsumerMessage.getContextID(), "Context IDs do not match");
    }


}