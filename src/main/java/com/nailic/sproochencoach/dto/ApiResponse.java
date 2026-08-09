package com.nailic.sproochencoach.dto;

public record ApiResponse<T>(
        boolean success,
        String message,
        T data
) {
}