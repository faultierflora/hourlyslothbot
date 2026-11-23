package faultierflora.hourlyslothbot.mastodon;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.wiremock.spring.EnableWireMock;
import social.bigbone.MastodonClient;

import static faultierflora.hourlyslothbot.mastodon.MastodonWireMockStubs.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for the {@link MastodonConfiguration MastodonConfiguration}.
 * <p>
 * Builds a real {@link MastodonClient MastodonClient} against a WireMock server.
 * This runs bigbone and its kotlin runtime, so it fails if the kotlin dependencies don't match bigbone.
 */
@SpringBootTest(classes = MastodonConfigurationIT.TestConfiguration.class)
@EnableWireMock
class MastodonConfigurationIT {

    @Value("${wiremock.server.port}")
    private int port;

    @Test
    void shouldReturnMastodonClientForWireMockInstance() {
        // Arrange
        stubMastodonInstance(port);
        MastodonConfiguration mastodonConfiguration = new MastodonConfiguration();

        // Act
        MastodonClient client = mastodonConfiguration.mastodonClient(
                INSTANCE, false, port, "supersecrettoken", 10, 10, 10);

        // Assert
        assertThat(client.getInstanceName()).isEqualTo(INSTANCE);
        assertThat(client.getPort()).isEqualTo(port);
        assertThat(client.getScheme()).isEqualTo("http");
        assertThat(client.getInstanceVersion()).isEqualTo(VERSION);
    }

    static class TestConfiguration {
    }
}
