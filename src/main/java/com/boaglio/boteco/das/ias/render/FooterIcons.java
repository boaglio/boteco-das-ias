package com.boaglio.boteco.das.ias.render;

/**
 * Small, self-contained inline-SVG badges for the footer link list — a
 * brand-coloured rounded square with a simple original glyph, so the magazine
 * stays a single file with no external icon assets.
 */
final class FooterIcons {

    private FooterIcons() {
    }

    /** The inline-SVG badge for the given icon slug (a generic link badge if unknown). */
    static String forSlug(String slug) {
        return switch (slug == null ? "" : slug) {
            case "github" -> badge("#181717", CODE);
            case "linkedin" -> badge("#0A66C2", letter("in", 8));
            case "youtube" -> badge("#FF0000", PLAY);
            case "x", "twitter" -> badge("#000000", letter("X", 10));
            case "amazon" -> badge("#FF9900", letter("a", 10));
            case "book" -> badge("#6B4F2A", BOOK);
            case "blog", "rss" -> badge("#C0392B", RSS);
            default -> badge("#3AA856", LINK);
        };
    }

    private static final String PLAY = "<path d=\"M6 4.6l5 3.4-5 3.4z\" fill=\"#fff\"/>";
    private static final String CODE =
            "<path d=\"M6.2 5.4 3.6 8l2.6 2.6M9.8 5.4 12.4 8l-2.6 2.6\" fill=\"none\" "
            + "stroke=\"#fff\" stroke-width=\"1.3\" stroke-linecap=\"round\" stroke-linejoin=\"round\"/>";
    private static final String BOOK =
            "<path d=\"M5 4h5a1 1 0 0 1 1 1v7l-3.5-1.5L5 12z\" fill=\"#fff\"/>";
    private static final String RSS =
            "<circle cx=\"5\" cy=\"11\" r=\"1.2\" fill=\"#fff\"/>"
            + "<path d=\"M4.6 7.2a4.2 4.2 0 0 1 4.2 4.2M4.6 4.7a6.7 6.7 0 0 1 6.7 6.7\" "
            + "fill=\"none\" stroke=\"#fff\" stroke-width=\"1.3\" stroke-linecap=\"round\"/>";
    private static final String LINK =
            "<path d=\"M7 9l2-2M6.6 10.4a2 2 0 0 1 0-2.8l1-1a2 2 0 0 1 2.8 0M9.4 5.6a2 2 0 0 1 "
            + "2.8 0 2 2 0 0 1 0 2.8l-1 1\" fill=\"none\" stroke=\"#fff\" stroke-width=\"1.2\" "
            + "stroke-linecap=\"round\"/>";

    private static String letter(String text, int size) {
        return "<text x=\"8\" y=\"11.6\" text-anchor=\"middle\" font-family=\"Arial,sans-serif\" "
                + "font-size=\"" + size + "\" font-weight=\"700\" fill=\"#fff\">" + text + "</text>";
    }

    private static String badge(String color, String inner) {
        return "<svg class=\"flogo\" viewBox=\"0 0 16 16\" width=\"16\" height=\"16\" aria-hidden=\"true\">"
                + "<rect width=\"16\" height=\"16\" rx=\"4\" fill=\"" + color + "\"/>" + inner + "</svg>";
    }
}
