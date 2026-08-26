package com.nailic.sproochencoach.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SkillProgressDto {
    private String exerciseType;
    private int totalActivities;
    private int evaluatedActivities;
    private Double averageRatingOverall;
    private String latestExerciseName;
}
