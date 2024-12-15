package eu.cise.adaptor.plugin;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestHelper {


    public static String readResource(String resourcePath) throws IOException, URISyntaxException {
        Path path = Paths.get(TestHelper.class.getClassLoader().getResource(resourcePath).toURI());
        return Files.readString(path);
    }

    public static File resourceToFile(String resourcePath) throws IOException, URISyntaxException {
        Path path = Paths.get(TestHelper.class.getClassLoader().getResource(resourcePath).toURI());
        return path.toFile();
    }


}
