package com.nailic.sproochencoach.exceptions;

public class OtpRateLimitExceededException extends RuntimeException {
    public OtpRateLimitExceededException(String message) {
        super(message);
    }
}
