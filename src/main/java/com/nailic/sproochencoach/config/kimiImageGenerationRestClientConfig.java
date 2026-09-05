package com.nailic.sproochencoach.config;

import com.nailic.sproochencoach.constants.AppConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

@Configuration
public class kimiImageGenerationRestClientConfig {

    @Value(AppConstants.PropertyPlaceholders.AI_KIMI_API_KEY)
    private String apiKey;

    @Value(AppConstants.PropertyPlaceholders.AI_KIMI_BASE_IMAGE_URL)
    private String baseUrl;

    @Value(AppConstants.PropertyPlaceholders.AI_KIMI_ANTHROPIC_VERSION)
    private String anthropicVersion;

    @Bean(AppConstants.RestClientBeans.KIMI_IMAGE_GENERATION)
    public RestClient anthropicRestClient(OutboundApiCallLoggingInterceptorFactory loggingInterceptorFactory) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, AppConstants.Http.BEARER_PREFIX + apiKey)
                .requestInterceptor(loggingInterceptorFactory.create(AppConstants.Providers.KIMI_IMAGE))
                .build();
    }
}
