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
 * Scores popularity from Hacker News engagement (points + comments) via its free
 * Algolia search API, matched by the article URL. Network failures or articles
 * that were never posted to HN simply score 0, so the build degrades gracefully
 * to recency-based selection.
 */
@Component
public class HackerNewsPopularityScorer implements PopularitySource {

    private static final Logger log = LoggerFactory.getLogger(HackerNewsPopularityScorer.class);

    @Override
    public String name() {
        return "HN";
    }

    private final RestClient http = RestClient.builder()
            .baseUrl("https://hn.algolia.com/api/v1")
            .requestFactory(ClientHttpRequestFactoryBuilder.jdk().build(
                    ClientHttpRequestFactorySettings.defaults()
                            .withConnectTimeout(Duration.ofSeconds(5))
                            .withReadTimeout(Duration.ofSeconds(8))))
            .build();

    @Override
    public int score(News news) {
        var url = news.url();
        if (url == null || url.isBlank()) {
            return 0;
        }
        try {
            var key = normalize(url);
            var response = http.get()
                    .uri(b -> b.path("/search")
                            .queryParam("restrictSearchableAttributes", "url")
                            .queryParam("query", key)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null) {
                return 0;
            }
            var best = 0;
            for (var hit : response.path("hits")) {
                if (normalize(hit.path("url").asText("")).equals(key)) {
                    best = Math.max(best,
                            hit.path("points").asInt(0) + hit.path("num_comments").asInt(0));
                }
            }
            return best;
        } catch (Exception e) {
            log.debug("HN popularity lookup failed for {}: {}", url, e.getMessage());
            return 0;
        }
    }

    /** Host + path, lowercased, without scheme, "www.", query, fragment or trailing slash. */
    static String normalize(String url) {
        try {
            var uri = URI.create(url);
            var host = uri.getHost() == null ? "" : uri.getHost().toLowerCase().replaceFirst("^www\\.", "");
            var path = uri.getPath() == null ? "" : uri.getPath().replaceAll("/+$", "");
            return host + path;
        } catch (Exception e) {
            return url;
        }
    }
}
