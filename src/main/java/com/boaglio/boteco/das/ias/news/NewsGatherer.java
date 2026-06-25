package com.boaglio.boteco.das.ias.news;

import com.boaglio.boteco.das.ias.config.BotecoProperties;
import com.boaglio.boteco.das.ias.model.Magazine;
import com.boaglio.boteco.das.ias.model.News;
import com.boaglio.boteco.das.ias.model.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Stage 1 of the build process: gather last-week news from the official feeds,
 * pick the best item per {@link Subject}, and assemble a {@link Magazine}.
 */
@Service
public class NewsGatherer {

    private static final Logger log = LoggerFactory.getLogger(NewsGatherer.class);

    private final BotecoProperties properties;
    private final FeedReader feedReader;
    private final NewsSelector selector;

    public NewsGatherer(BotecoProperties properties, FeedReader feedReader, NewsSelector selector) {
        this.properties = properties;
        this.feedReader = feedReader;
        this.selector = selector;
    }

    /** Builds a magazine for today's release date with one news item per subject. */
    public Magazine gather() {
        return gather(null);
    }

    /**
     * Builds a magazine for today's release date with one news item per subject.
     * Subjects already present in {@code existing} are kept as-is and not crawled
     * again, so a retry only fills in the subjects that are still missing.
     */
    public Magazine gather(Magazine existing) {
        var releaseDate = LocalDate.now();
        var selected = new ArrayList<News>();
        // Keys of items already chosen, so no article repeats across subjects
        // (some subjects share a feed, e.g. Spring Boot and Spring AI).
        var alreadyChosen = new HashSet<String>();
        for (var subject : Subject.values()) {
            var reused = existingFor(existing, subject);
            if (reused != null) {
                log.info("{}: already gathered \"{}\", skipping", subject, reused.title());
                selected.add(reused);
                alreadyChosen.add(key(reused));
                continue;
            }
            var feeds = properties.feeds().forSubject(subject);
            var candidates = feedReader.readRecent(subject, feeds, properties.newsWindowDays()).stream()
                    .filter(news -> !alreadyChosen.contains(key(news)))
                    .toList();
            selector.selectBest(subject, candidates).ifPresentOrElse(
                    best -> {
                        log.info("{}: selected \"{}\" ({})", subject, best.title(), best.source());
                        selected.add(best);
                        alreadyChosen.add(key(best));
                    },
                    () -> log.warn("{}: no news found within the window", subject));
        }
        var title = properties.title().replace("{date}", releaseDate.toString());
        return new Magazine(title, releaseDate, selected);
    }

    /** The already-gathered item for the subject, or null if none yet. */
    private static News existingFor(Magazine existing, Subject subject) {
        if (existing == null) {
            return null;
        }
        return existing.news().stream()
                .filter(news -> news.subject() == subject)
                .findFirst()
                .orElse(null);
    }

    /** Dedup key for a news item: its URL when present, otherwise its title. */
    private static String key(News news) {
        return news.url() != null && !news.url().isBlank() ? news.url() : news.title();
    }
}
