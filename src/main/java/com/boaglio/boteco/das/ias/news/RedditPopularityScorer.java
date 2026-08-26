package com.boaglio.boteco.das.ias.news;

import com.boaglio.boteco.das.ias.config.BotecoProperties;
import com.boaglio.boteco.das.ias.model.News;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * Scores popularity from Reddit, summing upvotes + comments across submissions
 * that link to the article URL ({@code /api/info.json?url=…}).
 *
 * <p>Reddit 403s unauthenticated requests to its public JSON endpoints (a
 * platform-wide anti-scraping change, not fixable with request headers alone),
 * so this authenticates as the configured "script" app via the OAuth2
 * client_credentials grant — app-only auth, no Reddit user login involved —
 * and calls {@code oauth.reddit.com} with the resulting bearer token. Without
 * {@link BotecoProperties.Reddit} credentials configured, it scores 0 without
 * making any network call; any other failure (rate limit, no match, network
 * error) also yields 0, so the build still degrades gracefully.
 */
@Component
public class RedditPopularityScorer implements PopularitySource {

    private static final Logger log = LoggerFactory.getLogger(RedditPopularityScorer.class);

    private static final String USER_AGENT = "boteco-das-ias/1.0 (+https://github.com/boaglio/boteco-das-ias)";

    /** Refresh the token this long before its actual expiry, to absorb clock/latency slack. */
    private static final Duration EXPIRY_SLACK = Duration.ofSeconds(60);

    private final BotecoProperties.Reddit config;
    private final RestClient authHttp;
    private final RestClient apiHttp;

    private volatile String accessToken;
    private volatile Instant tokenExpiry = Instant.MIN;

    public RedditPopularityScorer(BotecoProperties properties) {
        this.config = properties.reddit();
        var requestFactory = ClientHttpRequestFactoryBuilder.jdk().build(
                HttpClientSettings.defaults()
                        .withConnectTimeout(Duration.ofSeconds(5))
                        .withReadTimeout(Duration.ofSeconds(8)));
        this.authHttp = RestClient.builder()
                .baseUrl("https://www.reddit.com")
                .defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT)
                .requestFactory(requestFactory)
                .build();
        this.apiHttp = RestClient.builder()
                .baseUrl("https://oauth.reddit.com")
                .defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public String name() {
        return "Reddit";
    }

    @Override
    public int score(News news) {
        if (!config.configured()) {
            return 0;
        }
        var url = news.url();
        if (url == null || url.isBlank()) {
            return 0;
        }
        try {
            var canonical = canonical(url);
            var response = apiHttp.get()
                    .uri(b -> b.path("/api/info.json").queryParam("url", canonical).build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken())
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null) {
                return 0;
            }
            var best = 0;
            for (var child : response.path("data").path("children")) {
                var data = child.path("data");
                best = Math.max(best, data.path("score").asInt(0) + data.path("num_comments").asInt(0));
            }
            return best;
        } catch (Exception e) {
            log.debug("Reddit popularity lookup failed for {}: {}", url, e.getMessage());
            return 0;
        }
    }

    /** Returns a cached, still-valid app-only access token, fetching a fresh one when needed. */
    private synchronized String accessToken() {
        if (accessToken == null || Instant.now().isAfter(tokenExpiry)) {
            var credentials = Base64.getEncoder().encodeToString(
                    (config.clientId() + ":" + config.clientSecret()).getBytes(StandardCharsets.UTF_8));
            var response = authHttp.post()
                    .uri("/api/v1/access_token")
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body("grant_type=client_credentials")
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || !response.has("access_token")) {
                throw new IllegalStateException("Reddit token endpoint returned no access_token");
            }
            accessToken = response.path("access_token").asText();
            var expiresIn = response.path("expires_in").asInt(3600);
            tokenExpiry = Instant.now().plusSeconds(expiresIn).minus(EXPIRY_SLACK);
            log.info("Reddit: obtained app-only access token (expires in {}s)", expiresIn);
        }
        return accessToken;
    }

    /** scheme://host/path — drops query and fragment so submissions match. */
    static String canonical(String url) {
        try {
            var uri = URI.create(url);
            if (uri.getHost() == null) {
                return url;
            }
            var scheme = uri.getScheme() == null ? "https" : uri.getScheme();
            var path = uri.getPath() == null ? "" : uri.getPath();
            return scheme + "://" + uri.getHost() + path;
        } catch (Exception e) {
            return url;
        }
    }
}
