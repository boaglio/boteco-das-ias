package com.boaglio.boteco.das.ias.news;

import com.boaglio.boteco.das.ias.model.News;
import com.boaglio.boteco.das.ias.model.Subject;

import java.util.List;

/**
 * Picks the final news item for a subject out of a short, pre-ranked list of
 * candidates (best first). Kept as an interface so the interactive console
 * default can be swapped for an automated chooser in tests or future modes.
 */
public interface NewsChooser {

    /**
     * @param ranked candidates for {@code subject}, best-ranked first; never empty
     * @return the chosen item, always one of {@code ranked}
     */
    News choose(Subject subject, List<News> ranked);
}
