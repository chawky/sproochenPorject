package com.nailic.sproochencoach.dto;

public record AdminSupportEmailSyncResultDto(
        int imported,
        int existing,
        int ignored,
        int attachmentsAdded,
        int failed
) {
}
