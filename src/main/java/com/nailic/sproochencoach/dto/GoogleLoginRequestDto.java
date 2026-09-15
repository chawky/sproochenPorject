package com.nailic.sproochencoach.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequestDto(
        @NotBlank String idToken
) {
}
