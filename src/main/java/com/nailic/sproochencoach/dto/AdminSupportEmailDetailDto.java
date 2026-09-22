package com.nailic.sproochencoach.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record AdminSupportEmailDetailDto(
        Long id,
        String fromEmail,
        String toEmail,
        String subject,
        String textBody,
        @Schema(description = "Raw untrusted inbound email HTML. Admin UI must sanitize or sandbox before rendering.")
        String htmlBody,
        LocalDateTime receivedAt,
        boolean read
) {
}
