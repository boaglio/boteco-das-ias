package com.boaglio.boteco.das.ias.opinion;

import com.boaglio.boteco.das.ias.config.BotecoProperties;
import com.boaglio.boteco.das.ias.model.Reviewer;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

/** Reviewer backed by local Ollama running Microsoft's Phi-4 mini model. */
@Component
public class OllamaPhiEngine extends OllamaOpinionEngine {

    public OllamaPhiEngine(ChatModel chatModel, BotecoProperties properties) {
        super(chatModel, properties.reviewers().ollama().phiModel());
    }

    @Override
    public Reviewer reviewer() {
        return Reviewer.OLLAMA_PHI;
    }
}
