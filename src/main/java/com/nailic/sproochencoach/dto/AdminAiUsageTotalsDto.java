package com.nailic.sproochencoach.dto;

import java.math.BigDecimal;

public record AdminAiUsageTotalsDto(
        long requests,
        long inputTokens,
        long outputTokens,
        long totalTokens,
        BigDecimal estimatedCostUsd
) {
}
