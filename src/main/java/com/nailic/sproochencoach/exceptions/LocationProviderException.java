package com.nailic.sproochencoach.exceptions;

import lombok.Getter;

@Getter
public class LocationProviderException extends RuntimeException {

    private final int statusCode;

    public LocationProviderException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }
}
