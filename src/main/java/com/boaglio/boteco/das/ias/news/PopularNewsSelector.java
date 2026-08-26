package com.boaglio.boteco.das.ias.news;

import com.boaglio.boteco.das.ias.model.News;
import com.boaglio.boteco.das.ias.model.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Default {@link NewsSelector}: ranks candidates for a subject by <em>popularity</em>
 * using a {@link PopularityScorer} (Hacker News engagement), then hands the top few
 * to a {@link NewsChooser} — by default the console prompt — so the operator picks
 * which one to feature. Only items that carry a real summary are considered. Every
 * candidate gathered for the window is scored (up to {@link #MAX_TO_SCORE}, a safety
 * cap against a runaway feed rather than a normal limit) — a high-volume subject feed
 * must not crowd a genuinely more popular, slightly older item out of ranking just by
 * publishing more recently. Candidates are always offered to the chooser, even when
 * none of them has a popularity signal yet (common for news published within the last
 * day or two) — ties then fall back to most-recent-first, and a blank answer from the
 * operator keeps that top choice.
 */
@Component
public class PopularNewsSelector implements NewsSelector {

    private static final Logger log = LoggerFactory.getLogger(PopularNewsSelector.class);

    /**
     * Safety cap on how many candidates we look up popularity for, most-recent
     * first. Sized well above any observed per-subject weekly volume — it exists
     * only to bound external lookups against a runaway/misbehaving feed, not to
     * routinely trim the pool (that would silently re-introduce a recency bias).
     */
    private static final int MAX_TO_SCORE = 200;

    /** How many top candidates the operator gets to choose among. */
    private static final int TOP_N = 3;

    private final PopularityScorer scorer;
    private final NewsChooser chooser;

    public PopularNewsSelector(PopularityScorer scorer, NewsChooser chooser) {
        this.scorer = scorer;
        this.chooser = chooser;
    }

    @Override
    public Optional<News> selectBest(Subject subject, List<News> candidates) {
        var withSummary = candidates.stream()
                .filter(news -> news.summary() != null && !news.summary().isBlank())
                .toList();
        var pool = withSummary.isEmpty() ? candidates : withSummary;
        if (pool.isEmpty()) {
            return Optional.empty();
        }

        var toScore = pool.stream()
                .sorted(Comparator.comparing(News::publishedDate).reversed())
                .limit(MAX_TO_SCORE)
                .toList();

        var ranked = toScore.stream()
                .map(news -> Map.entry(news, scorer.score(news)))
                .sorted(Comparator.<Map.Entry<News, Integer>>comparingInt(Map.Entry::getValue)
                        .thenComparing(e -> e.getKey().publishedDate())
                        .thenComparing(e -> e.getKey().title(), Comparator.reverseOrder())
                        .reversed())
                .toList();

        var topN = ranked.stream().limit(TOP_N).toList();
        log.info("{}: top {} candidate(s) by popularity: {}", subject, topN.size(), topN.stream()
                .map(e -> "\"" + e.getKey().title() + "\" (" + e.getValue() + ")")
                .toList());
        var chosen = chooser.choose(subject, topN.stream().map(Map.Entry::getKey).toList());
        return Optional.of(chosen);
    }
}
