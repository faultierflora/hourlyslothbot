package faultierflora.hourlyslothbot.mastodon;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.wiremock.spring.EnableWireMock;
import social.bigbone.MastodonClient;
import social.bigbone.api.entity.Status;

import java.io.File;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static faultierflora.hourlyslothbot.mastodon.MastodonWireMockStubs.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for the {@link StatusRepositoryImpl StatusRepositoryImpl}.
 * <p>
 * Posts a sloth via a real {@link MastodonClient MastodonClient} to a WireMock server.
 * This covers the media upload and the status creation including the JSON (de)serialization of bigbone.
 */
@SpringBootTest(classes = StatusRepositoryImplIT.TestConfiguration.class)
@EnableWireMock
class StatusRepositoryImplIT {

    @Value("${wiremock.server.port}")
    private int port;

    @Test
    void shouldUploadImageAndPostStatus() throws Exception {
        // Arrange
        stubMastodonInstance(port);
        stubFor(post(urlPathEqualTo("/api/v1/media"))
                .willReturn(okJson("{\"id\":\"4711\",\"type\":\"image\",\"url\":\"http://localhost/sloth.jpg\",\"description\":\"A sloth\"}")));
        stubFor(post(urlPathEqualTo("/api/v1/statuses"))
                .willReturn(okJson("{\"id\":\"1337\",\"content\":\"Hello sloth\",\"visibility\":\"public\",\"language\":\"en\"}")));

        MastodonClient client = new MastodonConfiguration().mastodonClient(
                INSTANCE, false, port, "supersecrettoken", 10, 10, 10);
        StatusRepositoryImpl repository = new StatusRepositoryImpl(client);
        ReflectionTestUtils.setField(repository, "greetings", List.of("Hello everyone,"));
        ReflectionTestUtils.setField(repository, "announcement", List.of("Here is a new sloth picture for you."));
        ReflectionTestUtils.setField(repository, "salutation", "Have fun!");
        ReflectionTestUtils.setField(repository, "tags", List.of("sloth", "faultier"));

        Map<String, Object> yaml = Map.of(
                "description", "A sloth",
                "author", "Flora",
                "url", "https://example.com/sloth",
                "license", Map.of("name", "CC0", "url", "https://creativecommons.org/publicdomain/zero/1.0/"));
        File image = new File("src/main/resources/sloths/00001.jpg");

        // Act
        Status status = repository.postStatus(yaml, image);

        // Assert
        assertThat(status.getId()).isEqualTo("1337");
        verify(postRequestedFor(urlPathEqualTo("/api/v1/media")));
        verify(postRequestedFor(urlPathEqualTo("/api/v1/statuses"))
                .withRequestBody(containing("media_ids"))
                .withRequestBody(containing("4711"))
                .withRequestBody(containing("%23sloth")));
    }

    static class TestConfiguration {
    }
}
