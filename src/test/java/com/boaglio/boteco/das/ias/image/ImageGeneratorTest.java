package com.boaglio.boteco.das.ias.image;

import com.boaglio.boteco.das.ias.config.BotecoProperties;
import com.boaglio.boteco.das.ias.model.Magazine;
import com.boaglio.boteco.das.ias.model.News;
import com.boaglio.boteco.das.ias.model.Subject;
import com.boaglio.boteco.das.ias.storage.MagazineStore;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageGeneratorTest {

    private static final LocalDate RELEASE = LocalDate.of(2026, 6, 20);

    private record FakeEngine(byte[] png, boolean fail, boolean unavailable) implements ImageEngine {
        @Override
        public byte[] generate(String scenePrompt) throws Exception {
            if (fail) {
                throw new IllegalStateException("ComfyUI down");
            }
            return png;
        }

        @Override
        public void checkAvailable() {
            if (unavailable) {
                throw new IllegalStateException("ComfyUI is not reachable");
            }
        }
    }

    private MagazineStore storeIn(Path releasesDir) {
        BotecoProperties properties = new BotecoProperties(
                null, 0, null, null, null, null, releasesDir.toString(), null, null, null);
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

        Magazine result = new ImageGenerator(new FakeEngine(png, false, false), null, store)
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

        Magazine result = new ImageGenerator(new FakeEngine(null, true, false), null, store)
                .illustrate(twoItemMagazine());

        assertThat(result.news()).extracting(News::imagePath).containsOnlyNulls();
        assertThat(Files.exists(store.releaseDir(RELEASE).resolve("images").resolve("java.png")))
                .isFalse();
    }

    @Test
    void abortsUpFrontWhenTheEngineIsNotReachable(@TempDir Path releasesDir) {
        MagazineStore store = storeIn(releasesDir);
        ImageGenerator generator = new ImageGenerator(new FakeEngine(null, false, true), null, store);

        assertThatThrownBy(() -> generator.illustrate(twoItemMagazine()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not reachable");
    }

    @Test
    void skipsTheAvailabilityCheckWhenEveryImageIsAlreadyCached(@TempDir Path releasesDir) throws Exception {
        MagazineStore store = storeIn(releasesDir);
        Path imagesDir = store.releaseDir(RELEASE).resolve("images");
        Files.createDirectories(imagesDir);
        Files.write(imagesDir.resolve("java.png"), "cached".getBytes(StandardCharsets.UTF_8));
        Files.write(imagesDir.resolve("technology.png"), "cached".getBytes(StandardCharsets.UTF_8));

        // The fake engine would throw from checkAvailable() if it were called —
        // proves it wasn't, since every image is already on disk and force=false.
        Magazine result = new ImageGenerator(new FakeEngine(null, false, true), null, store)
                .illustrate(twoItemMagazine());

        assertThat(result.news()).extracting(News::imagePath)
                .containsExactly("images/java.png", "images/technology.png");
    }
}
