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
public class AdminUserProgressDto {
    private Long id;
    private String exerciseType;
    private String exerciseName;
    private String status;
    private String level;
    private String topic;
    private String answerType;
    private Double averageRatingOverall;
    private LocalDateTime generatedAt;
    private LocalDateTime completedAt;
    private LocalDateTime evaluatedAt;
}
