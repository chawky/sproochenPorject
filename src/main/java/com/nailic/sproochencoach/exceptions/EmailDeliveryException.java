package com.nailic.sproochencoach.exceptions;

public class EmailDeliveryException extends RuntimeException {
    private final int statusCode;

    public EmailDeliveryException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public EmailDeliveryException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
