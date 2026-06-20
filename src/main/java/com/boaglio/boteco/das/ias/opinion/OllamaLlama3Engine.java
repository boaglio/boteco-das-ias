package com.boaglio.boteco.das.ias.opinion;

import com.boaglio.boteco.das.ias.config.BotecoProperties;
import com.boaglio.boteco.das.ias.model.Reviewer;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

/** Reviewer backed by local Ollama running the Llama3 model. */
@Component
public class OllamaLlama3Engine extends OllamaOpinionEngine {

    public OllamaLlama3Engine(ChatModel chatModel, BotecoProperties properties) {
        super(chatModel, properties.reviewers().ollama().llama3Model());
    }

    @Override
    public Reviewer reviewer() {
        return Reviewer.OLLAMA_LLAMA3;
    }
}
