package com.boaglio.boteco.das.ias.opinion;

import com.boaglio.boteco.das.ias.model.News;

/** Builds the shared prompt sent to each AI reviewer. */
public final class OpinionPrompts {

    private OpinionPrompts() {
    }

    /** A concise, opinionated-take prompt for the given news item. */
    public static String forNews(News news) {
        return """
                You are a tech commentator for the "Boteco das IAs" weekly newsletter.
                Give a short, opinionated take (2-3 sentences, first person) on the news below.
                Reply with the opinion text only — no preamble, no markdown, no quotes.

                Subject: %s
                Title: %s
                Source: %s
                Summary: %s
                Link: %s
                """.formatted(
                news.subject(), news.title(), news.source(), news.summary(), news.url());
    }
}
