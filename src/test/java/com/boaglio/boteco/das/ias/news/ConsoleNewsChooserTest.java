package com.boaglio.boteco.das.ias.news;

import com.boaglio.boteco.das.ias.model.News;
import com.boaglio.boteco.das.ias.model.Subject;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.StringReader;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConsoleNewsChooserTest {

    private News news(String title) {
        return new News(Subject.JAVA, title, "https://x/" + title, "src",
                LocalDate.of(2026, 6, 18), "abstract", List.of(), null, null, null);
    }

    private final List<News> ranked = List.of(news("first"), news("second"), news("third"));

    private ConsoleNewsChooser chooserFor(String typedLine) {
        return new ConsoleNewsChooser(new BufferedReader(new StringReader(typedLine)));
    }

    @Test
    void picksTheCandidateAtTheTypedIndex() {
        var picked = chooserFor("2\n").choose(Subject.JAVA, ranked);

        assertThat(picked).isEqualTo(ranked.get(1));
    }

    @Test
    void defaultsToTheTopCandidateOnBlankInput() {
        var picked = chooserFor("\n").choose(Subject.JAVA, ranked);

        assertThat(picked).isEqualTo(ranked.get(0));
    }

    @Test
    void defaultsToTheTopCandidateOnOutOfRangeInput() {
        var picked = chooserFor("9\n").choose(Subject.JAVA, ranked);

        assertThat(picked).isEqualTo(ranked.get(0));
    }

    @Test
    void defaultsToTheTopCandidateOnGarbageInput() {
        var picked = chooserFor("not-a-number\n").choose(Subject.JAVA, ranked);

        assertThat(picked).isEqualTo(ranked.get(0));
    }

    @Test
    void skipsThePromptWhenThereIsOnlyOneCandidate() {
        // No input queued at all — proves the reader is never touched.
        var picked = chooserFor("").choose(Subject.JAVA, List.of(ranked.get(0)));

        assertThat(picked).isEqualTo(ranked.get(0));
    }
}
