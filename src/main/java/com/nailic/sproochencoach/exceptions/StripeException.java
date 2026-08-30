package com.nailic.sproochencoach.exceptions;

import lombok.Getter;

@Getter
public class StripeException extends RuntimeException {

    private final int statusCode;

    public StripeException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }
}
