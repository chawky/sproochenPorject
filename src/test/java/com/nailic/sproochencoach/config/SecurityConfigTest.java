package com.nailic.sproochencoach.config;

import com.nailic.sproochencoach.service.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SecurityConfigTest {
    @Test
    void corsAllowsOnlyConfiguredOrigins() {
        SecurityConfig securityConfig = new SecurityConfig(mock(JwtAuthenticationFilter.class));
        ReflectionTestUtils.setField(securityConfig, "allowedOrigins", List.of("http://localhost:4200"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me");

        CorsConfiguration configuration = securityConfig.corsConfigurationSource()
                .getCorsConfiguration(request);

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedOrigins()).containsExactly("http://localhost:4200");
        assertThat(configuration.getAllowedOriginPatterns()).isNull();
        assertThat(configuration.getAllowedMethods()).containsExactly("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        assertThat(configuration.getAllowedHeaders()).containsExactly("Authorization", "Content-Type", "X-XSRF-TOKEN");
        assertThat(configuration.getAllowCredentials()).isTrue();
    }

    @Test
    void corsFiltersBlankOrigins() {
        SecurityConfig securityConfig = new SecurityConfig(mock(JwtAuthenticationFilter.class));
        ReflectionTestUtils.setField(securityConfig, "allowedOrigins", List.of(""));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me");

        CorsConfiguration configuration = securityConfig.corsConfigurationSource()
                .getCorsConfiguration(request);

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedOrigins()).isEmpty();
    }

    @Test
    void spaCsrfHandlerAcceptsRawHeaderToken() {
        SecurityConfig.SpaCsrfTokenRequestHandler handler = new SecurityConfig.SpaCsrfTokenRequestHandler();
        DefaultCsrfToken csrfToken = new DefaultCsrfToken("X-XSRF-TOKEN", "_csrf", "raw-token");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/users/logout");
        request.addHeader(csrfToken.getHeaderName(), csrfToken.getToken());

        assertThat(handler.resolveCsrfTokenValue(request, csrfToken)).isEqualTo("raw-token");
    }
}
