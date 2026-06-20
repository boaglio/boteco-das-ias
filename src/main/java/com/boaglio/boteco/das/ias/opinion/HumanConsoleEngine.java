package com.boaglio.boteco.das.ias.opinion;

import com.boaglio.boteco.das.ias.model.News;
import com.boaglio.boteco.das.ias.model.Reviewer;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Reviewer that shows the news on the console and prompts the operator for
 * their own opinion (the "show me and prompt for my input" step).
 */
@Component
public class HumanConsoleEngine implements OpinionEngine {

    private final BufferedReader in;

    public HumanConsoleEngine() {
        this(new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)));
    }

    HumanConsoleEngine(BufferedReader in) {
        this.in = in;
    }

    @Override
    public Reviewer reviewer() {
        return Reviewer.HUMAN;
    }

    @Override
    public String opine(News news) throws IOException {
        System.out.printf("%n=== [%s] %s ===%n", news.subject(), news.title());
        System.out.println(news.url());
        System.out.println(news.summary());
        System.out.print("Your opinion> ");
        System.out.flush();

        String line = in.readLine();
        if (line == null || line.isBlank()) {
            throw new IOException("No opinion entered");
        }
        return line.strip();
    }
}
