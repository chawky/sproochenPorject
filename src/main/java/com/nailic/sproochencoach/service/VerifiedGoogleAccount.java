package com.nailic.sproochencoach.service;

public record VerifiedGoogleAccount(
        String subject,
        String email,
        boolean emailVerified,
        String firstName,
        String lastName
) {
}
