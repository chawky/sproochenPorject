package com.nailic.sproochencoach.dto;

import lombok.Data;

@Data
public class VerifyOtpRequest {
    private int otp;
    private String email;
}
