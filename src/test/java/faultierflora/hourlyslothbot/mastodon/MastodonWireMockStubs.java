package faultierflora.hourlyslothbot.mastodon;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * WireMock stubs for the endpoints a {@link social.bigbone.MastodonClient MastodonClient} calls while it is built.
 */
final class MastodonWireMockStubs {

    static final String INSTANCE = "localhost";
    static final String VERSION = "4.3.3";

    private MastodonWireMockStubs() {
    }

    /**
     * Stubs nodeinfo and the instance endpoint, so a {@link social.bigbone.MastodonClient MastodonClient}
     * accepts the WireMock server as a mastodon instance.
     *
     * @param port The port of the WireMock server.
     */
    static void stubMastodonInstance(final int port) {
        stubFor(get(urlPathEqualTo("/.well-known/nodeinfo"))
                .willReturn(okJson("{\"links\":[{\"rel\":\"http://nodeinfo.diaspora.software/ns/schema/2.0\",\"href\":\"http://" + INSTANCE + ":" + port + "/nodeinfo/2.0\"}]}")));
        stubFor(get(urlPathEqualTo("/nodeinfo/2.0"))
                .willReturn(okJson("{\"version\":\"2.0\",\"software\":{\"name\":\"mastodon\",\"version\":\"" + VERSION + "\"},\"protocols\":[\"activitypub\"],\"services\":{\"outbound\":[],\"inbound\":[]},\"usage\":{\"users\":{\"total\":1,\"activeMonth\":1,\"activeHalfyear\":1},\"localPosts\":1},\"openRegistrations\":false,\"metadata\":{}}")));
        stubFor(get(urlPathEqualTo("/api/v2/instance"))
                .willReturn(okJson("{\"domain\":\"" + INSTANCE + "\",\"title\":\"WireMockMastodon\",\"version\":\"" + VERSION + "\",\"source_url\":\"https://github.com/mastodon/mastodon\",\"description\":\"Just a WireMock version of Mastodon\",\"usage\":{\"users\":{\"active_month\":1}},\"thumbnail\":{\"url\":\"http://" + INSTANCE + "/thumbnail.png\"},\"languages\":[\"en\"],\"configuration\":{\"urls\":{\"streaming\":\"ws://" + INSTANCE + ":" + port + "\"},\"statuses\":{\"max_characters\":500,\"max_media_attachments\":4,\"characters_reserved_per_url\":23}},\"api_versions\":{\"mastodon\":2}}")));
    }
}
