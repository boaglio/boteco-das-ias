package com.boaglio.boteco.das.ias.news;

import com.boaglio.boteco.das.ias.model.News;
import com.boaglio.boteco.das.ias.model.Subject;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Default {@link NewsSelector}: picks the most recently published candidate,
 * breaking ties by title for determinism.
 */
@Component
public class MostRecentNewsSelector implements NewsSelector {

    @Override
    public Optional<News> selectBest(Subject subject, List<News> candidates) {
        // Prefer items that actually carry a readable summary (skip bare links
        // like a Wikipedia URL from an aggregator); fall back to all if none do.
        var withSummary = candidates.stream()
                .filter(news -> news.summary() != null && !news.summary().isBlank())
                .toList();
        var pool = withSummary.isEmpty() ? candidates : withSummary;
        return pool.stream()
                .max(Comparator.comparing(News::publishedDate)
                        .thenComparing(News::title, Comparator.reverseOrder()));
    }
}
