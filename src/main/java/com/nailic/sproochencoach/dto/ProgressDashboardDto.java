package com.nailic.sproochencoach.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class ProgressDashboardDto {
    private Integer userId;
    private String username;
    private String email;
    private int loggedInDays;
    private int currentStreakDays;
    private LocalDate lastLoginDate;
    private int totalActivities;
    private int evaluatedActivities;
    private Double averageRatingOverall;
    private List<SkillProgressDto> skillProgress;
}
