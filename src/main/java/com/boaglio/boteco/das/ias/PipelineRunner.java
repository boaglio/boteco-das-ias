package com.boaglio.boteco.das.ias;

import com.boaglio.boteco.das.ias.model.Magazine;
import com.boaglio.boteco.das.ias.news.NewsGatherer;
import com.boaglio.boteco.das.ias.storage.MagazineStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * Drives the build process from the command line, one stage per invocation:
 * <pre>
 *   java -jar boteco-das-ias.jar gather   # stage 1: gather news -> magazine.json
 * </pre>
 * With no stage argument the app just starts and stays idle (so it can be run
 * as a service or have stages invoked programmatically).
 */
@Component
public class PipelineRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PipelineRunner.class);

    private final NewsGatherer newsGatherer;
    private final MagazineStore magazineStore;

    public PipelineRunner(NewsGatherer newsGatherer, MagazineStore magazineStore) {
        this.newsGatherer = newsGatherer;
        this.magazineStore = magazineStore;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (args.getNonOptionArgs().contains("gather")) {
            gather();
        }
    }

    private void gather() {
        log.info("Stage 1: gathering news from official feeds…");
        Magazine magazine = newsGatherer.gather();
        Path jsonPath = magazineStore.save(magazine);
        log.info("Gathered {} news item(s) into {}", magazine.news().size(), jsonPath);
    }
}
