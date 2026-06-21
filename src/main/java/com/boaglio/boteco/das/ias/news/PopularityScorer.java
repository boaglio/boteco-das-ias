package com.boaglio.boteco.das.ias.news;

import com.boaglio.boteco.das.ias.model.News;

/**
 * Scores how popular a news item is. RSS feeds don't carry engagement, so the
 * default implementation looks the article up on Hacker News; a score of 0 means
 * "no popularity signal" (the selector then falls back to recency).
 */
public interface PopularityScorer {

    /** A non-negative popularity score (higher is more popular); 0 when unknown. */
    int score(News news);
}
