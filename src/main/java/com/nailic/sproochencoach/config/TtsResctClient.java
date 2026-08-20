package com.nailic.sproochencoach.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class TtsResctClient {
    @Value("${ai.elevenlabs.api-key}")
    private String apiKey;

    @Value("${ai.elevenlabs.base-url}")
    private String baseUrl;
    @Bean("ttsRestClient")
    public RestClient ttsRestClient() {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("xi-api-key", apiKey)
                .build();
    }
}
