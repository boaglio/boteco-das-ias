package com.boaglio.boteco.das.ias;

import com.boaglio.boteco.das.ias.image.ImageGenerator;
import com.boaglio.boteco.das.ias.news.NewsGatherer;
import com.boaglio.boteco.das.ias.opinion.OpinionCollector;
import com.boaglio.boteco.das.ias.render.MagazineRenderer;
import com.boaglio.boteco.das.ias.storage.MagazineStore;
import com.boaglio.boteco.das.ias.translate.NewsTranslator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Drives the build process from the command line, one stage per invocation:
 * <pre>
 *   java -jar boteco-das-ias.jar gather     # stage 1: gather news -> magazine.json
 *   java -jar boteco-das-ias.jar translate  # stage 2: translate news to pt-BR
 *   java -jar boteco-das-ias.jar collect    # stage 3: collect opinions into magazine.json
 *   java -jar boteco-das-ias.jar illustrate # stage 4: generate images into magazine.json
 *   java -jar boteco-das-ias.jar render     # stage 5: render magazine.html from magazine.json
 * </pre>
 * By default each stage reuses work already done for today's edition, so a
 * retry only fills in what failed previously. Pass {@code --force} to redo
 * everything (re-crawl, re-translate, re-collect opinions, regenerate images).
 * With no stage argument the app just starts and stays idle (so it can be run
 * as a service or have stages invoked programmatically).
 */
@Component
public class PipelineRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PipelineRunner.class);

    private final NewsGatherer newsGatherer;
    private final NewsTranslator newsTranslator;
    private final OpinionCollector opinionCollector;
    private final ImageGenerator imageGenerator;
    private final MagazineRenderer magazineRenderer;
    private final MagazineStore magazineStore;

    public PipelineRunner(NewsGatherer newsGatherer, NewsTranslator newsTranslator,
                          OpinionCollector opinionCollector, ImageGenerator imageGenerator,
                          MagazineRenderer magazineRenderer, MagazineStore magazineStore) {
        this.newsGatherer = newsGatherer;
        this.newsTranslator = newsTranslator;
        this.opinionCollector = opinionCollector;
        this.imageGenerator = imageGenerator;
        this.magazineRenderer = magazineRenderer;
        this.magazineStore = magazineStore;
    }

    @Override
    public void run(ApplicationArguments args) {
        var stages = args.getNonOptionArgs();
        var force = args.containsOption("force");
        if (force) {
            log.info("--force enabled: redoing every stage from scratch");
        }
        if (stages.contains("gather")) {
            gather(force);
        }
        if (stages.contains("translate")) {
            translate(force);
        }
        if (stages.contains("collect")) {
            collect(force);
        }
        if (stages.contains("illustrate")) {
            illustrate(force);
        }
        if (stages.contains("render")) {
            render();
        }
    }

    private void gather(boolean force) {
        log.info("Stage 1: gathering news from official feeds…");
        var today = LocalDate.now();
        var existing = !force && magazineStore.exists(today) ? magazineStore.load(today) : null;
        var magazine = newsGatherer.gather(existing);
        var jsonPath = magazineStore.save(magazine);
        log.info("Gathered {} news item(s) into {}", magazine.news().size(), jsonPath);
    }

    private void translate(boolean force) {
        log.info("Stage 2: translating news to Brazilian Portuguese…");
        var magazine = magazineStore.load(LocalDate.now());
        var translated = newsTranslator.translate(magazine, force);
        var jsonPath = magazineStore.save(translated);
        log.info("Translated {} news item(s) into {}", translated.news().size(), jsonPath);
    }

    private void collect(boolean force) {
        log.info("Stage 3: collecting opinions from the reviewers…");
        var magazine = magazineStore.load(LocalDate.now());
        var reviewed = opinionCollector.collect(magazine, force);
        var jsonPath = magazineStore.save(reviewed);
        log.info("Collected opinions into {}", jsonPath);
    }

    private void illustrate(boolean force) {
        log.info("Stage 4: generating anime-style images…");
        var magazine = magazineStore.load(LocalDate.now());
        var illustrated = imageGenerator.illustrate(magazine, force);
        var jsonPath = magazineStore.save(illustrated);
        log.info("Generated images for {} news item(s) into {}", illustrated.news().size(), jsonPath);
    }

    private void render() {
        log.info("Stage 5: rendering the final HTML magazine…");
        var magazine = magazineStore.load(LocalDate.now());
        var htmlPath = magazineRenderer.renderToFile(magazine);
        log.info("Rendered final magazine to {}", htmlPath);
    }
}
