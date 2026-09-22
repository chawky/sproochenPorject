package com.nailic.sproochencoach.dto;

public record SupportEmailAttachmentDownload(
        String filename,
        String contentType,
        byte[] content
) {
}
