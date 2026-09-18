package com.nailic.sproochencoach.service;

import java.util.List;

public interface EmailSender {
    void send(String to, String subject, String text);

    void send(String to, String subject, String text, String replyTo, List<EmailAttachment> attachments);
}
