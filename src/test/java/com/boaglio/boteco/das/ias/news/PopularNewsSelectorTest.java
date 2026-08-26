package com.boaglio.boteco.das.ias.news;

import com.boaglio.boteco.das.ias.model.News;
import com.boaglio.boteco.das.ias.model.Subject;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PopularNewsSelectorTest {

    /** Always keeps the top-ranked candidate, i.e. the old fully-automatic behavior. */
    private static final NewsChooser AUTO_TOP = (subject, ranked) -> ranked.get(0);

    private News news(String title, LocalDate published, String summary) {
        return new News(Subject.JAVA, title, "https://x/" + title, "src", published, summary,
                List.of(), null, null, null);
    }

    /** Scores by title, so tests can set popularity deterministically. */
    private NewsSelector selectorWith(Map<String, Integer> scores) {
        return selectorWith(scores, AUTO_TOP);
    }

    private NewsSelector selectorWith(Map<String, Integer> scores, NewsChooser chooser) {
        return new PopularNewsSelector(n -> scores.getOrDefault(n.title(), 0), chooser);
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
    void picksTheMostRecentWhenNoPopularitySignal() {
        News older = news("older", LocalDate.of(2026, 6, 10), "abstract");
        News newer = news("newer", LocalDate.of(2026, 6, 18), "abstract");

        var best = selectorWith(Map.of())  // everything scores 0
                .selectBest(Subject.JAVA, List.of(older, newer));

        assertThat(best).contains(newer);
    }

    @Test
    void stillOffersTheChooserWhenNoPopularitySignal() {
        // Fresh news (published in the last day or two) commonly has zero HN/Reddit
        // engagement yet — the operator should still get to pick, not have the
        // choice made for them silently.
        News older = news("older", LocalDate.of(2026, 6, 10), "abstract");
        News newer = news("newer", LocalDate.of(2026, 6, 18), "abstract");

        List<News> offered = new ArrayList<>();
        NewsChooser recording = (subject, ranked) -> {
            offered.addAll(ranked);
            return ranked.get(0);
        };

        selectorWith(Map.of(), recording)  // everything scores 0
                .selectBest(Subject.JAVA, List.of(older, newer));

        assertThat(offered).containsExactly(newer, older);
    }

    @Test
    void ignoresCandidatesWithoutASummary() {
        News bareButPopular = news("bare", LocalDate.of(2026, 6, 18), "");
        News realArticle = news("real", LocalDate.of(2026, 6, 12), "abstract");

        var best = selectorWith(Map.of("bare", 999, "real", 5))
                .selectBest(Subject.JAVA, List.of(bareButPopular, realArticle));

        assertThat(best).contains(realArticle);
    }

    @Test
    void offersUpToTheTopThreeToTheChooserBestFirst() {
        News first = news("first", LocalDate.of(2026, 6, 12), "abstract");
        News second = news("second", LocalDate.of(2026, 6, 13), "abstract");
        News third = news("third", LocalDate.of(2026, 6, 14), "abstract");
        News fourth = news("fourth", LocalDate.of(2026, 6, 15), "abstract");

        List<News> offered = new ArrayList<>();
        NewsChooser recording = (subject, ranked) -> {
            offered.addAll(ranked);
            return ranked.get(0);
        };

        selectorWith(Map.of("first", 400, "second", 300, "third", 200, "fourth", 100), recording)
                .selectBest(Subject.JAVA, List.of(fourth, third, second, first));

        assertThat(offered).containsExactly(first, second, third);
    }

    @Test
    void honorsTheChoosersPickEvenWhenNotTheMostPopular() {
        News popular = news("popular", LocalDate.of(2026, 6, 12), "abstract");
        News runnerUp = news("runner-up", LocalDate.of(2026, 6, 13), "abstract");

        NewsChooser pickSecond = (subject, ranked) -> ranked.get(1);

        var best = selectorWith(Map.of("popular", 100, "runner-up", 50), pickSecond)
                .selectBest(Subject.JAVA, List.of(popular, runnerUp));

        assertThat(best).contains(runnerUp);
    }
}
