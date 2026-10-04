package com.boaglio.boteco.das.ias.news;

import com.boaglio.boteco.das.ias.model.News;
import com.boaglio.boteco.das.ias.model.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Asks the operator, on the console, for news items to add by hand on top of
 * the gathered ones ({@link Subject#CUSTOM}), so the reviewers comment on them
 * too. Loops until a blank headline; end of input also stops, so unattended
 * runs simply add nothing.
 *
 * <p>The summary is multi-line (ended by a line holding just {@value #END_OF_SUMMARY}),
 * since it's often pasted from a post. The link and the yes/no answer are
 * validated and asked again when invalid — so stray pasted lines can't
 * silently turn into other fields or into a second news item.
 */
@Component
public class CustomNewsPrompter {

    private static final Logger log = LoggerFactory.getLogger(CustomNewsPrompter.class);
    private static final String DEFAULT_SOURCE = "Boteco das IAs";
    static final String END_OF_SUMMARY = ".";

    private final BufferedReader in;

    public CustomNewsPrompter() {
        this(new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)));
    }

    CustomNewsPrompter(BufferedReader in) {
        this.in = in;
    }

    /**
     * @param existing items already in the edition, used to reject duplicates
     * @return the new custom items, in the order they were typed; possibly empty
     */
    public List<News> prompt(List<News> existing) {
        var taken = new HashSet<String>();
        existing.forEach(news -> taken.add(NewsGatherer.key(news)));
        var added = new ArrayList<News>();

        System.out.printf("%n=== Notícia extra (adicionada por você) ===%n");
        System.out.println("Deixe o título em branco para terminar.");
        while (true) {
            var title = ask("Título> ");
            if (title.isBlank()) {
                return added;
            }
            var url = askUrl();
            var summary = askSummary();
            if (summary.isBlank()) {
                summary = title;
            }
            var defaultSource = sourceFrom(url);
            var source = ask("Fonte [%s]> ".formatted(defaultSource));
            if (source.isBlank()) {
                source = defaultSource;
            }
            var inPortuguese = askYesNo("Texto em português? [S/n]> ");

            var news = new News(Subject.CUSTOM, title, url.isBlank() ? null : url, source,
                    LocalDate.now(), summary, List.of(), null,
                    // Already pt-BR: fill the translation in so the translate stage skips it.
                    inPortuguese ? title : null, inPortuguese ? summary : null);
            if (!taken.add(NewsGatherer.key(news))) {
                System.out.println("  ✗ essa notícia já está na edição — ignorada");
                continue;
            }
            added.add(news);
            log.info("Custom news added: \"{}\"", title);
            System.out.println("  ✓ adicionada");
        }
    }

    /** Asks for an optional http(s) link until the answer is blank or valid. */
    private String askUrl() {
        while (true) {
            var url = ask("Link (opcional)> ");
            if (url.isBlank() || isHttpUrl(url)) {
                return url;
            }
            System.out.println("  ✗ link inválido — use http(s)://… ou deixe em branco");
        }
    }

    /** Reads summary lines until a line with just {@value #END_OF_SUMMARY}, or end of input. */
    private String askSummary() {
        System.out.printf("Resumo (é o que as IAs vão comentar; pode colar várias linhas, termine com uma linha só com \"%s\")%n",
                END_OF_SUMMARY);
        var lines = new ArrayList<String>();
        while (true) {
            var line = readLine("> ");
            if (line == null || line.strip().equals(END_OF_SUMMARY)) {
                return String.join("\n", lines).strip();
            }
            lines.add(line.stripTrailing());
        }
    }

    /** Asks a yes/no question (yes by default) until the answer is blank, s/y or n. */
    private boolean askYesNo(String question) {
        while (true) {
            var answer = ask(question).toLowerCase();
            if (answer.isBlank() || answer.startsWith("s") || answer.startsWith("y")) {
                return true;
            }
            if (answer.startsWith("n")) {
                return false;
            }
            System.out.println("  ✗ responda s ou n");
        }
    }

    /** Prints the question and reads one stripped line; end of input reads as blank. */
    private String ask(String question) {
        var line = readLine(question);
        return line == null ? "" : line.strip();
    }

    /** Prints the prompt and reads one raw line, or null at end of input. */
    private String readLine(String prompt) {
        System.out.print(prompt);
        System.out.flush();
        try {
            return in.readLine();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read custom news from the console", e);
        }
    }

    static boolean isHttpUrl(String url) {
        try {
            var uri = URI.create(url);
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** The link's host (without "www."), or the magazine's name when there's no usable link. */
    static String sourceFrom(String url) {
        try {
            var host = url.isBlank() ? null : URI.create(url).getHost();
            return host == null ? DEFAULT_SOURCE : host.replaceFirst("^www\\.", "");
        } catch (IllegalArgumentException e) {
            return DEFAULT_SOURCE;
        }
    }
}
