package faultierflora.hourlyslothbot;

import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RunAfterStartupTest {

    @TempDir
    Path slothDirectory;

    private RunAfterStartup runAfterStartup(final Path directory) {
        return new RunAfterStartup(new SlothDirectory(directory.toString()));
    }

    @Test
    void shouldLogNumberOfSlothImages() throws IOException {
        Files.createFile(slothDirectory.resolve("00001.jpg"));
        Files.createFile(slothDirectory.resolve("00001.yaml"));
        Files.createFile(slothDirectory.resolve("00002.jpg"));
        Files.createFile(slothDirectory.resolve("00002.yaml"));
        Files.createFile(slothDirectory.resolve("README.md"));

        try (LogCapture log = new LogCapture(RunAfterStartup.class)) {
            runAfterStartup(slothDirectory).runAfterStartup();

            assertThat(log.messages())
                    .containsExactly("Bot started. I have 2 sloths found in " + slothDirectory);
        }
    }

    @Test
    void shouldWarnIfDirectoryContainsNoSloths() {
        try (LogCapture log = new LogCapture(RunAfterStartup.class)) {
            runAfterStartup(slothDirectory).runAfterStartup();

            assertThat(log.events()).singleElement().satisfies(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.WARN);
                assertThat(event.getFormattedMessage())
                        .isEqualTo("Bot started, but I found no sloths in " + slothDirectory);
            });
        }
    }

    @Test
    void shouldFailWithClearMessageIfDirectoryDoesNotExist() {
        Path missing = slothDirectory.resolve("missing");

        assertThatThrownBy(() -> runAfterStartup(missing).runAfterStartup())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Sloth directory " + missing + " doesn't exist or can't be read");
    }
}
