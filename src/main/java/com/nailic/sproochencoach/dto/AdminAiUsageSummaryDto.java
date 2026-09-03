package com.nailic.sproochencoach.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AdminAiUsageSummaryDto {
    private Integer userId;
    private long totalRequests;
    private long totalInputTokens;
    private long totalOutputTokens;
    private long totalTokens;
}
