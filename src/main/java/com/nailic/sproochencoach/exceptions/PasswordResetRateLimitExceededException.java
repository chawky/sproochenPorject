package com.nailic.sproochencoach.exceptions;

public class PasswordResetRateLimitExceededException extends RuntimeException {
    public PasswordResetRateLimitExceededException(String message) {
        super(message);
    }
}
