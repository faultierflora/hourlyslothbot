package faultierflora.hourlyslothbot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks the sloth images and descriptions shipped with the bot.
 * The {@link SlothScheduler SlothScheduler} expects a yaml with description, author, url and license
 * for every image.
 */
class SlothResourcesTest {

    private static final Path SLOTH_DIRECTORY = Path.of("src/main/resources/sloths");

    /**
     * Mastodon accepts up to 1500 characters as image description.
     */
    private static final int MAX_DESCRIPTION_LENGTH = 1500;

    static Stream<String> slothNumbers() throws IOException {
        try (Stream<Path> files = Files.list(SLOTH_DIRECTORY)) {
            return files.map(file -> file.getFileName().toString())
                    .filter(name -> name.endsWith(".jpg"))
                    .map(name -> name.substring(0, name.length() - ".jpg".length()))
                    .sorted()
                    .toList()
                    .stream();
        }
    }

    @Test
    void thereShouldBeSloths() throws IOException {
        assertThat(slothNumbers()).isNotEmpty();
    }

    @ParameterizedTest
    @MethodSource("slothNumbers")
    @SuppressWarnings("unchecked")
    void everySlothShouldHaveACompleteYaml(final String number) throws IOException {
        Path yamlFile = SLOTH_DIRECTORY.resolve(number + ".yaml");
        assertThat(yamlFile).exists();

        Map<String, Object> yaml;
        try (InputStream in = Files.newInputStream(yamlFile)) {
            yaml = new Yaml().load(in);
        }

        assertThat(yaml).containsKeys("description", "author", "url", "license");
        assertThat((String) yaml.get("description")).isNotBlank().hasSizeLessThanOrEqualTo(MAX_DESCRIPTION_LENGTH);
        assertThat((String) yaml.get("author")).isNotBlank();
        assertThat((String) yaml.get("url")).startsWith("https://");
        Map<String, String> license = (Map<String, String>) yaml.get("license");
        assertThat(license.get("name")).isNotBlank();
        assertThat(license.get("url")).startsWith("https://");
    }

    @ParameterizedTest
    @MethodSource("slothNumbers")
    void everySlothImageShouldBeAJpeg(final String number) throws IOException {
        byte[] image = Files.readAllBytes(SLOTH_DIRECTORY.resolve(number + ".jpg"));

        assertThat(image).hasSizeGreaterThan(3);
        assertThat(new byte[]{image[0], image[1], image[2]}).containsExactly(0xFF, 0xD8, 0xFF);
    }
}
