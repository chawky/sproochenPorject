package com.nailic.sproochencoach.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SubscriptionInfoDto {
    private boolean subscribed;
    private String status;
    private LocalDate startedAt;
    private LocalDate currentPeriodEnd;
}
