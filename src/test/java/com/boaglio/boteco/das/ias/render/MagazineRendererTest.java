package com.boaglio.boteco.das.ias.render;

import com.boaglio.boteco.das.ias.config.BotecoProperties;
import com.boaglio.boteco.das.ias.config.BotecoProperties.FooterLink;
import com.boaglio.boteco.das.ias.model.Magazine;
import com.boaglio.boteco.das.ias.model.News;
import com.boaglio.boteco.das.ias.model.Opinion;
import com.boaglio.boteco.das.ias.model.Reviewer;
import com.boaglio.boteco.das.ias.model.Subject;
import com.boaglio.boteco.das.ias.storage.MagazineStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MagazineRendererTest {

    private static final LocalDate RELEASE = LocalDate.of(2026, 6, 20);
    private static final List<FooterLink> FOOTER = List.of(
            new FooterLink("GitHub", "https://github.com/boaglio", "github"));

    private BotecoProperties properties(String releasesDir) {
        return new BotecoProperties(null, 0, null, null, null, releasesDir, null, null, FOOTER);
    }

    private MagazineRenderer rendererIn(Path releasesDir) {
        var props = properties(releasesDir.toString());
        return new MagazineRenderer(new MagazineStore(props, new ObjectMapper()), props);
    }

    private Magazine sampleMagazine() {
        var java = new News(Subject.JAVA, "JEP 500 lands", "https://inside.java/jep", "inside.java",
                RELEASE, "A big JEP <ships>.", List.of(
                new Opinion(Reviewer.HUMAN, "Curti demais."),
                new Opinion(Reviewer.CLAUDE_CLI, "Solid improvement.")),
                "images/java.png",
                "JEP 500 chega", "Um grande JEP <chega>.");
        var tech = new News(Subject.TECHNOLOGY, "Tech thing", "https://infoq/x", "infoq",
                RELEASE, "Summary two.", List.of(), null, null, null);
        return new Magazine("Boteco das IAs — 2026-06-20", RELEASE, List.of(java, tech));
    }

    @Test
    void rendersTranslationOpinionsImagesAlignmentAndFooter() {
        var html = rendererIn(Path.of("unused")).render(sampleMagazine());

        assertThat(html)
                .contains("<title>Boteco das IAs — 2026-06-20</title>")
                .contains("JEP 500 chega")                       // translated headline as the title
                .contains("class=\"original-title\">JEP 500 lands")  // original shown beneath
                .contains("Um grande JEP &lt;chega&gt;.")        // pt-BR summary, HTML-escaped
                .contains("<img class=\"image\" src=\"images/java.png\"")
                .contains("placeholder")                         // second item has no image
                .contains("class=\"opinion left\"")              // HUMAN on the left
                .contains("class=\"opinion right\"")             // CLAUDE on the right
                .contains("Curti demais.")
                .contains("class=\"footer-list\"")                // organized footer list
                .contains("href=\"https://github.com/boaglio\"")  // footer link
                .contains("<span>GitHub</span>");                 // footer link label
    }

    @Test
    void fallsBackToOriginalWhenUntranslated() {
        var html = rendererIn(Path.of("unused")).render(sampleMagazine());

        // The untranslated tech item keeps its English text and shows no original-title line.
        assertThat(html).contains("Tech thing").contains("Summary two.");
        assertThat(html).doesNotContain("class=\"original-title\">Tech thing");
    }

    @Test
    void writesMagazineHtmlIntoReleaseDir(@TempDir Path releasesDir) {
        var renderer = rendererIn(releasesDir);

        var htmlPath = renderer.renderToFile(sampleMagazine());

        var expected = new MagazineStore(properties(releasesDir.toString()), new ObjectMapper())
                .releaseDir(RELEASE).resolve("magazine.html");
        assertThat(htmlPath).isEqualTo(expected);
        assertThat(Files.exists(htmlPath)).isTrue();
    }
}
