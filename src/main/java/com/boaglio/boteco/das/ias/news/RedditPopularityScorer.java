package com.boaglio.boteco.das.ias.news;

import com.boaglio.boteco.das.ias.model.News;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;

/**
 * Scores popularity from Reddit, summing upvotes + comments across submissions
 * that link to the article URL ({@code /api/info.json?url=…}). Reddit requires a
 * descriptive User-Agent; rate limits or no-match simply yield 0, so the build
 * degrades gracefully.
 */
@Component
public class RedditPopularityScorer implements PopularitySource {

    private static final Logger log = LoggerFactory.getLogger(RedditPopularityScorer.class);

    private final RestClient http = RestClient.builder()
            .baseUrl("https://www.reddit.com")
            .defaultHeader("User-Agent", "boteco-das-ias/1.0 (+https://github.com/boaglio/boteco-das-ias)")
            .requestFactory(ClientHttpRequestFactoryBuilder.jdk().build(
                    ClientHttpRequestFactorySettings.defaults()
                            .withConnectTimeout(Duration.ofSeconds(5))
                            .withReadTimeout(Duration.ofSeconds(8))))
            .build();

    @Override
    public String name() {
        return "Reddit";
    }

    @Override
    public int score(News news) {
        var url = news.url();
        if (url == null || url.isBlank()) {
            return 0;
        }
        try {
            var canonical = canonical(url);
            var response = http.get()
                    .uri(b -> b.path("/api/info.json").queryParam("url", canonical).build())
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
