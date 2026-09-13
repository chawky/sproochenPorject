package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.constants.AppConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class JwtCookieService {
    private final JwtService jwtService;
    @Value(AppConstants.PropertyPlaceholders.SECURITY_JWT_COOKIE_SECURE)
    private boolean secure;
    @Value(AppConstants.PropertyPlaceholders.SECURITY_JWT_COOKIE_SAME_SITE)
    private String sameSite;

    public ResponseCookie accessTokenCookie(String jwt) {
        return baseCookie(jwt)
                .maxAge(Duration.ofMillis(jwtService.getJwtExpiration()))
                .build();
    }

    public ResponseCookie clearAccessTokenCookie() {
        return baseCookie("")
                .maxAge(Duration.ZERO)
                .build();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(AppConstants.Http.ACCESS_TOKEN_COOKIE, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/");
    }
}
