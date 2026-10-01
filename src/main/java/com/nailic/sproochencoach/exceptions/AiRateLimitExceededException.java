package com.nailic.sproochencoach.exceptions;

public class AiRateLimitExceededException extends RuntimeException {
    public AiRateLimitExceededException(String message) {
        super(message);
    }
}
