package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.model.AppUser;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {
    private static final String SECRET =
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @Test
    void generatedTokenIsValidForCurrentTokenVersion() {
        JwtService jwtService = jwtService();
        AppUser user = enabledUser();

        String jwt = jwtService.generateToken(user);

        assertThat(jwtService.isTokenValid(jwt, user.getEmail(), user)).isTrue();
    }

    @Test
    void tokenBecomesInvalidAfterTokenVersionChanges() {
        JwtService jwtService = jwtService();
        AppUser user = enabledUser();
        String jwt = jwtService.generateToken(user);

        user.setTokenVersion(user.getTokenVersion() + 1);

        assertThat(jwtService.isTokenValid(jwt, user.getEmail(), user)).isFalse();
    }

    private JwtService jwtService() {
        JwtService jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3_600_000L);
        return jwtService;
    }

    private AppUser enabledUser() {
        AppUser user = new AppUser();
        user.setId(42);
        user.setEmail("learner@example.com");
        user.setEnabled(true);
        return user;
    }
}
