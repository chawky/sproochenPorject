package com.nailic.sproochencoach.exceptions;

public class LoginRateLimitExceededException extends RuntimeException {
    public LoginRateLimitExceededException(String message) {
        super(message);
    }
}
