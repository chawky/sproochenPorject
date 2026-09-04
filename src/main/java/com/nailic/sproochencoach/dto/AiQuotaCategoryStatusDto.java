package com.nailic.sproochencoach.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AiQuotaCategoryStatusDto {
    private String category;
    private String window;
    private Integer limit;
    private long used;
    private Long remaining;
    private LocalDateTime windowStart;
    private LocalDateTime windowEnd;
}
