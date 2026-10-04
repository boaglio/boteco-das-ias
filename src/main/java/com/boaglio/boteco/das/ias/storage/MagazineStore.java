package com.boaglio.boteco.das.ias.storage;

import com.boaglio.boteco.das.ias.config.BotecoProperties;
import com.boaglio.boteco.das.ias.model.Magazine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

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
        } catch (JacksonException e) {
            throw new UncheckedIOException("Failed to save magazine to " + jsonPath, new IOException(e));
        }
    }

    /** Whether a working magazine already exists for the given release date. */
    public boolean exists(LocalDate releaseDate) {
        return Files.exists(releaseDir(releaseDate).resolve(JSON_FILE));
    }

    /**
     * The edition's sequence number (#1 for the oldest): one more than the
     * number of editions saved before {@code releaseDate}, so it's right
     * whether or not this edition is already on disk, and follows a re-dated one.
     */
    public int editionNumber(LocalDate releaseDate) {
        var releasesDir = Path.of(properties.releasesDir());
        if (!Files.isDirectory(releasesDir)) {
            return 1;
        }
        try (var dirs = Files.list(releasesDir)) {
            var earlier = dirs
                    .filter(dir -> Files.exists(dir.resolve(JSON_FILE)))
                    .map(dir -> dir.getFileName().toString())
                    .filter(name -> name.matches("\\d{4}-\\d{2}-\\d{2}"))
                    .filter(name -> name.compareTo(releaseDate.toString()) < 0)
                    .count();
            return (int) earlier + 1;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to list editions in " + releasesDir, e);
        }
    }

    /** Loads the working magazine for the given release date. */
    public Magazine load(LocalDate releaseDate) {
        Path jsonPath = releaseDir(releaseDate).resolve(JSON_FILE);
        try {
            return objectMapper.readValue(jsonPath.toFile(), Magazine.class);
        } catch (JacksonException e) {
            throw new UncheckedIOException("Failed to load magazine from " + jsonPath, new IOException(e));
        }
    }
}
