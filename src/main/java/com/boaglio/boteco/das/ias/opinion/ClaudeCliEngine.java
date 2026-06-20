package com.boaglio.boteco.das.ias.opinion;

import com.boaglio.boteco.das.ias.config.BotecoProperties;
import com.boaglio.boteco.das.ias.model.News;
import com.boaglio.boteco.das.ias.model.Reviewer;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Reviewer backed by the local Claude Code CLI, invoked non-interactively as
 * {@code claude -p "<prompt>"}. Uses no API key — runs whatever the local
 * {@code claude} client is configured with.
 */
@Component
public class ClaudeCliEngine implements OpinionEngine {

    private static final long TIMEOUT_SECONDS = 180;

    private final String command;
    private final String promptFlag;

    public ClaudeCliEngine(BotecoProperties properties) {
        BotecoProperties.Reviewers.ClaudeCli cli = properties.reviewers().claudeCli();
        this.command = cli.command();
        this.promptFlag = cli.promptFlag();
    }

    @Override
    public Reviewer reviewer() {
        return Reviewer.CLAUDE_CLI;
    }

    @Override
    public String opine(News news) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command, promptFlag, OpinionPrompts.forNews(news));
        pb.redirectErrorStream(true);
        Process process = pb.start();

        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
        boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IOException("Claude CLI timed out after " + TIMEOUT_SECONDS + "s");
        }
        if (process.exitValue() != 0) {
            throw new IOException("Claude CLI exited with " + process.exitValue() + ": " + output);
        }
        return output;
    }
}
