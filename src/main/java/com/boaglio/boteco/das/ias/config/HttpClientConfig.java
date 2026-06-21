package com.boaglio.boteco.das.ias.config;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Gives every {@code RestClient} — notably Spring AI's Ollama client — a
 * generous read timeout, so a local model that is slow to load on first use
 * (cold start) doesn't trip the framework's short default timeout. The JDK HTTP
 * client is forced so the read timeout is honored directly, without a
 * reactor-netty {@code ReadTimeoutHandler} firing at its own default.
 */
@Configuration
class HttpClientConfig {

    @Bean
    RestClientCustomizer ollamaReadTimeoutCustomizer() {
        var settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(Duration.ofSeconds(20))
                .withReadTimeout(Duration.ofMinutes(5));
        return builder -> builder.requestFactory(
                ClientHttpRequestFactoryBuilder.jdk().build(settings));
    }
}
