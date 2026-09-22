package com.nailic.sproochencoach.dto;

public record AdminSupportEmailAttachmentDto(
        Long id,
        String filename,
        String contentType,
        String contentDisposition,
        String contentId,
        Long sizeBytes
) {
}
