package com.boaglio.boteco.das.ias.image;

import com.boaglio.boteco.das.ias.model.Magazine;
import com.boaglio.boteco.das.ias.model.News;
import com.boaglio.boteco.das.ias.storage.MagazineStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Stage 3 of the build process: render an anime-style image for each news item
 * and attach its release-relative path. Images are written to an
 * {@code images/} subdirectory of the release. A failing render is logged and
 * skipped so the rest of the edition can still be produced.
 */
@Service
public class ImageGenerator {

    private static final Logger log = LoggerFactory.getLogger(ImageGenerator.class);
    private static final String IMAGES_SUBDIR = "images";

    private final ImageEngine engine;
    private final SceneDescriber sceneDescriber;
    private final MagazineStore store;

    public ImageGenerator(ImageEngine engine, SceneDescriber sceneDescriber, MagazineStore store) {
        this.engine = engine;
        this.sceneDescriber = sceneDescriber;
        this.store = store;
    }

    /** Returns a copy of the magazine with an image path attached to each news item. */
    public Magazine illustrate(Magazine magazine) {
        var imagesDir = store.releaseDir(magazine.releaseDate()).resolve(IMAGES_SUBDIR);
        try {
            Files.createDirectories(imagesDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create images directory " + imagesDir, e);
        }
        var illustrated = new ArrayList<News>();
        for (var news : magazine.news()) {
            illustrated.add(illustrate(news, imagesDir));
        }
        return new Magazine(magazine.title(), magazine.releaseDate(), illustrated);
    }

    private News illustrate(News news, Path imagesDir) {
        try {
            var png = engine.generate(scenePrompt(news));
            var filename = news.subject().name().toLowerCase(Locale.ROOT) + ".png";
            Files.write(imagesDir.resolve(filename), png);
            var relativePath = IMAGES_SUBDIR + "/" + filename;
            log.info("{}: generated image {}", news.subject(), relativePath);
            return news.withImagePath(relativePath);
        } catch (Exception e) {
            log.warn("{}: image generation failed: {}", news.subject(), e.getMessage());
            return news;
        }
    }

    /**
     * The visual scene fed to the image model: an AI-described, text-free scene
     * when available, otherwise the subject-themed fallback. Either way the
     * article's headline/summary text is never passed to the image model.
     */
    private String scenePrompt(News news) {
        if (sceneDescriber != null) {
            try {
                var scene = sceneDescriber.describe(news);
                if (scene != null && !scene.isBlank()) {
                    return scene;
                }
            } catch (Exception e) {
                log.warn("{}: scene description failed, using fallback: {}",
                        news.subject(), e.getMessage());
            }
        }
        return ImagePrompts.forNews(news);
    }
}
