package com.nailic.sproochencoach.dto;

import java.math.BigDecimal;

public record AdminAiUsageModelSummaryDto(
        String provider,
        String model,
        long requests,
        long inputTokens,
        long outputTokens,
        long totalTokens,
        BigDecimal estimatedCostUsd
) {
}
