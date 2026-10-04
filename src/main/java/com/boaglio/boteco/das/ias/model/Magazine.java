package com.boaglio.boteco.das.ias.model;

import java.time.LocalDate;
import java.util.List;

/**
 * The full magazine release: title, release date and the selected news items
 * (one per feed {@link Subject}, plus any {@link Subject#CUSTOM} items added by
 * hand). This is the object serialized to the JSON
 * file that flows through the whole build process.
 *
 * @param title       magazine title for this edition
 * @param releaseDate the date used to name the release file
 * @param news        the selected news items, one per feed Subject plus custom ones
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
