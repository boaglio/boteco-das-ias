package com.boaglio.boteco.das.ias.image;

import com.boaglio.boteco.das.ias.config.BotecoProperties;
import com.boaglio.boteco.das.ias.model.Magazine;
import com.boaglio.boteco.das.ias.model.News;
import com.boaglio.boteco.das.ias.model.Subject;
import com.boaglio.boteco.das.ias.storage.MagazineStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ImageGeneratorTest {

    private static final LocalDate RELEASE = LocalDate.of(2026, 6, 20);

    private record FakeEngine(byte[] png, boolean fail) implements ImageEngine {
        @Override
        public byte[] generate(String scenePrompt) throws Exception {
            if (fail) {
                throw new IllegalStateException("ComfyUI down");
            }
            return png;
        }
    }

    private MagazineStore storeIn(Path releasesDir) {
        BotecoProperties properties = new BotecoProperties(
                null, 0, null, null, null, releasesDir.toString(), null, null, null);
        return new MagazineStore(properties, new ObjectMapper());
    }

    private Magazine twoItemMagazine() {
        News java = new News(Subject.JAVA, "JEP news", "https://x", "inside.java",
                RELEASE, "summary", List.of(), null, null, null);
        News tech = new News(Subject.TECHNOLOGY, "Tech news", "https://y", "infoq",
                RELEASE, "summary", List.of(), null, null, null);
        return new Magazine("title", RELEASE, List.of(java, tech));
    }

    @Test
    void writesImagesAndAttachesRelativePaths(@TempDir Path releasesDir) {
        byte[] png = "fake-png".getBytes(StandardCharsets.UTF_8);
        MagazineStore store = storeIn(releasesDir);

        Magazine result = new ImageGenerator(new FakeEngine(png, false), null, store)
                .illustrate(twoItemMagazine());

        assertThat(result.news()).extracting(News::imagePath)
                .containsExactly("images/java.png", "images/technology.png");
        Path imagesDir = store.releaseDir(RELEASE).resolve("images");
        assertThat(Files.exists(imagesDir.resolve("java.png"))).isTrue();
        assertThat(Files.exists(imagesDir.resolve("technology.png"))).isTrue();
    }

    @Test
    void leavesImagePathNullWhenRenderFails(@TempDir Path releasesDir) {
        MagazineStore store = storeIn(releasesDir);

        Magazine result = new ImageGenerator(new FakeEngine(null, true), null, store)
                .illustrate(twoItemMagazine());

        assertThat(result.news()).extracting(News::imagePath).containsOnlyNulls();
        assertThat(Files.exists(store.releaseDir(RELEASE).resolve("images").resolve("java.png")))
                .isFalse();
    }
}
