package com.boaglio.boteco.das.ias.model;

import java.time.LocalDate;
import java.util.List;

/**
 * A selected news item — the "best" pick for its {@link Subject} — enriched
 * with a pt-BR translation, reviewer opinions and an anime-style image as the
 * pipeline progresses.
 *
 * @param subject       the category this item represents
 * @param title         original headline (kept verbatim from the source)
 * @param url           link to the official article
 * @param source        official source name (e.g. "inside.java", "spring.io")
 * @param publishedDate publication date from the feed
 * @param summary       original short summary used for prompts
 * @param opinions      collected reviewer opinions (filled in stage 2)
 * @param imagePath     path to the generated image, relative to the release (stage 3)
 * @param titlePt       headline translated to Brazilian Portuguese (translate stage)
 * @param summaryPt     summary translated to Brazilian Portuguese (translate stage)
 */
public record News(
        Subject subject,
        String title,
        String url,
        String source,
        LocalDate publishedDate,
        String summary,
        List<Opinion> opinions,
        String imagePath,
        String titlePt,
        String summaryPt
) {
    public News {
        opinions = opinions == null ? List.of() : List.copyOf(opinions);
    }

    /** Returns a copy of this item with the given opinions attached. */
    public News withOpinions(List<Opinion> newOpinions) {
        return new News(subject, title, url, source, publishedDate, summary,
                newOpinions, imagePath, titlePt, summaryPt);
    }

    /** Returns a copy of this item with the given generated image path attached. */
    public News withImagePath(String newImagePath) {
        return new News(subject, title, url, source, publishedDate, summary,
                opinions, newImagePath, titlePt, summaryPt);
    }

    /** Returns a copy of this item with the pt-BR headline and summary attached. */
    public News withTranslation(String newTitlePt, String newSummaryPt) {
        return new News(subject, title, url, source, publishedDate, summary,
                opinions, imagePath, newTitlePt, newSummaryPt);
    }
}
