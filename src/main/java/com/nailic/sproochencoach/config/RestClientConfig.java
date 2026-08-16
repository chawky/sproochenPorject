package com.nailic.sproochencoach.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Value("${ai.openrouter.base-url}")
    private String baseUrl;
    @Value("${ai.openrouter.api-key}")
    private String key;
    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                .defaultHeader("Authorization","Bearer "+key)
                .baseUrl(baseUrl)
                .build();
    }
}
