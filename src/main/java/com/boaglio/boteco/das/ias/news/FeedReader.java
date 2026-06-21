package com.boaglio.boteco.das.ias.news;

import com.boaglio.boteco.das.ias.model.News;
import com.boaglio.boteco.das.ias.model.Subject;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Reads official RSS/Atom feeds and turns recent entries into {@link News}
 * candidates. Feed-level failures are logged and skipped so a single broken
 * feed never aborts the whole edition.
 */
@Component
public class FeedReader {

    private static final Logger log = LoggerFactory.getLogger(FeedReader.class);
    private static final int SUMMARY_MAX_CHARS = 500;

    /**
     * Reads all given feed URLs and returns candidate news for the subject,
     * keeping only entries published within the last {@code windowDays} days.
     */
    public List<News> readRecent(Subject subject, List<String> feedUrls, int windowDays) {
        LocalDate cutoff = LocalDate.now().minusDays(windowDays);
        List<News> candidates = new ArrayList<>();
        for (String feedUrl : feedUrls) {
            try {
                candidates.addAll(readFeed(subject, feedUrl, cutoff));
            } catch (Exception e) {
                log.warn("Skipping feed {} for {}: {}", feedUrl, subject, e.getMessage());
            }
        }
        log.info("{}: {} candidate(s) within {} day window", subject, candidates.size(), windowDays);
        return candidates;
    }

    private List<News> readFeed(Subject subject, String feedUrl, LocalDate cutoff) throws Exception {
        List<News> items = new ArrayList<>();
        URI uri = URI.create(feedUrl);
        try (XmlReader xml = new XmlReader(uri.toURL())) {
            SyndFeed feed = new SyndFeedInput().build(xml);
            String source = feed.getTitle() != null ? feed.getTitle() : uri.getHost();
            for (SyndEntry entry : feed.getEntries()) {
                LocalDate published = toLocalDate(entry.getPublishedDate());
                if (published == null || published.isBefore(cutoff)) {
                    continue;
                }
                items.add(new News(
                        subject,
                        entry.getTitle() != null ? entry.getTitle().strip() : "(untitled)",
                        entry.getLink(),
                        source,
                        published,
                        summarize(entry),
                        List.of(),
                        null,
                        null,
                        null
                ));
            }
        }
        return items;
    }

    private LocalDate toLocalDate(Date date) {
        return date == null ? null : date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private String summarize(SyndEntry entry) {
        String raw = entry.getDescription() != null ? entry.getDescription().getValue() : null;
        if (raw == null && !entry.getContents().isEmpty()) {
            raw = entry.getContents().get(0).getValue();
        }
        if (raw == null) {
            return "";
        }
        String text = raw.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").strip();
        return text.length() > SUMMARY_MAX_CHARS ? text.substring(0, SUMMARY_MAX_CHARS).strip() + "…" : text;
    }
}
