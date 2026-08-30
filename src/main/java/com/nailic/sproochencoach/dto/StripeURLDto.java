package com.nailic.sproochencoach.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StripeURLDto {
private String sucessURL;
private String cancelURL;
}
