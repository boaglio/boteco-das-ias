package com.boaglio.boteco.das.ias.news;

import com.boaglio.boteco.das.ias.model.News;
import com.boaglio.boteco.das.ias.model.Subject;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MostRecentNewsSelectorTest {

    private final MostRecentNewsSelector selector = new MostRecentNewsSelector();

    private News news(String title, LocalDate published) {
        return news(title, published, "");
    }

    private News news(String title, LocalDate published, String summary) {
        return new News(Subject.JAVA, title, "https://x/" + title, "src", published, summary,
                List.of(), null, null, null);
    }

    @Test
    void picksMostRecentCandidate() {
        News older = news("older", LocalDate.of(2026, 6, 10));
        News newer = news("newer", LocalDate.of(2026, 6, 18));

        Optional<News> best = selector.selectBest(Subject.JAVA, List.of(older, newer));

        assertThat(best).contains(newer);
    }

    @Test
    void prefersACandidateWithASummaryOverANewerBareLink() {
        News newerNoSummary = news("newer", LocalDate.of(2026, 6, 18), "");
        News olderWithSummary = news("older", LocalDate.of(2026, 6, 12), "a real abstract");

        Optional<News> best = selector.selectBest(Subject.JAVA, List.of(newerNoSummary, olderWithSummary));

        assertThat(best).contains(olderWithSummary);
    }

    @Test
    void fallsBackToMostRecentWhenNoneHaveASummary() {
        News older = news("older", LocalDate.of(2026, 6, 10));
        News newer = news("newer", LocalDate.of(2026, 6, 18));

        assertThat(selector.selectBest(Subject.JAVA, List.of(older, newer))).contains(newer);
    }

    @Test
    void returnsEmptyWhenNoCandidates() {
        assertThat(selector.selectBest(Subject.JAVA, List.of())).isEmpty();
    }
}
