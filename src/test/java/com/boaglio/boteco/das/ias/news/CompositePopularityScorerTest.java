package com.boaglio.boteco.das.ias.news;

import com.boaglio.boteco.das.ias.model.News;
import com.boaglio.boteco.das.ias.model.Subject;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CompositePopularityScorerTest {

    private record FakeSource(String name, int value) implements PopularitySource {
        @Override
        public int score(News news) {
            return value;
        }
    }

    private News anyNews() {
        return new News(Subject.JAVA, "t", "https://x/t", "src", LocalDate.of(2026, 6, 18), "s",
                List.of(), null, null, null);
    }

    @Test
    void sumsEverySourcesScore() {
        var scorer = new CompositePopularityScorer(List.of(
                new FakeSource("HN", 120), new FakeSource("Reddit", 80)));

        assertThat(scorer.score(anyNews())).isEqualTo(200);
    }

    @Test
    void treatsNegativeOrMissingSourcesAsZero() {
        var scorer = new CompositePopularityScorer(List.of(
                new FakeSource("HN", -5), new FakeSource("Reddit", 0)));

        assertThat(scorer.score(anyNews())).isZero();
    }
}
