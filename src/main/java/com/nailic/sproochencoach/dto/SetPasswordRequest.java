package com.nailic.sproochencoach.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SetPasswordRequest(
        @NotBlank
        @Size(min = 8, max = 100)
        String newPassword,
        @NotBlank
        String confirmPassword
) {
}
