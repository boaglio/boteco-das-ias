package com.boaglio.boteco.das.ias.image;

/**
 * One image engine — turns a scene description into a rendered PNG. The only
 * implementation today is {@link ComfyUiImageEngine}, but the interface keeps
 * {@link ImageGenerator} testable without a running ComfyUI server.
 */
public interface ImageEngine {

    /**
     * Renders an image for the given scene prompt and returns the raw PNG bytes.
     * The engine is responsible for applying any house style (see the configured
     * {@code boteco.comfyui.style-prompt}).
     *
     * @throws Exception if the underlying server is unavailable or fails to render
     */
    byte[] generate(String scenePrompt) throws Exception;

    /**
     * Fails fast if the engine's backing service isn't reachable. Called once
     * before a batch of {@link #generate} calls, so a service that's simply not
     * running (e.g. {@code docker compose up} was never run) aborts the whole
     * stage with one clear message instead of every item failing individually.
     *
     * @throws RuntimeException describing why the service can't be reached
     */
    default void checkAvailable() {
    }
}
