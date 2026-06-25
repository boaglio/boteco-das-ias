package com.boaglio.boteco.das.ias.news;

import com.boaglio.boteco.das.ias.config.BotecoProperties;
import com.boaglio.boteco.das.ias.model.Magazine;
import com.boaglio.boteco.das.ias.model.News;
import com.boaglio.boteco.das.ias.model.Subject;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class NewsGathererTest {

    /** Records which subjects were actually crawled and returns one candidate each. */
    private static class RecordingFeedReader extends FeedReader {
        final Set<Subject> crawled = EnumSet.noneOf(Subject.class);

        @Override
        public List<News> readRecent(Subject subject, List<String> feedUrls, int windowDays) {
            crawled.add(subject);
            return List.of(new News(subject, "fresh " + subject, "https://" + subject,
                    "src", LocalDate.of(2026, 6, 18), "summary", List.of(), null, null, null));
        }
    }

    private static final NewsSelector FIRST = (subject, candidates) ->
            candidates.stream().findFirst();

    private static BotecoProperties properties() {
        var feeds = new BotecoProperties.Feeds(
                List.of("j"), List.of("sb"), List.of("sa"), List.of("t"));
        return new BotecoProperties("Boteco {date}", 7, feeds, null, null, "releases",
                null, null, null);
    }

    @Test
    void reusesAlreadyGatheredSubjectsAndCrawlsOnlyMissingOnes() {
        var reader = new RecordingFeedReader();
        var gatherer = new NewsGatherer(properties(), reader, FIRST);

        // A previous run already gathered JAVA.
        var existingJava = new News(Subject.JAVA, "reused java", "https://reused",
                "inside.java", LocalDate.of(2026, 6, 17), "summary", List.of(), null, null, null);
        var existing = new Magazine("title", LocalDate.now(), List.of(existingJava));

        var result = gatherer.gather(existing);

        // JAVA is reused verbatim and never crawled again.
        assertThat(reader.crawled).doesNotContain(Subject.JAVA);
        assertThat(reader.crawled).contains(
                Subject.SPRING_BOOT, Subject.SPRING_AI, Subject.TECHNOLOGY);
        var java = result.news().stream()
                .filter(n -> n.subject() == Subject.JAVA).findFirst();
        assertThat(java).map(News::title).contains("reused java");
        assertThat(result.news()).hasSize(4);
    }

    @Test
    void crawlsEverythingWhenNothingExistsYet() {
        var reader = new RecordingFeedReader();
        var gatherer = new NewsGatherer(properties(), reader, FIRST);

        var result = gatherer.gather();

        assertThat(reader.crawled).containsExactlyInAnyOrder(Subject.values());
        assertThat(result.news()).hasSize(4);
    }
}
