package com.nailic.sproochencoach.exceptions;

import lombok.Getter;

@Getter
public class OpenRouterError extends RuntimeException {


    private final int statusCode;

    public OpenRouterError(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }
}
