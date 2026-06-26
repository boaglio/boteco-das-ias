package com.boaglio.boteco.das.ias.opinion;

import com.boaglio.boteco.das.ias.model.Magazine;
import com.boaglio.boteco.das.ias.model.News;
import com.boaglio.boteco.das.ias.model.Opinion;
import com.boaglio.boteco.das.ias.model.Reviewer;
import com.boaglio.boteco.das.ias.model.Subject;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OpinionCollectorTest {

    private record FakeEngine(Reviewer reviewer, String reply, boolean fail) implements OpinionEngine {
        @Override
        public String opine(News news) throws Exception {
            if (fail) {
                throw new IllegalStateException("engine down");
            }
            return reply;
        }
    }

    private Magazine oneItemMagazine() {
        News news = new News(Subject.JAVA, "JEP news", "https://x", "inside.java",
                LocalDate.of(2026, 6, 18), "summary", List.of(), null, null, null);
        return new Magazine("title", LocalDate.of(2026, 6, 20), List.of(news));
    }

    @Test
    void attachesOpinionsInConversationOrderRegardlessOfBeanOrder() {
        // Supplied out of order on purpose.
        List<OpinionEngine> engines = List.of(
                new FakeEngine(Reviewer.CLAUDE_CLI, "claude take", false),
                new FakeEngine(Reviewer.HUMAN, "my take", false),
                new FakeEngine(Reviewer.OLLAMA_LLAMA, "llama take", false),
                new FakeEngine(Reviewer.OLLAMA_PHI, "phi take", false));

        Magazine result = new OpinionCollector(engines).collect(oneItemMagazine());

        List<Opinion> opinions = result.news().get(0).opinions();
        assertThat(opinions).extracting(Opinion::reviewer).containsExactly(
                Reviewer.HUMAN, Reviewer.OLLAMA_PHI, Reviewer.OLLAMA_LLAMA, Reviewer.CLAUDE_CLI);
    }

    @Test
    void reusesExistingOpinionsAndOnlyRunsMissingReviewers() {
        // The item already has the human's take from a previous run.
        News news = new News(Subject.JAVA, "JEP news", "https://x", "inside.java",
                LocalDate.of(2026, 6, 18), "summary",
                List.of(new Opinion(Reviewer.HUMAN, "my take")), null, null, null);
        Magazine magazine = new Magazine("title", LocalDate.of(2026, 6, 20), List.of(news));

        // The human engine would throw if invoked, proving it is skipped.
        List<OpinionEngine> engines = List.of(
                new FakeEngine(Reviewer.HUMAN, null, true),
                new FakeEngine(Reviewer.OLLAMA_PHI, "phi take", false),
                new FakeEngine(Reviewer.OLLAMA_LLAMA, "llama take", false),
                new FakeEngine(Reviewer.CLAUDE_CLI, "claude take", false));

        Magazine result = new OpinionCollector(engines).collect(magazine);

        List<Opinion> opinions = result.news().get(0).opinions();
        assertThat(opinions).extracting(Opinion::reviewer).containsExactly(
                Reviewer.HUMAN, Reviewer.OLLAMA_PHI, Reviewer.OLLAMA_LLAMA, Reviewer.CLAUDE_CLI);
        assertThat(opinions.get(0).text()).isEqualTo("my take");
    }

    @Test
    void forceReplacesExistingOpinions() {
        News news = new News(Subject.JAVA, "JEP news", "https://x", "inside.java",
                LocalDate.of(2026, 6, 18), "summary",
                List.of(new Opinion(Reviewer.HUMAN, "old take")), null, null, null);
        Magazine magazine = new Magazine("title", LocalDate.of(2026, 6, 20), List.of(news));

        List<OpinionEngine> engines = List.of(
                new FakeEngine(Reviewer.HUMAN, "fresh take", false));

        Magazine result = new OpinionCollector(engines).collect(magazine, true);

        assertThat(result.news().get(0).opinions()).extracting(Opinion::text)
                .containsExactly("fresh take");
    }

    @Test
    void skipsFailingEngineButKeepsTheRest() {
        List<OpinionEngine> engines = List.of(
                new FakeEngine(Reviewer.HUMAN, "my take", false),
                new FakeEngine(Reviewer.OLLAMA_PHI, null, true),
                new FakeEngine(Reviewer.OLLAMA_LLAMA, "llama take", false),
                new FakeEngine(Reviewer.CLAUDE_CLI, "claude take", false));

        Magazine result = new OpinionCollector(engines).collect(oneItemMagazine());

        assertThat(result.news().get(0).opinions()).extracting(Opinion::reviewer)
                .containsExactly(Reviewer.HUMAN, Reviewer.OLLAMA_LLAMA, Reviewer.CLAUDE_CLI);
    }
}
