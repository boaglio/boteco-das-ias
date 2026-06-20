package com.boaglio.boteco.das.ias.storage;

import com.boaglio.boteco.das.ias.config.BotecoProperties;
import com.boaglio.boteco.das.ias.model.Magazine;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

/**
 * Reads and writes the working {@link Magazine} JSON that every build stage
 * shares. Each edition lives in its own dated directory under the releases dir,
 * e.g. {@code releases/2026-06-20/magazine.json}.
 */
@Component
public class MagazineStore {

    private static final Logger log = LoggerFactory.getLogger(MagazineStore.class);
    private static final String JSON_FILE = "magazine.json";

    private final BotecoProperties properties;
    private final ObjectMapper objectMapper;

    public MagazineStore(BotecoProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /** Directory that holds all artifacts for the given release date. */
    public Path releaseDir(LocalDate releaseDate) {
        return Path.of(properties.releasesDir(), releaseDate.toString());
    }

    /** Serializes the magazine to its release directory and returns the JSON path. */
    public Path save(Magazine magazine) {
        Path dir = releaseDir(magazine.releaseDate());
        Path jsonPath = dir.resolve(JSON_FILE);
        try {
            Files.createDirectories(dir);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(jsonPath.toFile(), magazine);
            log.info("Saved magazine to {}", jsonPath);
            return jsonPath;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save magazine to " + jsonPath, e);
        }
    }

    /** Loads the working magazine for the given release date. */
    public Magazine load(LocalDate releaseDate) {
        Path jsonPath = releaseDir(releaseDate).resolve(JSON_FILE);
        try {
            return objectMapper.readValue(jsonPath.toFile(), Magazine.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load magazine from " + jsonPath, e);
        }
    }
}
