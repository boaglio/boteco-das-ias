package com.boaglio.boteco.das.ias.opinion;

import com.boaglio.boteco.das.ias.config.BotecoProperties;
import com.boaglio.boteco.das.ias.model.Reviewer;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

/** Reviewer backed by local Ollama running the GPT-OSS model. */
@Component
public class OllamaGptOssEngine extends OllamaOpinionEngine {

    public OllamaGptOssEngine(ChatModel chatModel, BotecoProperties properties) {
        super(chatModel, properties.reviewers().ollama().gptOssModel());
    }

    @Override
    public Reviewer reviewer() {
        return Reviewer.OLLAMA_GPT_OSS;
    }
}
