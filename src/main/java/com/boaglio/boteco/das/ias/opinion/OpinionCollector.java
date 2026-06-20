package com.boaglio.boteco.das.ias.opinion;

import com.boaglio.boteco.das.ias.model.Magazine;
import com.boaglio.boteco.das.ias.model.News;
import com.boaglio.boteco.das.ias.model.Opinion;
import com.boaglio.boteco.das.ias.model.Reviewer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Stage 2 of the build process: for each news item, collect opinions from
 * every reviewer in the magazine's conversation order, then attach them.
 * A failing engine (e.g. Ollama down, no human input) is logged and skipped so
 * the rest of the conversation still gets recorded.
 */
@Service
public class OpinionCollector {

    private static final Logger log = LoggerFactory.getLogger(OpinionCollector.class);

    /** Conversation order used by the magazine layout (left/right/left/right). */
    private static final List<Reviewer> CONVERSATION_ORDER = List.of(
            Reviewer.HUMAN,
            Reviewer.OLLAMA_GPT_OSS,
            Reviewer.OLLAMA_LLAMA3,
            Reviewer.CLAUDE_CLI);

    private final List<OpinionEngine> engines;

    public OpinionCollector(List<OpinionEngine> engines) {
        this.engines = engines.stream()
                .sorted(Comparator.comparingInt(e -> CONVERSATION_ORDER.indexOf(e.reviewer())))
                .toList();
    }

    /** Returns a copy of the magazine with opinions attached to every news item. */
    public Magazine collect(Magazine magazine) {
        List<News> withOpinions = new ArrayList<>();
        for (News news : magazine.news()) {
            withOpinions.add(news.withOpinions(opinionsFor(news)));
        }
        return new Magazine(magazine.title(), magazine.releaseDate(), withOpinions);
    }

    private List<Opinion> opinionsFor(News news) {
        List<Opinion> opinions = new ArrayList<>();
        for (OpinionEngine engine : engines) {
            try {
                opinions.add(new Opinion(engine.reviewer(), engine.opine(news)));
            } catch (Exception e) {
                log.warn("{} produced no opinion for \"{}\": {}",
                        engine.reviewer(), news.title(), e.getMessage());
            }
        }
        return opinions;
    }
}
