package com.boaglio.boteco.das.ias.translate;

import com.boaglio.boteco.das.ias.config.BotecoProperties;
import com.boaglio.boteco.das.ias.model.Magazine;
import com.boaglio.boteco.das.ias.model.News;
import com.boaglio.boteco.das.ias.model.Subject;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class NewsTranslatorTest {

    /** Counts model invocations and echoes a fixed translation. */
    private static class CountingChatModel implements ChatModel {
        final AtomicInteger calls = new AtomicInteger();

        @Override
        public ChatResponse call(Prompt prompt) {
            calls.incrementAndGet();
            return new ChatResponse(List.of(new Generation(new AssistantMessage("traduzido"))));
        }
    }

    private static BotecoProperties properties() {
        var ollama = new BotecoProperties.Reviewers.Ollama("phi", "llama");
        var reviewers = new BotecoProperties.Reviewers(null, ollama);
        return new BotecoProperties("title", 7, null, reviewers, null, "releases",
                null, null, null);
    }

    private static Magazine magazineWith(News news) {
        return new Magazine("title", LocalDate.of(2026, 6, 20), List.of(news));
    }

    @Test
    void skipsItemsAlreadyFullyTranslated() {
        var model = new CountingChatModel();
        var translator = new NewsTranslator(model, properties());
        var news = new News(Subject.JAVA, "JEP news", "https://x", "inside.java",
                LocalDate.of(2026, 6, 18), "summary", List.of(), null,
                "título pt", "resumo pt");

        var result = translator.translate(magazineWith(news));

        assertThat(model.calls).hasValue(0);
        assertThat(result.news().get(0).titlePt()).isEqualTo("título pt");
        assertThat(result.news().get(0).summaryPt()).isEqualTo("resumo pt");
    }

    @Test
    void translatesOnlyTheMissingField() {
        var model = new CountingChatModel();
        var translator = new NewsTranslator(model, properties());
        // Title already translated; summary still missing.
        var news = new News(Subject.JAVA, "JEP news", "https://x", "inside.java",
                LocalDate.of(2026, 6, 18), "summary", List.of(), null,
                "título pt", null);

        var result = translator.translate(magazineWith(news));

        assertThat(model.calls).hasValue(1);
        assertThat(result.news().get(0).titlePt()).isEqualTo("título pt");
        assertThat(result.news().get(0).summaryPt()).isEqualTo("traduzido");
    }
}
