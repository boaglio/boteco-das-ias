package com.boaglio.boteco.das.ias.news;

import com.boaglio.boteco.das.ias.model.News;
import com.boaglio.boteco.das.ias.model.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Default {@link NewsChooser}: shows the ranked candidates on the console and
 * asks the operator which one to feature (the "choose among the top 3" step).
 * Any unparsable or out-of-range answer, including a blank line, keeps the
 * top-ranked candidate — so unattended/scripted runs still make progress.
 */
@Component
public class ConsoleNewsChooser implements NewsChooser {

    private static final Logger log = LoggerFactory.getLogger(ConsoleNewsChooser.class);

    private final BufferedReader in;

    public ConsoleNewsChooser() {
        this(new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)));
    }

    ConsoleNewsChooser(BufferedReader in) {
        this.in = in;
    }

    @Override
    public News choose(Subject subject, List<News> ranked) {
        if (ranked.size() == 1) {
            return ranked.get(0);
        }
        System.out.printf("%n=== %s: choose the news to feature ===%n", subject);
        for (int i = 0; i < ranked.size(); i++) {
            var candidate = ranked.get(i);
            System.out.printf("%d) [%s] %s%n    %s%n", i + 1, candidate.source(), candidate.title(), candidate.url());
        }
        System.out.printf("Pick 1-%d (default 1)> ", ranked.size());
        System.out.flush();

        var choice = readChoice(ranked.size());
        var picked = ranked.get(choice - 1);
        log.info("{}: operator picked #{} \"{}\"", subject, choice, picked.title());
        return picked;
    }

    /** Parses the operator's answer into a valid 1-based index, defaulting to 1. */
    private int readChoice(int max) {
        try {
            var line = in.readLine();
            if (line == null || line.isBlank()) {
                return 1;
            }
            var choice = Integer.parseInt(line.strip());
            return choice >= 1 && choice <= max ? choice : 1;
        } catch (IOException | NumberFormatException e) {
            return 1;
        }
    }
}
