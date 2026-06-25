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
import java.util.Optional;

/**
 * Opinion-collection stage of the build process: attach every reviewer's take
 * to each news item, in the magazine's conversation order.
 *
 * <p>Reviewers are processed one engine at a time across all items (rather than
 * all engines per item). With local models this matters: a heavy Ollama model
 * loads at most once for the whole stage instead of being repeatedly evicted
 * and reloaded as we alternate engines. A failing engine (Ollama down, no human
 * input) is logged and skipped so the rest of the conversation still records.
 */
@Service
public class OpinionCollector {

    private static final Logger log = LoggerFactory.getLogger(OpinionCollector.class);

    /** Conversation order used by the magazine layout (left/right/left/right). */
    private static final List<Reviewer> CONVERSATION_ORDER = List.of(
            Reviewer.HUMAN,
            Reviewer.OLLAMA_PHI,
            Reviewer.OLLAMA_LLAMA,
            Reviewer.CLAUDE_CLI);

    private final List<OpinionEngine> engines;

    public OpinionCollector(List<OpinionEngine> engines) {
        this.engines = engines.stream()
                .sorted(Comparator.comparingInt(e -> CONVERSATION_ORDER.indexOf(e.reviewer())))
                .toList();
    }

    /** Returns a copy of the magazine with opinions attached to every news item. */
    public Magazine collect(Magazine magazine) {
        var news = magazine.news();
        // One ordered opinion list per item; engines append in conversation order.
        var opinionsByItem = new ArrayList<List<Opinion>>();
        for (var ignored : news) {
            opinionsByItem.add(new ArrayList<>());
        }

        // Outer loop is the engine, so each (possibly heavy) model is used for
        // every item before we move on to the next reviewer.
        for (var engine : engines) {
            for (var i = 0; i < news.size(); i++) {
                var item = news.get(i);
                var existing = existingOpinion(item, engine.reviewer());
                if (existing.isPresent()) {
                    log.info("{}: opinion for \"{}\" already exists, skipping",
                            engine.reviewer(), item.title());
                    opinionsByItem.get(i).add(existing.get());
                } else {
                    opine(engine, item).ifPresent(opinionsByItem.get(i)::add);
                }
            }
        }

        var withOpinions = new ArrayList<News>();
        for (var i = 0; i < news.size(); i++) {
            withOpinions.add(news.get(i).withOpinions(opinionsByItem.get(i)));
        }
        return new Magazine(magazine.title(), magazine.releaseDate(), withOpinions);
    }

    /** An already-collected, non-blank opinion from the reviewer, if any. */
    private static Optional<Opinion> existingOpinion(News news, Reviewer reviewer) {
        return news.opinions().stream()
                .filter(o -> o.reviewer() == reviewer)
                .filter(o -> o.text() != null && !o.text().isBlank())
                .findFirst();
    }

    private Optional<Opinion> opine(OpinionEngine engine, News news) {
        try {
            return Optional.of(new Opinion(engine.reviewer(), engine.opine(news)));
        } catch (Exception e) {
            log.warn("{} produced no opinion for \"{}\": {}",
                    engine.reviewer(), news.title(), e.getMessage());
            return Optional.empty();
        }
    }
}
