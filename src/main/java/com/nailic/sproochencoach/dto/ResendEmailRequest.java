package com.nailic.sproochencoach.dto;

import java.util.List;

public record ResendEmailRequest(
        String from,
        String to,
        String subject,
        String text,
        String reply_to,
        List<Attachment> attachments
) {
    public ResendEmailRequest(String from, String to, String subject, String text) {
        this(from, to, subject, text, null, null);
    }

    public record Attachment(
            String content,
            String filename,
            String content_type
    ) {
    }
}
