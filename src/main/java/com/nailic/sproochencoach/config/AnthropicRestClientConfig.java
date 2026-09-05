package com.nailic.sproochencoach.config;

import com.nailic.sproochencoach.constants.AppConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AnthropicRestClientConfig {

    @Value(AppConstants.PropertyPlaceholders.AI_KIMI_API_KEY)
    private String apiKey;

    @Value(AppConstants.PropertyPlaceholders.AI_KIMI_BASE_URL)
    private String baseUrl;

    @Value(AppConstants.PropertyPlaceholders.AI_KIMI_ANTHROPIC_VERSION)
    private String anthropicVersion;

    @Bean(AppConstants.RestClientBeans.ANTHROPIC)
    public RestClient anthropicRestClient(OutboundApiCallLoggingInterceptorFactory loggingInterceptorFactory) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(AppConstants.Http.ANTHROPIC_API_KEY_HEADER, apiKey)
                .defaultHeader(AppConstants.Http.ANTHROPIC_VERSION_HEADER, anthropicVersion)
                .requestInterceptor(loggingInterceptorFactory.create(AppConstants.Providers.KIMI))
                .build();
    }
}
