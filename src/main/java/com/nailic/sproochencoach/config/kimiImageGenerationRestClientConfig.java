package com.nailic.sproochencoach.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class kimiImageGenerationRestClientConfig {

    @Value("${ai.kimi.api-key}")
    private String apiKey;

    @Value("${ai.kimi.base-image-url}")
    private String baseUrl;

    @Value("${ai.kimi.anthropic-version}")
    private String anthropicVersion;

    @Bean("kimiImageGenerationRestClient")
    public RestClient anthropicRestClient() {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }
}
