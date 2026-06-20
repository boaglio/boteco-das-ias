package com.boaglio.boteco.das.ias.news;

import com.boaglio.boteco.das.ias.model.News;
import com.boaglio.boteco.das.ias.model.Subject;

import java.util.List;
import java.util.Optional;

/**
 * Chooses the single "best" news item for a subject out of the candidates
 * gathered from its feeds. Kept as an interface so the heuristic default can
 * later be swapped for an AI-based selector.
 */
public interface NewsSelector {

    Optional<News> selectBest(Subject subject, List<News> candidates);
}
