package com.boaglio.boteco.das.ias.opinion;

import com.boaglio.boteco.das.ias.config.BotecoProperties;
import com.boaglio.boteco.das.ias.model.Reviewer;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

/** Reviewer backed by local Ollama running Meta's Llama 3.2 model. */
@Component
public class OllamaLlamaEngine extends OllamaOpinionEngine {

    public OllamaLlamaEngine(ChatModel chatModel, BotecoProperties properties) {
        super(chatModel, properties.reviewers().ollama().llamaModel());
    }

    @Override
    public Reviewer reviewer() {
        return Reviewer.OLLAMA_LLAMA;
    }
}
