package com.nailic.sproochencoach.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class GroqRestClientConfig {

    @Value("${ai.groq.api-key}")
    private String apiKey;

    @Value("${ai.groq.base-url}")
    private String baseUrl;

    @Bean("groqRestClient")
    public RestClient groqRestClient() {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }
}