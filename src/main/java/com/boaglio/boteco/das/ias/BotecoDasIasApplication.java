package com.boaglio.boteco.das.ias;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for Boteco das IAs — builds a weekly HTML info-magazine from
 * official tech news, gathers opinions from local AIs (Claude CLI + Ollama),
 * generates anime-style images with ComfyUI, and renders the final release.
 */
@SpringBootApplication
public class BotecoDasIasApplication {

    public static void main(String[] args) {
        SpringApplication.run(BotecoDasIasApplication.class, args);
    }
}
