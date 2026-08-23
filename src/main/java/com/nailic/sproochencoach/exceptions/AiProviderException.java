package com.nailic.sproochencoach.exceptions;

import lombok.Getter;

@Getter
public class AiProviderException extends RuntimeException {

    private final int statusCode;

    public AiProviderException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }
}
