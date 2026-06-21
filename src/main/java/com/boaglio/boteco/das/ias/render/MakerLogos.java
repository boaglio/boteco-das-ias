package com.boaglio.boteco.das.ias.render;

import com.boaglio.boteco.das.ias.model.Reviewer;

/**
 * Tiny, self-contained inline-SVG badges shown next to each opinion's speaker,
 * hinting at the model's maker. Kept as simple original marks (a colour grid for
 * Microsoft, brand-coloured letter chips, a neutral avatar for the human) so the
 * magazine stays a single self-contained file with no external assets.
 */
final class MakerLogos {

    private MakerLogos() {
    }

    /** The inline-SVG badge for the reviewer's maker. */
    static String forReviewer(Reviewer reviewer) {
        return switch (reviewer) {
            case HUMAN -> PERSON;
            case OLLAMA_PHI -> MICROSOFT;
            case OLLAMA_LLAMA -> META;
            case CLAUDE_CLI -> ANTHROPIC;
        };
    }

    /** Microsoft — the four-colour square mark. */
    private static final String MICROSOFT =
            "<svg class=\"logo\" viewBox=\"0 0 10 10\" width=\"14\" height=\"14\" aria-hidden=\"true\">"
            + "<rect width=\"4.4\" height=\"4.4\" x=\"0\" y=\"0\" fill=\"#F25022\"/>"
            + "<rect width=\"4.4\" height=\"4.4\" x=\"5.6\" y=\"0\" fill=\"#7FBA00\"/>"
            + "<rect width=\"4.4\" height=\"4.4\" x=\"0\" y=\"5.6\" fill=\"#00A4EF\"/>"
            + "<rect width=\"4.4\" height=\"4.4\" x=\"5.6\" y=\"5.6\" fill=\"#FFB900\"/></svg>";

    /** Meta — brand-blue chip. */
    private static final String META = badge("#0467DF", "M");

    /** Anthropic — clay chip. */
    private static final String ANTHROPIC = badge("#D97757", "A");

    /** The human operator — neutral avatar. */
    private static final String PERSON =
            "<svg class=\"logo\" viewBox=\"0 0 16 16\" width=\"14\" height=\"14\" aria-hidden=\"true\">"
            + "<circle cx=\"8\" cy=\"8\" r=\"8\" fill=\"#b9b2a6\"/>"
            + "<circle cx=\"8\" cy=\"6.2\" r=\"2.6\" fill=\"#fff\"/>"
            + "<path d=\"M2.8 14c.4-2.7 2.6-4.2 5.2-4.2s4.8 1.5 5.2 4.2z\" fill=\"#fff\"/></svg>";

    private static String badge(String color, String letter) {
        return "<svg class=\"logo\" viewBox=\"0 0 16 16\" width=\"14\" height=\"14\" aria-hidden=\"true\">"
                + "<rect width=\"16\" height=\"16\" rx=\"4\" fill=\"" + color + "\"/>"
                + "<text x=\"8\" y=\"11.5\" text-anchor=\"middle\" font-family=\"Arial,sans-serif\" "
                + "font-size=\"10\" font-weight=\"700\" fill=\"#fff\">" + letter + "</text></svg>";
    }
}
