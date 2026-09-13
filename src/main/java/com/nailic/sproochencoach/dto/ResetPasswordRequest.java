package com.nailic.sproochencoach.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @Email
    @NotBlank
    private String email;
    @NotBlank
    @Pattern(regexp = "\\d{6}", message = "must be a 6-digit code")
    private String code;
    @NotBlank
    @Size(min = 8, max = 100)
    private String newPassword;
}
