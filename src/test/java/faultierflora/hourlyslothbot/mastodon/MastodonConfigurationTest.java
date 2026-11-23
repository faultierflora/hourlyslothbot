package faultierflora.hourlyslothbot.mastodon;

import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import social.bigbone.MastodonClient;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit test for the {@link MastodonConfiguration MastodonConfiguration}.
 * The {@link MastodonClient.Builder MastodonClient.Builder} is mocked, so no connection to a mastodon instance is made.
 */
class MastodonConfigurationTest {

    private final MastodonClient client = mock(MastodonClient.class);

    private MockedConstruction<MastodonClient.Builder> mockBuilder() {
        return mockConstruction(MastodonClient.Builder.class,
                withSettings().defaultAnswer(RETURNS_SELF),
                (builder, context) -> when(builder.build()).thenReturn(client));
    }

    @Test
    void shouldConfigureClientWithHttps() {
        try (MockedConstruction<MastodonClient.Builder> builders = mockBuilder()) {
            MastodonClient result = new MastodonConfiguration().mastodonClient(
                    "mastodon.example.com", true, 443, "supersecrettoken", 240, 120, 60);

            assertThat(result).isSameAs(client);
            assertThat(builders.constructed()).hasSize(1);
            MastodonClient.Builder builder = builders.constructed().getFirst();
            verify(builder).accessToken("supersecrettoken");
            verify(builder).setReadTimeoutSeconds(240L);
            verify(builder).setWriteTimeoutSeconds(120L);
            verify(builder).setConnectTimeoutSeconds(60L);
            verify(builder).withPort(443);
            verify(builder, never()).withHttpsDisabled();
            verify(builder).build();
        }
    }

    @Test
    void shouldDisableHttpsIfConfigured() {
        try (MockedConstruction<MastodonClient.Builder> builders = mockBuilder()) {
            MastodonClient result = new MastodonConfiguration().mastodonClient(
                    "localhost", false, 8080, "supersecrettoken", 10, 10, 10);

            assertThat(result).isSameAs(client);
            MastodonClient.Builder builder = builders.constructed().getFirst();
            verify(builder).withPort(8080);
            verify(builder).withHttpsDisabled();
            verify(builder).build();
        }
    }

    @Test
    void shouldCreateBuilderForConfiguredInstance() {
        List<Object> constructorArguments = new ArrayList<>();
        try (MockedConstruction<MastodonClient.Builder> builders = mockConstruction(MastodonClient.Builder.class,
                withSettings().defaultAnswer(RETURNS_SELF),
                (builder, context) -> {
                    constructorArguments.addAll(context.arguments());
                    when(builder.build()).thenReturn(client);
                })) {
            new MastodonConfiguration().mastodonClient(
                    "mastodon.example.com", true, 443, "supersecrettoken", 240, 240, 240);

            assertThat(builders.constructed()).hasSize(1);
            assertThat(constructorArguments).containsExactly("mastodon.example.com");
        }
    }
}
