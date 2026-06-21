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
 * Default {@link NewsSelector}: picks the <em>most popular</em> candidate for a
 * subject, using a {@link PopularityScorer} (Hacker News engagement). Only items
 * that carry a real summary are considered, and only the most recent few are
 * scored to bound the external lookups. When nothing has a popularity signal it
 * falls back to the most recent item.
 */
@Component
public class PopularNewsSelector implements NewsSelector {

    private static final Logger log = LoggerFactory.getLogger(PopularNewsSelector.class);

    /** Cap on how many (most-recent) candidates we look up popularity for. */
    private static final int MAX_TO_SCORE = 20;

    private final PopularityScorer scorer;
    private final MostRecentNewsSelector byRecency = new MostRecentNewsSelector();

    public PopularNewsSelector(PopularityScorer scorer) {
        this.scorer = scorer;
    }

    @Override
    public Optional<News> selectBest(Subject subject, List<News> candidates) {
        var withSummary = candidates.stream()
                .filter(news -> news.summary() != null && !news.summary().isBlank())
                .toList();
        var pool = withSummary.isEmpty() ? candidates : withSummary;

        var toScore = pool.stream()
                .sorted(Comparator.comparing(News::publishedDate).reversed())
                .limit(MAX_TO_SCORE)
                .toList();

        var best = toScore.stream()
                .map(news -> Map.entry(news, scorer.score(news)))
                .max(Comparator.<Map.Entry<News, Integer>>comparingInt(Map.Entry::getValue)
                        .thenComparing(e -> e.getKey().publishedDate())
                        .thenComparing(e -> e.getKey().title(), Comparator.reverseOrder()));

        if (best.isPresent() && best.get().getValue() > 0) {
            var pick = best.get();
            log.info("{}: most popular \"{}\" ({} popularity points)",
                    subject, pick.getKey().title(), pick.getValue());
            return Optional.of(pick.getKey());
        }
        // No popularity signal anywhere — fall back to the most recent item.
        return byRecency.selectBest(subject, pool);
    }
}
