package com.boaglio.boteco.das.ias.image;

import com.boaglio.boteco.das.ias.config.BotecoProperties;
import com.boaglio.boteco.das.ias.model.News;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.stereotype.Service;

/**
 * Turns a news item into a vivid, purely visual scene description for the image
 * model, using a local Ollama model. The description stays news-related but
 * contains no words to depict, so the rendered image carries no text (the
 * headline/summary are never fed verbatim to the image model).
 */
@Service
public class SceneDescriber {

    private final ChatModel chatModel;
    private final String model;

    public SceneDescriber(ChatModel chatModel, BotecoProperties properties) {
        this.chatModel = chatModel;
        this.model = properties.reviewers().ollama().llamaModel();
    }

    /** A one-sentence, text-free visual scene for the news item. */
    public String describe(News news) {
        var prompt = new Prompt(promptFor(news),
                OllamaChatOptions.builder().model(model).build());
        return chatModel.call(prompt).getResult().getOutput().getText().strip();
    }

    private static String promptFor(News news) {
        return """
                You are an art director briefing an anime illustrator.
                In ONE vivid English sentence, describe an illustration that represents
                the news below. Describe only physical objects, characters, setting,
                colours and mood. The illustration MUST contain no text, words, letters,
                numbers, signs, labels or logos. Do not quote the headline.
                Reply with the description only — no preamble.

                Subject: %s
                Headline: %s
                Summary: %s
                """.formatted(news.subject(), news.title(), news.summary());
    }
}
