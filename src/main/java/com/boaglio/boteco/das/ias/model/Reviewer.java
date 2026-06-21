package com.boaglio.boteco.das.ias.model;

/** The four opinion engines that comment on each selected news item. */
public enum Reviewer {
    /** Claude Code CLI, invoked locally via {@code claude -p}. */
    CLAUDE_CLI,
    /** Local Ollama running Microsoft's Phi-4 mini model. */
    OLLAMA_PHI,
    /** Local Ollama running Meta's Llama 3.2 model. */
    OLLAMA_LLAMA,
    /** The human operator, prompted interactively on the console. */
    HUMAN
}
