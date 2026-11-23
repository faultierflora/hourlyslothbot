package faultierflora.hourlyslothbot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import social.bigbone.MastodonClient;

/**
 * Smoke test for the {@link SlothBotApplication SlothBotApplication}.
 * <p>
 * It ensures that the application context can start
 * and that all components needed by Spring are present.
 */
@SpringBootTest
class SlothBotApplicationTest {

    /**
     * MastodonClient needs to be mocked because it directly tests the connection to a nonexistent webservice.
     */
    @SuppressWarnings("unused")
    @MockitoBean
    MastodonClient client;

    @SuppressWarnings("EmptyMethod")
    @Test
    void contextLoads() {
        // This test will fail if the application context cannot start
        // or if Spring finds that components that should be present are missing.
    }
}
