package com.nailic.sproochencoach.service;

public interface EmailSender {
    void send(String to, String subject, String text);
}
