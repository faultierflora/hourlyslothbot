package faultierflora.hourlyslothbot;

import ch.qos.logback.classic.Level;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Integration test for the whole bot.
 * <p>
 * Starts the complete application with the real {@link social.bigbone.MastodonClient MastodonClient},
 * the shipped sloths and a WireMock server as mastodon instance.
 * The stubs are in <code>src/test/resources/wiremock/mastodon/mappings</code>,
 * because the client already talks to the instance while the application context starts.
 */
@SpringBootTest(properties = {
        "mastodon.instance=localhost",
        "mastodon.https=false",
        "mastodon.accessToken=integrationtesttoken",
        "mastodon.readTimeout=10",
        "mastodon.writeTimeout=10",
        "mastodon.connectTimeout=10"
})
@EnableWireMock(@ConfigureWireMock(
        name = "mastodon",
        portProperties = "mastodon.port",
        filesUnderClasspath = "wiremock/mastodon"))
class SlothBotIT {

    private static final Path SLOTH_DIRECTORY = Path.of("src/main/resources/sloths");

    @InjectWireMock("mastodon")
    WireMockServer mastodon;

    @Autowired
    SlothScheduler slothScheduler;

    @Autowired
    List<ScheduledTaskHolder> scheduledTaskHolders;

    @Test
    void botShouldBeScheduledToPostEveryFullHour() {
        List<String> cronExpressions = scheduledTaskHolders.stream()
                .flatMap(holder -> holder.getScheduledTasks().stream())
                .map(ScheduledTask::getTask)
                .filter(CronTask.class::isInstance)
                .map(task -> ((CronTask) task).getExpression())
                .toList();

        assertThat(cronExpressions).containsExactly("0 0 * * * ?");
    }

    @Test
    void botShouldTootARandomSlothWithAttribution() throws IOException {
        slothScheduler.postSloth();

        // The image is uploaded with the description of its yaml
        List<LoggedRequest> uploads = mastodon.findAll(postRequestedFor(urlPathEqualTo("/api/v1/media")));
        assertThat(uploads).hasSize(1);
        LoggedRequest upload = uploads.getFirst();
        assertThat(upload.getHeader("Authorization")).isEqualTo("Bearer integrationtesttoken");
        String uploadBody = upload.getBodyAsString();
        Map<String, Object> sloth = shippedSloths()
                .filter(yaml -> uploadBody.contains((String) yaml.get("description")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Upload does not contain the description of a shipped sloth"));

        // The status references the uploaded image and the attribution of the same sloth
        List<LoggedRequest> statuses = mastodon.findAll(postRequestedFor(urlPathEqualTo("/api/v1/statuses")));
        assertThat(statuses).hasSize(1);
        LoggedRequest status = statuses.getFirst();
        assertThat(status.getHeader("Authorization")).isEqualTo("Bearer integrationtesttoken");
        String statusBody = URLDecoder.decode(status.getBodyAsString(), StandardCharsets.UTF_8);
        @SuppressWarnings("unchecked")
        Map<String, String> license = (Map<String, String>) sloth.get("license");
        assertThat(statusBody)
                .contains("4711")
                .contains("visibility=public")
                .contains("language=en")
                .contains("This picture is from " + sloth.get("author"))
                .contains("can be found here: " + sloth.get("url"))
                .contains("It is licensed under " + license.get("name") + " (" + license.get("url") + ")")
                .contains("by @faultierflora@mastodon.art")
                .contains("#sloth #cutesloth #nature");
    }

    @Test
    void botShouldSurviveAnUnavailableMastodonInstance() {
        mastodon.stubFor(post(urlPathEqualTo("/api/v1/statuses"))
                .atPriority(1)
                .willReturn(aResponse()
                        .withStatus(503)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"Service unavailable\"}")));

        try (LogCapture log = new LogCapture(SlothScheduler.class)) {
            assertThatNoException().isThrownBy(() -> slothScheduler.postSloth());

            assertThat(log.events())
                    .filteredOn(event -> event.getLevel() == Level.ERROR)
                    .singleElement()
                    .satisfies(event -> assertThat(event.getFormattedMessage()).contains("Status code: 503"));
        }
        mastodon.verify(1, postRequestedFor(urlPathEqualTo("/api/v1/media")));
    }

    private static Stream<Map<String, Object>> shippedSloths() throws IOException {
        try (Stream<Path> files = Files.list(SLOTH_DIRECTORY)) {
            return files.filter(file -> file.getFileName().toString().endsWith(".yaml"))
                    .map(SlothBotIT::loadYaml)
                    .toList()
                    .stream();
        }
    }

    private static Map<String, Object> loadYaml(final Path file) {
        try (InputStream in = Files.newInputStream(file)) {
            return new Yaml().load(in);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
