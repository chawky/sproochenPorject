package com.nailic.sproochencoach.exceptions;

import org.springframework.security.core.AuthenticationException;

public class InvalidGoogleTokenException extends AuthenticationException {
    public InvalidGoogleTokenException(String message) {
        super(message);
    }

    public InvalidGoogleTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
