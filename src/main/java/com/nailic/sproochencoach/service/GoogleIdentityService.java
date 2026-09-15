package com.nailic.sproochencoach.service;

import com.google.api.client.json.webtoken.JsonWebSignature;
import com.google.auth.oauth2.TokenVerifier;
import com.nailic.sproochencoach.constants.AppConstants;
import com.nailic.sproochencoach.exceptions.InvalidGoogleTokenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Set;

@Service
public class GoogleIdentityService {
    private static final Logger log = LoggerFactory.getLogger(GoogleIdentityService.class);
    private static final Set<String> GOOGLE_ISSUERS = Set.of("accounts.google.com", "https://accounts.google.com");

    @Value(AppConstants.PropertyPlaceholders.SECURITY_GOOGLE_CLIENT_ID)
    private String googleClientId;

    public VerifiedGoogleAccount verify(String idToken) {
        if (!StringUtils.hasText(googleClientId)) {
            throw new IllegalStateException("Google client ID is not configured");
        }

        try {
            JsonWebSignature signature = TokenVerifier.newBuilder()
                    .setAudience(googleClientId)
                    .build()
                    .verify(idToken);
            JsonWebSignature.Payload payload = signature.getPayload();

            if (!GOOGLE_ISSUERS.contains(payload.getIssuer())) {
                log.warn("Google token rejected because issuer is invalid");
                throw new InvalidGoogleTokenException("Invalid Google token");
            }

            String subject = payload.getSubject();
            String email = claimString(payload, "email");
            if (!StringUtils.hasText(subject) || !StringUtils.hasText(email)) {
                log.warn("Google token rejected because required claims are missing");
                throw new InvalidGoogleTokenException("Invalid Google token");
            }

            return new VerifiedGoogleAccount(
                    subject,
                    email.trim().toLowerCase(Locale.ROOT),
                    claimBoolean(payload, "email_verified"),
                    claimString(payload, "given_name"),
                    claimString(payload, "family_name")
            );
        } catch (TokenVerifier.VerificationException exception) {
            log.warn("Google token verification failed");
            throw new InvalidGoogleTokenException("Invalid Google token", exception);
        }
    }

    private String claimString(JsonWebSignature.Payload payload, String claim) {
        Object value = payload.get(claim);
        return value == null ? null : value.toString();
    }

    private boolean claimBoolean(JsonWebSignature.Payload payload, String claim) {
        Object value = payload.get(claim);
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return Boolean.parseBoolean(value == null ? null : value.toString());
    }
}
