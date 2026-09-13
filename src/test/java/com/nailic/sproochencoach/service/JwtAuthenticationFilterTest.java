package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.constants.AppConstants;
import com.nailic.sproochencoach.model.AppUser;
import jakarta.servlet.http.Cookie;
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
        SecurityContextHolder.clearContext();
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

    @Test
    void validCookieJwtAuthenticatesUser() {
        SecurityContextHolder.clearContext();
        JwtService jwtService = mock(JwtService.class);
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        AppUser user = new AppUser();
        user.setId(42);
        user.setEmail("learner@example.com");

        when(jwtService.extractUsername("cookie-token")).thenReturn("learner@example.com");
        when(userDetailsService.loadUserByUsername("learner@example.com")).thenReturn(user);
        when(jwtService.isTokenValid("cookie-token", "learner@example.com", user)).thenReturn(true);

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                jwtService,
                userDetailsService
        );
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me");
        request.setCookies(new Cookie(AppConstants.Http.ACCESS_TOKEN_COOKIE, "cookie-token"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        assertThatCode(() -> filter.doFilter(request, response, filterChain))
                .doesNotThrowAnyException();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(user);

        SecurityContextHolder.clearContext();
    }
}
