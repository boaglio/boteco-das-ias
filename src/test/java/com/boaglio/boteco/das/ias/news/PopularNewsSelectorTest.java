package com.boaglio.boteco.das.ias.news;

import com.boaglio.boteco.das.ias.model.News;
import com.boaglio.boteco.das.ias.model.Subject;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PopularNewsSelectorTest {

    private News news(String title, LocalDate published, String summary) {
        return new News(Subject.JAVA, title, "https://x/" + title, "src", published, summary,
                List.of(), null, null, null);
    }

    /** Scores by title, so tests can set popularity deterministically. */
    private NewsSelector selectorWith(Map<String, Integer> scores) {
        return new PopularNewsSelector(n -> scores.getOrDefault(n.title(), 0));
    }

    @Test
    void picksTheMostPopularNotTheNewest() {
        News newer = news("newer", LocalDate.of(2026, 6, 18), "abstract");
        News popular = news("popular", LocalDate.of(2026, 6, 12), "abstract");

        var best = selectorWith(Map.of("newer", 3, "popular", 250))
                .selectBest(Subject.JAVA, List.of(newer, popular));

        assertThat(best).contains(popular);
    }

    @Test
    void fallsBackToMostRecentWhenNoPopularitySignal() {
        News older = news("older", LocalDate.of(2026, 6, 10), "abstract");
        News newer = news("newer", LocalDate.of(2026, 6, 18), "abstract");

        var best = selectorWith(Map.of())  // everything scores 0
                .selectBest(Subject.JAVA, List.of(older, newer));

        assertThat(best).contains(newer);
    }

    @Test
    void ignoresCandidatesWithoutASummary() {
        News bareButPopular = news("bare", LocalDate.of(2026, 6, 18), "");
        News realArticle = news("real", LocalDate.of(2026, 6, 12), "abstract");

        var best = selectorWith(Map.of("bare", 999, "real", 5))
                .selectBest(Subject.JAVA, List.of(bareButPopular, realArticle));

        assertThat(best).contains(realArticle);
    }
}
