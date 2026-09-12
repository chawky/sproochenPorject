package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.constants.AppConstants;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {
    @Test
    void invalidJwtContinuesUnauthenticated() {
        JwtService jwtService = mock(JwtService.class);
        when(jwtService.extractUsername("invalid-token")).thenThrow(new IllegalArgumentException("bad token"));

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                jwtService,
                mock(CustomUserDetailsService.class)
        );
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me");
        request.addHeader(HttpHeaders.AUTHORIZATION, AppConstants.Http.BEARER_PREFIX + "invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        assertThatCode(() -> filter.doFilter(request, response, filterChain))
                .doesNotThrowAnyException();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        SecurityContextHolder.clearContext();
    }
}
