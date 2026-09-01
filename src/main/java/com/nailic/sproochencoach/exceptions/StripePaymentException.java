package com.nailic.sproochencoach.exceptions;

import lombok.Getter;

@Getter
public class StripePaymentException extends RuntimeException {

    private final int statusCode;

    public StripePaymentException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }
}
