package com.boaglio.boteco.das.ias.image;

import com.boaglio.boteco.das.ias.model.Magazine;
import com.boaglio.boteco.das.ias.model.News;
import com.boaglio.boteco.das.ias.model.Subject;
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
 * {@code images/} subdirectory of the release. A single item's render failing
 * is logged and skipped so the rest of the edition can still be produced —
 * but if the engine's backing service isn't reachable at all, {@link #illustrate}
 * aborts once up front (see {@link ImageEngine#checkAvailable()}) instead of
 * letting every item fail the same way one by one.
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
        return illustrate(magazine, false);
    }

    /**
     * Returns a copy of the magazine with an image path attached to each news
     * item. When {@code force} is false, items whose image already exists are
     * skipped; when true, every image is regenerated.
     */
    public Magazine illustrate(Magazine magazine, boolean force) {
        var imagesDir = store.releaseDir(magazine.releaseDate()).resolve(IMAGES_SUBDIR);
        try {
            Files.createDirectories(imagesDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create images directory " + imagesDir, e);
        }
        // Only probe the engine when there's actually something to render — a
        // fully-cached re-run (everything already illustrated) shouldn't fail
        // just because the image service happens to be down right now.
        var needsGeneration = force || magazine.news().stream()
                .anyMatch(news -> !Files.exists(imagesDir.resolve(filenameFor(news))));
        if (needsGeneration) {
            engine.checkAvailable();
        }
        var illustrated = new ArrayList<News>();
        for (var news : magazine.news()) {
            illustrated.add(illustrate(news, imagesDir, force));
        }
        return new Magazine(magazine.title(), magazine.releaseDate(), illustrated);
    }

    private static String filenameFor(News news) {
        // An edition may hold several custom items, so each gets a name derived
        // from its link/title (stable across runs) instead of just the subject.
        if (news.subject() == Subject.CUSTOM) {
            var key = news.url() != null && !news.url().isBlank() ? news.url() : news.title();
            return "custom-%08x.png".formatted(key.hashCode());
        }
        return news.subject().name().toLowerCase(Locale.ROOT) + ".png";
    }

    private News illustrate(News news, Path imagesDir, boolean force) {
        var filename = filenameFor(news);
        var relativePath = IMAGES_SUBDIR + "/" + filename;
        if (!force && Files.exists(imagesDir.resolve(filename))) {
            log.info("{}: image {} already exists, skipping", news.subject(), relativePath);
            return news.withImagePath(relativePath);
        }
        try {
            var png = engine.generate(scenePrompt(news));
            Files.write(imagesDir.resolve(filename), png);
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
