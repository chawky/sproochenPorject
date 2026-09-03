package com.nailic.sproochencoach.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AdminAiUsageDto {
    private Long id;
    private String provider;
    private String model;
    private String requestName;
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer totalTokens;
    private String usageUnit;
    private Long usageAmount;
    private BigDecimal estimatedCostUsd;
    private LocalDateTime createdAt;
}
