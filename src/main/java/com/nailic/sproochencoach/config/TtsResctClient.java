package com.nailic.sproochencoach.config;

import com.nailic.sproochencoach.constants.AppConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class TtsResctClient {
    @Value(AppConstants.PropertyPlaceholders.AI_ELEVENLABS_API_KEY)
    private String apiKey;

    @Value(AppConstants.PropertyPlaceholders.AI_ELEVENLABS_BASE_URL)
    private String baseUrl;
    @Bean(AppConstants.RestClientBeans.TTS)
    public RestClient ttsRestClient(OutboundApiCallLoggingInterceptorFactory loggingInterceptorFactory) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(AppConstants.Http.XI_API_KEY_HEADER, apiKey)
                .requestInterceptor(loggingInterceptorFactory.create(AppConstants.Providers.ELEVENLABS))
                .build();
    }
}
