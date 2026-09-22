package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.constants.AppConstants;
import com.nailic.sproochencoach.exceptions.BadRequestException;
import com.svix.Webhook;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ResendWebhookVerifier {
    private static final Logger log = LoggerFactory.getLogger(ResendWebhookVerifier.class);

    @Value(AppConstants.PropertyPlaceholders.RESEND_WEBHOOK_SECRET)
    private String webhookSecret;

    public void verify(String payload, HttpHeaders headers) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new IllegalStateException("Resend webhook secret is not configured");
        }

        try {
            Webhook webhook = new Webhook(webhookSecret);
            webhook.verify(payload, java.net.http.HttpHeaders.of(headerMap(headers), (name, value) -> true));
        } catch (Exception exception) {
            log.warn("Invalid Resend webhook signature. message={}", exception.getMessage());
            throw new BadRequestException("Invalid webhook signature");
        }
    }

    private Map<String, List<String>> headerMap(HttpHeaders headers) {
        return Map.of(
                "svix-id", value(headers, "svix-id"),
                "svix-timestamp", value(headers, "svix-timestamp"),
                "svix-signature", value(headers, "svix-signature")
        );
    }

    private List<String> value(HttpHeaders headers, String name) {
        List<String> values = headers.get(name);
        return values == null ? List.of() : values;
    }
}
