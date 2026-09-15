package com.nailic.sproochencoach.exceptions;

public class AccountLinkingRequiredException extends RuntimeException {
    public AccountLinkingRequiredException(String message) {
        super(message);
    }
}
