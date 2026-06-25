package com.boaglio.boteco.das.ias.image;

import com.boaglio.boteco.das.ias.config.BotecoProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Generates anime-style images on a local ComfyUI server through its HTTP API.
 * A text-to-image workflow template ({@code comfyui-workflow.json}) is loaded
 * once, the news scene plus the configured house style are injected, the job is
 * queued via {@code /prompt}, polled via {@code /history/{id}}, and the rendered
 * PNG is fetched from {@code /view}.
 *
 * <p>Node ids in the template are fixed: {@code 6} is the positive prompt,
 * {@code 7} the negative, {@code 4} the checkpoint loader, {@code 5} the latent
 * size, {@code 3} the sampler, and {@code 9} the SaveImage output.
 */
@Component
public class ComfyUiImageEngine implements ImageEngine {

    private static final Logger log = LoggerFactory.getLogger(ComfyUiImageEngine.class);
    private static final String WORKFLOW_RESOURCE = "comfyui-workflow.json";
    private static final Duration POLL_INTERVAL = Duration.ofSeconds(2);
    private static final Duration DEFAULT_RENDER_TIMEOUT = Duration.ofMinutes(20);

    private final BotecoProperties.ComfyUi config;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final JsonNode workflowTemplate;
    private final Duration renderTimeout;

    public ComfyUiImageEngine(BotecoProperties properties, ObjectMapper objectMapper) {
        this.config = properties.comfyui();
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().baseUrl(config.baseUrl()).build();
        this.workflowTemplate = loadTemplate(objectMapper);
        this.renderTimeout = config.renderTimeout() == null ? DEFAULT_RENDER_TIMEOUT : config.renderTimeout();
    }

    private static JsonNode loadTemplate(ObjectMapper mapper) {
        try (var in = new ClassPathResource(WORKFLOW_RESOURCE).getInputStream()) {
            return mapper.readTree(in);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load ComfyUI workflow template", e);
        }
    }

    @Override
    public byte[] generate(String scenePrompt) throws InterruptedException {
        ObjectNode graph = workflowTemplate.deepCopy();
        applyInputs(graph, scenePrompt);
        var promptId = queue(graph);
        var image = awaitImage(promptId);
        return fetch(image);
    }

    private void applyInputs(ObjectNode graph, String scenePrompt) {
        inputs(graph, "6").put("text", config.stylePrompt() + ", " + scenePrompt);
        inputs(graph, "7").put("text", config.negativePrompt());
        inputs(graph, "4").put("ckpt_name", config.checkpoint());
        inputs(graph, "5").put("width", config.width());
        inputs(graph, "5").put("height", config.height());
        inputs(graph, "3").put("steps", config.steps());
        inputs(graph, "3").put("seed", ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE));
    }

    private static ObjectNode inputs(ObjectNode graph, String nodeId) {
        return (ObjectNode) graph.path(nodeId).path("inputs");
    }

    private String queue(ObjectNode graph) {
        var body = objectMapper.createObjectNode();
        body.set("prompt", graph);
        body.put("client_id", UUID.randomUUID().toString());

        var response = restClient.post().uri("/prompt")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        var promptId = response == null ? null : response.path("prompt_id").asText(null);
        if (promptId == null || promptId.isBlank()) {
            throw new IllegalStateException("ComfyUI did not return a prompt_id: " + response);
        }
        return promptId;
    }

    private JsonNode awaitImage(String promptId) throws InterruptedException {
        var deadline = Instant.now().plus(renderTimeout);
        while (Instant.now().isBefore(deadline)) {
            var history = restClient.get().uri("/history/{id}", promptId)
                    .retrieve()
                    .body(JsonNode.class);
            var images = history == null ? null
                    : history.path(promptId).path("outputs").path("9").path("images");
            if (images != null && images.isArray() && !images.isEmpty()) {
                return images.get(0);
            }
            Thread.sleep(POLL_INTERVAL.toMillis());
        }
        throw new IllegalStateException(
                "ComfyUI render timed out after " + renderTimeout + " for prompt " + promptId);
    }

    private byte[] fetch(JsonNode image) {
        return restClient.get().uri(uri -> uri.path("/view")
                        .queryParam("filename", image.path("filename").asText())
                        .queryParam("subfolder", image.path("subfolder").asText())
                        .queryParam("type", image.path("type").asText("output"))
                        .build())
                .retrieve()
                .body(byte[].class);
    }
}
