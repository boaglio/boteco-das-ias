package com.boaglio.boteco.das.ias.opinion;

import com.boaglio.boteco.das.ias.model.News;
import com.boaglio.boteco.das.ias.model.Reviewer;

/**
 * One opinion engine — a reviewer that produces a take on a news item.
 * Implementations may call a local model, the Claude CLI, or prompt the human.
 */
public interface OpinionEngine {

    /** Which reviewer this engine represents. */
    Reviewer reviewer();

    /**
     * Produces this reviewer's opinion text for the news item.
     *
     * @throws Exception if the underlying model/process/input is unavailable
     */
    String opine(News news) throws Exception;
}
