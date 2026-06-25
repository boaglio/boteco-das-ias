package com.boaglio.boteco.das.ias.config;

import com.boaglio.boteco.das.ias.model.Subject;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * Strongly-typed binding of all {@code boteco.*} settings from application.yml.
 */
@ConfigurationProperties(prefix = "boteco")
public record BotecoProperties(
        String title,
        int newsWindowDays,
        Feeds feeds,
        Reviewers reviewers,
        ComfyUi comfyui,
        String releasesDir,
        String logoFile,
        Author author,
        List<FooterLink> footerLinks
) {

    public BotecoProperties {
        footerLinks = footerLinks == null ? List.of() : List.copyOf(footerLinks);
    }

    /** The human reviewer's display name and avatar image. */
    public record Author(String name, String avatarFile) {
    }

    /** One footer link (label, URL and icon slug), sourced from the author's Linktree. */
    public record FooterLink(String label, String url, String icon) {
    }

    /** Official feed URLs per category. */
    public record Feeds(
            List<String> java,
            List<String> springBoot,
            List<String> springAi,
            List<String> technology
    ) {
        /** Returns the configured feed URLs for the given subject. */
        public List<String> forSubject(Subject subject) {
            List<String> urls = switch (subject) {
                case JAVA -> java;
                case SPRING_BOOT -> springBoot;
                case SPRING_AI -> springAi;
                case TECHNOLOGY -> technology;
            };
            return urls == null ? List.of() : urls;
        }
    }

    /** Opinion-engine settings. */
    public record Reviewers(ClaudeCli claudeCli, Ollama ollama) {
        public record ClaudeCli(String command, String promptFlag) {
        }

        public record Ollama(String phiModel, String llamaModel) {
        }
    }

    /** Local ComfyUI image generation settings. */
    public record ComfyUi(
            String baseUrl,
            String stylePrompt,
            String negativePrompt,
            String checkpoint,
            int width,
            int height,
            int steps,
            Duration renderTimeout
    ) {
    }
}
