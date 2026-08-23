package com.nailic.sproochencoach.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AnthropicRestClientConfig {

    @Value("${ai.kimi.api-key}")
    private String apiKey;

    @Value("${ai.kimi.base-url}")
    private String baseUrl;

    @Value("${ai.kimi.anthropic-version}")
    private String anthropicVersion;

    @Bean("anthropicRestClient")
    public RestClient anthropicRestClient() {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", anthropicVersion)
                .build();
    }
}
