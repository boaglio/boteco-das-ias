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
        return new News(Subject.JAVA, title, "https://x/" + title, "src", published, "", List.of(), null);
    }

    @Test
    void picksMostRecentCandidate() {
        News older = news("older", LocalDate.of(2026, 6, 10));
        News newer = news("newer", LocalDate.of(2026, 6, 18));

        Optional<News> best = selector.selectBest(Subject.JAVA, List.of(older, newer));

        assertThat(best).contains(newer);
    }

    @Test
    void returnsEmptyWhenNoCandidates() {
        assertThat(selector.selectBest(Subject.JAVA, List.of())).isEmpty();
    }
}
