package com.nailic.sproochencoach.service;

public record EmailAttachment(
        String filename,
        String contentType,
        String base64Content
) {
}
