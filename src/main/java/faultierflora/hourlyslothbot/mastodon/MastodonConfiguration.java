package faultierflora.hourlyslothbot.mastodon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import social.bigbone.MastodonClient;

/**
 * Spring configuration for the {@link MastodonClient MastodonClient}.
 * The client is provided as a bean, so it can be replaced by a mock in tests.
 *
 * @author faultierflora
 */
@Configuration
public class MastodonConfiguration {

    /**
     * The {@link org.slf4j.Logger Logger} for this class.
     * The logger is used for logging as configured for the application.
     *
     * @see "src/main/ressources/logback.xml"
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(MastodonConfiguration.class);

    /**
     * Creates the {@link MastodonClient MastodonClient} for the configured mastodon instance.
     * All parameters can be configured in the <code>application.properties</code>.
     *
     * @param instance       The mastodon instance of the bot.
     * @param https          Whether https is used to connect to the instance. Should only be disabled for tests.
     * @param port           The port of the mastodon instance.
     * @param accessToken    The access token of the bot.
     *                       You get an access token on the instance of your bot at the {@link <a href="https://docs.joinmastodon.org/spec/oauth/#token">Token Endpoint</a>} of your bot's instance or in the GUI.
     * @param readTimeout    The read timeout in seconds.
     * @param writeTimeout   The write timeout in seconds.
     * @param connectTimeout The connect timeout in seconds.
     * @return The configured {@link MastodonClient MastodonClient}.
     */
    @Bean
    public MastodonClient mastodonClient(@Value("${mastodon.instance}") final String instance,
                                         @Value("${mastodon.https}") final boolean https,
                                         @Value("${mastodon.port}") final int port,
                                         @Value("${mastodon.accessToken}") final String accessToken,
                                         @Value("${mastodon.readTimeout}") final int readTimeout,
                                         @Value("${mastodon.writeTimeout}") final int writeTimeout,
                                         @Value("${mastodon.connectTimeout}") final int connectTimeout) {
        MastodonClient.Builder builder = new MastodonClient.Builder(instance)
                .accessToken(accessToken)
                .setReadTimeoutSeconds(readTimeout)
                .setWriteTimeoutSeconds(writeTimeout)
                .setConnectTimeoutSeconds(connectTimeout)
                .withPort(port);
        if (!https) {
            LOGGER.warn("Starting Mastodon configuration with https disabled. This is dangerous!");
            builder = builder.withHttpsDisabled();
        }
        MastodonClient client = builder.build();
        LOGGER.info("MastodonClient for mastodon instance " + instance + " created");
        return client;
    }
}
