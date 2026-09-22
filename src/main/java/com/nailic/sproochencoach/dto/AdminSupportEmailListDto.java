package com.nailic.sproochencoach.dto;

import java.time.LocalDateTime;

public record AdminSupportEmailListDto(
        Long id,
        String fromEmail,
        String subject,
        LocalDateTime receivedAt,
        boolean read
) {
}
