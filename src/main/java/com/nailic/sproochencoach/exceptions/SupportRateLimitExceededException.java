package com.nailic.sproochencoach.exceptions;

public class SupportRateLimitExceededException extends RuntimeException {
    public SupportRateLimitExceededException(String message) {
        super(message);
    }
}
