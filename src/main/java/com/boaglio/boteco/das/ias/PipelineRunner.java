package com.boaglio.boteco.das.ias;

import com.boaglio.boteco.das.ias.model.Magazine;
import com.boaglio.boteco.das.ias.news.NewsGatherer;
import com.boaglio.boteco.das.ias.opinion.OpinionCollector;
import com.boaglio.boteco.das.ias.storage.MagazineStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/**
 * Drives the build process from the command line, one stage per invocation:
 * <pre>
 *   java -jar boteco-das-ias.jar gather    # stage 1: gather news -> magazine.json
 *   java -jar boteco-das-ias.jar collect   # stage 2: collect opinions into magazine.json
 * </pre>
 * With no stage argument the app just starts and stays idle (so it can be run
 * as a service or have stages invoked programmatically).
 */
@Component
public class PipelineRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PipelineRunner.class);

    private final NewsGatherer newsGatherer;
    private final OpinionCollector opinionCollector;
    private final MagazineStore magazineStore;

    public PipelineRunner(NewsGatherer newsGatherer, OpinionCollector opinionCollector,
                          MagazineStore magazineStore) {
        this.newsGatherer = newsGatherer;
        this.opinionCollector = opinionCollector;
        this.magazineStore = magazineStore;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> stages = args.getNonOptionArgs();
        if (stages.contains("gather")) {
            gather();
        }
        if (stages.contains("collect")) {
            collect();
        }
    }

    private void gather() {
        log.info("Stage 1: gathering news from official feeds…");
        Magazine magazine = newsGatherer.gather();
        Path jsonPath = magazineStore.save(magazine);
        log.info("Gathered {} news item(s) into {}", magazine.news().size(), jsonPath);
    }

    private void collect() {
        log.info("Stage 2: collecting opinions from the reviewers…");
        Magazine magazine = magazineStore.load(LocalDate.now());
        Magazine reviewed = opinionCollector.collect(magazine);
        Path jsonPath = magazineStore.save(reviewed);
        log.info("Collected opinions into {}", jsonPath);
    }
}
