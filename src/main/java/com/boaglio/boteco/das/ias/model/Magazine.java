package com.boaglio.boteco.das.ias.model;

import java.time.LocalDate;
import java.util.List;

/**
 * The full magazine release: title, release date and the four selected news
 * items (one per {@link Subject}). This is the object serialized to the JSON
 * file that flows through the whole build process.
 *
 * @param title       magazine title for this edition
 * @param releaseDate the date used to name the release file
 * @param news        the selected news items, expected one per Subject
 */
public record Magazine(
        String title,
        LocalDate releaseDate,
        List<News> news
) {
    public Magazine {
        news = news == null ? List.of() : List.copyOf(news);
    }
}
