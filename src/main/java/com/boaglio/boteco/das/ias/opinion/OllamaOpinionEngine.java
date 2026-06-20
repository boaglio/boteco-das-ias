package com.boaglio.boteco.das.ias.opinion;

import com.boaglio.boteco.das.ias.model.News;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.api.OllamaChatOptions;

/**
 * Base for reviewers backed by a local Ollama model. The concrete model name
 * is selected per call so a single {@link ChatModel} bean can serve both
 * GPT-OSS and Llama3 engines.
 */
public abstract class OllamaOpinionEngine implements OpinionEngine {

    private final ChatModel chatModel;
    private final String model;

    protected OllamaOpinionEngine(ChatModel chatModel, String model) {
        this.chatModel = chatModel;
        this.model = model;
    }

    @Override
    public String opine(News news) {
        Prompt prompt = new Prompt(
                OpinionPrompts.forNews(news),
                OllamaChatOptions.builder().model(model).build());
        return chatModel.call(prompt).getResult().getOutput().getText().strip();
    }
}
