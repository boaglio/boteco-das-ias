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
import java.util.List;

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
        LocalDate releaseDate = LocalDate.now();
        List<News> selected = new ArrayList<>();
        for (Subject subject : Subject.values()) {
            List<String> feeds = properties.feeds().forSubject(subject);
            List<News> candidates = feedReader.readRecent(subject, feeds, properties.newsWindowDays());
            selector.selectBest(subject, candidates).ifPresentOrElse(
                    best -> {
                        log.info("{}: selected \"{}\" ({})", subject, best.title(), best.source());
                        selected.add(best);
                    },
                    () -> log.warn("{}: no news found within the window", subject));
        }
        String title = properties.title().replace("{date}", releaseDate.toString());
        return new Magazine(title, releaseDate, selected);
    }
}
