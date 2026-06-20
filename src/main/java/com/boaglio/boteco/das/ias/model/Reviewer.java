package com.boaglio.boteco.das.ias.model;

/** The four opinion engines that comment on each selected news item. */
public enum Reviewer {
    /** Claude Code CLI, invoked locally via {@code claude -p}. */
    CLAUDE_CLI,
    /** Local Ollama running the GPT-OSS model. */
    OLLAMA_GPT_OSS,
    /** Local Ollama running the Llama3 model. */
    OLLAMA_LLAMA3,
    /** The human operator, prompted interactively on the console. */
    HUMAN
}
