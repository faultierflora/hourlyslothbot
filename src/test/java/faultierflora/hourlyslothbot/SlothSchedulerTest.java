package faultierflora.hourlyslothbot;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import faultierflora.hourlyslothbot.mastodon.StatusRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.annotation.Scheduled;
import social.bigbone.api.entity.Status;
import social.bigbone.api.exception.BigBoneRequestException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SlothSchedulerTest {

    @TempDir
    Path slothDirectory;

    private final StatusRepository repository = mock(StatusRepository.class);

    private SlothScheduler scheduler() {
        return scheduler(slothDirectory);
    }

    private SlothScheduler scheduler(final Path directory) {
        return new SlothScheduler(repository, new SlothDirectory(directory.toString()));
    }

    private void createSloth(final int number) throws IOException {
        String paddedNumber = String.format("%05d", number);
        Files.writeString(slothDirectory.resolve(paddedNumber + ".jpg"), "image " + number);
        Files.writeString(slothDirectory.resolve(paddedNumber + ".yaml"), """
                description:
                  Sloth number %d
                author:
                  Flora
                url:
                  "https://example.com/sloth/%d"
                license:
                  name: CC0 1.0 Deed
                  url: https://creativecommons.org/publicdomain/zero/1.0/
                """.formatted(number, number));
    }

    @Test
    void postSlothShouldBeScheduledEveryFullHour() throws NoSuchMethodException {
        Scheduled scheduled = SlothScheduler.class.getMethod("postSloth").getAnnotation(Scheduled.class);

        assertThat(scheduled).isNotNull();
        assertThat(scheduled.cron()).isEqualTo("0 0 * * * ?");
    }

    @Test
    @SuppressWarnings("unchecked")
    void postSlothShouldPostImageWithItsYaml() throws Exception {
        createSloth(1);
        Status status = mock(Status.class);
        when(status.getId()).thenReturn("1337");
        when(repository.postStatus(any(), any())).thenReturn(status);

        try (LogCapture log = new LogCapture(SlothScheduler.class)) {
            scheduler().postSloth();

            ArgumentCaptor<Map<String, Object>> yaml = ArgumentCaptor.forClass(Map.class);
            ArgumentCaptor<File> image = ArgumentCaptor.forClass(File.class);
            verify(repository).postStatus(yaml.capture(), image.capture());
            assertThat(image.getValue()).isEqualTo(slothDirectory.resolve("00001.jpg").toFile());
            assertThat(yaml.getValue())
                    .containsEntry("description", "Sloth number 1")
                    .containsEntry("author", "Flora")
                    .containsEntry("url", "https://example.com/sloth/1")
                    .containsEntry("license", Map.of(
                            "name", "CC0 1.0 Deed",
                            "url", "https://creativecommons.org/publicdomain/zero/1.0/"));
            assertThat(log.messages()).containsExactly(
                    "Going to post new sloth",
                    "Posting sloth number 00001",
                    "Sloth successfully postet with id 1337");
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void postSlothShouldOnlyChooseExistingSloths() throws Exception {
        createSloth(1);
        createSloth(2);
        createSloth(3);
        when(repository.postStatus(any(), any())).thenReturn(mock(Status.class));
        SlothScheduler scheduler = scheduler();

        for (int i = 0; i < 50; i++) {
            scheduler.postSloth();
        }

        ArgumentCaptor<File> images = ArgumentCaptor.forClass(File.class);
        verify(repository, times(50)).postStatus(any(Map.class), images.capture());
        Set<String> postedImages = new HashSet<>();
        images.getAllValues().forEach(file -> postedImages.add(file.getName()));
        assertThat(postedImages)
                .isSubsetOf("00001.jpg", "00002.jpg", "00003.jpg")
                .hasSizeGreaterThan(1);
    }

    @Test
    void postSlothShouldLogAndSuppressBigBoneRequestException() throws Exception {
        createSloth(1);
        when(repository.postStatus(any(), any())).thenThrow(new BigBoneRequestException("instance unavailable"));

        try (LogCapture log = new LogCapture(SlothScheduler.class)) {
            scheduler().postSloth();

            assertThat(log.events())
                    .filteredOn(event -> event.getLevel() == Level.ERROR)
                    .singleElement()
                    .satisfies(event -> assertThat(event.getFormattedMessage())
                            .startsWith("An error occurred.")
                            .contains("message: instance unavailable"));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void postSlothShouldHandleGapsInTheNumbering() throws Exception {
        createSloth(1);
        createSloth(3);
        createSloth(10);
        when(repository.postStatus(any(), any())).thenReturn(mock(Status.class));
        SlothScheduler scheduler = scheduler();

        for (int i = 0; i < 60; i++) {
            scheduler.postSloth();
        }

        ArgumentCaptor<Map<String, Object>> yamls = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<File> images = ArgumentCaptor.forClass(File.class);
        verify(repository, times(60)).postStatus(yamls.capture(), images.capture());
        Set<String> postedImages = new HashSet<>();
        images.getAllValues().forEach(file -> postedImages.add(file.getName()));
        // With 60 tries the chance to miss one of three sloths is below 1e-10
        assertThat(postedImages).containsExactlyInAnyOrder("00001.jpg", "00003.jpg", "00010.jpg");
        // Every image is posted with its own description
        for (int i = 0; i < 60; i++) {
            String number = String.valueOf(Integer.parseInt(images.getAllValues().get(i).getName().substring(0, 5)));
            assertThat(yamls.getAllValues().get(i)).containsEntry("description", "Sloth number " + number);
        }
    }

    @Test
    void postSlothShouldLogErrorIfDirectoryDoesNotExist() {
        Path missing = slothDirectory.resolve("missing");

        try (LogCapture log = new LogCapture(SlothScheduler.class)) {
            assertThatNoException().isThrownBy(() -> scheduler(missing).postSloth());

            assertThat(log.events())
                    .filteredOn(event -> event.getLevel() == Level.ERROR)
                    .extracting(ILoggingEvent::getFormattedMessage)
                    .containsExactly("Can't post a sloth: Sloth directory " + missing + " doesn't exist or can't be read");
        }
        verifyNoInteractions(repository);
    }

    @Test
    void postSlothShouldLogErrorIfDirectoryContainsNoSloths() {
        try (LogCapture log = new LogCapture(SlothScheduler.class)) {
            assertThatNoException().isThrownBy(() -> scheduler().postSloth());

            assertThat(log.events())
                    .filteredOn(event -> event.getLevel() == Level.ERROR)
                    .extracting(ILoggingEvent::getFormattedMessage)
                    .containsExactly("Can't post a sloth: No sloths found in " + slothDirectory);
        }
        verifyNoInteractions(repository);
    }

    @Test
    void postSlothShouldThrowIfYamlIsMissing() throws IOException {
        Files.writeString(slothDirectory.resolve("00001.jpg"), "image without yaml");

        assertThatThrownBy(() -> scheduler().postSloth()).isInstanceOf(FileNotFoundException.class);
        verifyNoInteractions(repository);
    }
}
