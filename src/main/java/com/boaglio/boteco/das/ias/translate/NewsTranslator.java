package com.boaglio.boteco.das.ias.translate;

import com.boaglio.boteco.das.ias.config.BotecoProperties;
import com.boaglio.boteco.das.ias.model.Magazine;
import com.boaglio.boteco.das.ias.model.News;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

/**
 * Translation stage of the build process: render each news item's headline and
 * summary into Brazilian Portuguese (keeping technical terms in English) so the
 * magazine reads for its dev/IT-student audience. The original English headline
 * is kept too — the layout shows both. A failing translation is logged and the
 * field is left null, so rendering falls back to the original text.
 */
@Service
public class NewsTranslator {

    private static final Logger log = LoggerFactory.getLogger(NewsTranslator.class);

    private final ChatModel chatModel;
    private final String model;

    public NewsTranslator(ChatModel chatModel, BotecoProperties properties) {
        this.chatModel = chatModel;
        // Reuse the Llama reviewer model for translation.
        this.model = properties.reviewers().ollama().llamaModel();
    }

    /** Returns a copy of the magazine with pt-BR headline and summary attached to each item. */
    public Magazine translate(Magazine magazine) {
        var translated = new ArrayList<News>();
        for (var news : magazine.news()) {
            translated.add(translate(news));
        }
        return new Magazine(magazine.title(), magazine.releaseDate(), translated);
    }

    private News translate(News news) {
        var titlePt = translateText(news.title());
        var summaryPt = translateText(news.summary());
        log.info("{}: translated headline -> {}", news.subject(),
                titlePt != null ? titlePt : "(kept original)");
        return news.withTranslation(titlePt, summaryPt);
    }

    /** Translates one snippet; returns null (so callers fall back) when unavailable. */
    private String translateText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            var prompt = new Prompt(
                    TranslationPrompts.toBrazilianPortuguese(text),
                    OllamaChatOptions.builder().model(model).build());
            return chatModel.call(prompt).getResult().getOutput().getText().strip();
        } catch (Exception e) {
            log.warn("Translation failed, keeping original text: {}", e.getMessage());
            return null;
        }
    }
}
