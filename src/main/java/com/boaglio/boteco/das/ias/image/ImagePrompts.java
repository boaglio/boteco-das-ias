package com.boaglio.boteco.das.ias.image;

import com.boaglio.boteco.das.ias.model.News;
import com.boaglio.boteco.das.ias.model.Subject;

import java.util.Locale;

/** Builds the scene description used to illustrate a news item. */
public final class ImagePrompts {

    private ImagePrompts() {
    }

    /**
     * A text-free fallback scene, themed only by subject — used when no AI scene
     * description is available. Deliberately omits the headline/summary so no
     * article text is fed to the image model (which would render it as text).
     */
    public static String forNews(News news) {
        // A custom item has no technical theme of its own, so fall back to plain "technology".
        var subject = news.subject() == Subject.CUSTOM ? "technology"
                : news.subject().label().toLowerCase(Locale.ROOT);
        return "a symbolic, wordless anime illustration evoking %s technology, "
                .formatted(subject)
                + "conceptual objects and characters, no text, no letters, no signs";
    }
}
