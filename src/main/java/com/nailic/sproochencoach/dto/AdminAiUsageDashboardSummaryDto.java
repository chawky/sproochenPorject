package com.nailic.sproochencoach.dto;

import java.util.List;

public record AdminAiUsageDashboardSummaryDto(
        List<AdminAiUsageModelSummaryDto> models,
        AdminAiUsageTotalsDto totals
) {
}
