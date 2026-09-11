package com.nailic.sproochencoach.dto;

public record ResendEmailRequest(
        String from,
        String to,
        String subject,
        String text
) {
}
