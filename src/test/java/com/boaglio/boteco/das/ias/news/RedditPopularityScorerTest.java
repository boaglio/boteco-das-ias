package com.boaglio.boteco.das.ias.news;

import com.boaglio.boteco.das.ias.config.BotecoProperties;
import com.boaglio.boteco.das.ias.model.News;
import com.boaglio.boteco.das.ias.model.Subject;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RedditPopularityScorerTest {

    private News news(String url) {
        return new News(Subject.JAVA, "title", url, "src",
                LocalDate.of(2026, 6, 18), "summary", List.of(), null, null, null);
    }

    @Test
    void scoresZeroWithoutMakingAnyNetworkCallWhenNoCredentialsAreConfigured() {
        var properties = new BotecoProperties(null, 0, null, null,
                new BotecoProperties.Reddit(null, null), null, "releases", null, null, null);
        var scorer = new RedditPopularityScorer(properties);

        // If this touched the network it would either hang past the test timeout or
        // throw for an unresolvable/blocked host — scoring 0 immediately proves the
        // "configured()" gate short-circuits before any request is built.
        assertThat(scorer.score(news("https://techcrunch.com/some-article"))).isZero();
    }

    @Test
    void scoresZeroForABlankUrlEvenWhenConfigured() {
        var properties = new BotecoProperties(null, 0, null, null,
                new BotecoProperties.Reddit("id", "secret"), null, "releases", null, null, null);
        var scorer = new RedditPopularityScorer(properties);

        assertThat(scorer.score(news(""))).isZero();
        assertThat(scorer.score(news(null))).isZero();
    }

    @Test
    void canonicalDropsQueryAndFragmentButKeepsSchemeHostAndPath() {
        assertThat(RedditPopularityScorer.canonical("https://Example.com/a/b/?utm=x#frag"))
                .isEqualTo("https://Example.com/a/b/");
    }

    @Test
    void canonicalReturnsTheOriginalUrlWhenItHasNoHost() {
        assertThat(RedditPopularityScorer.canonical("not a url")).isEqualTo("not a url");
    }
}
