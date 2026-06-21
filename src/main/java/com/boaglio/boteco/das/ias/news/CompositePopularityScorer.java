package com.boaglio.boteco.das.ias.news;

import com.boaglio.boteco.das.ias.model.News;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The active {@link PopularityScorer}: sums the signal from every
 * {@link PopularitySource} bean (Hacker News, Reddit, …), so the selector ranks
 * by combined cross-platform popularity.
 */
@Component
public class CompositePopularityScorer implements PopularityScorer {

    private final List<PopularitySource> sources;

    public CompositePopularityScorer(List<PopularitySource> sources) {
        this.sources = sources;
    }

    @Override
    public int score(News news) {
        var total = 0;
        for (var source : sources) {
            total += Math.max(0, source.score(news));
        }
        return total;
    }
}
