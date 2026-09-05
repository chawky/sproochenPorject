package com.nailic.sproochencoach.config;

import com.nailic.sproochencoach.constants.AppConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

@Configuration
public class OpenRouterRestClientConfig {

    @Value(AppConstants.PropertyPlaceholders.AI_OPENROUTER_API_KEY)
    private String apiKey;

    @Value(AppConstants.PropertyPlaceholders.AI_OPENROUTER_BASE_URL)
    private String baseUrl;

    @Bean(AppConstants.RestClientBeans.OPEN_ROUTER)
    public RestClient openRouterRestClient(OutboundApiCallLoggingInterceptorFactory loggingInterceptorFactory) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, AppConstants.Http.BEARER_PREFIX + apiKey)
                .requestInterceptor(loggingInterceptorFactory.create(AppConstants.Providers.OPEN_ROUTER))
                .build();
    }
}
