package com.boaglio.boteco.das.ias.news;

import com.boaglio.boteco.das.ias.model.News;

/**
 * One source of popularity signal for a news item (e.g. Hacker News, Reddit).
 * All sources are summed by {@link CompositePopularityScorer}. A score of 0
 * means "this source has no signal for the item".
 */
public interface PopularitySource {

    /** A non-negative popularity score from this source; 0 when unknown. */
    int score(News news);

    /** Short name for logging (e.g. "HN", "Reddit"). */
    String name();
}
