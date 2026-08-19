package com.nailic.sproochencoach.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
@Configuration
public class OpenRouterRestClientConfig {

    @Value("${ai.openrouter.api-key}")
    private String apiKey;

    @Value("${ai.openrouter.base-url}")
    private String baseUrl;

    @Bean("openRouterRestClient")
    public RestClient openRouterRestClient() {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }
}
