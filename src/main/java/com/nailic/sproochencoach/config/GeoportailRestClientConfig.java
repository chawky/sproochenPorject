package com.nailic.sproochencoach.config;

import com.nailic.sproochencoach.constants.AppConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

@Configuration
public class GeoportailRestClientConfig {

    @Value(AppConstants.PropertyPlaceholders.LUXEMBOURG_GEOPORTAIL_BASE_URL)
    private String baseUrl;

    @Bean(AppConstants.RestClientBeans.GEOPORTAIL)
    public RestClient geoportailRestClient(OutboundApiCallLoggingInterceptorFactory loggingInterceptorFactory) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.USER_AGENT, "SproochenCoach")
                .requestInterceptor(loggingInterceptorFactory.create(AppConstants.Providers.GEOPORTAIL))
                .build();
    }
}
