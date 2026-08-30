package com.nailic.sproochencoach.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StripeSessionURLDto {
    private String stripeSessionUrl;
}
