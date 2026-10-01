package com.nailic.sproochencoach.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AiQuotaFeatureStatusDto {
    private String feature;
    private String window;
    @Schema(nullable = true)
    private Integer weeklyLimit;
    private long used;
    @Schema(nullable = true)
    private Long remaining;
    @Schema(nullable = true)
    private LocalDateTime windowStart;
    @Schema(nullable = true)
    private LocalDateTime windowEnd;
}
