package com.nailic.sproochencoach.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

@Configuration
public class GeoportailRestClientConfig {

    @Value("${luxembourg.geoportail.base-url}")
    private String baseUrl;

    @Bean("geoportailRestClient")
    public RestClient geoportailRestClient() {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.USER_AGENT, "SproochenCoach")
                .build();
    }
}
