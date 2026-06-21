package com.boaglio.boteco.das.ias.image;

import com.boaglio.boteco.das.ias.model.News;

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
        var subject = news.subject().name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return "a symbolic, wordless anime illustration evoking %s technology, "
                .formatted(subject)
                + "conceptual objects and characters, no text, no letters, no signs";
    }
}
