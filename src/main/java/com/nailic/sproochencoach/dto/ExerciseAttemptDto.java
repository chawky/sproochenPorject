package com.nailic.sproochencoach.dto;

import com.nailic.sproochencoach.model.ExerciseAttemptStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ExerciseAttemptDto {
    private Long id;
    private String exerciseType;
    private String exerciseName;
    private ExerciseAttemptStatus status;
    private String level;
    private String topic;
    private String answerType;
    private String learnerAnswer;
    private Double averageRatingOverall;
    private LocalDateTime generatedAt;
    private LocalDateTime completedAt;
    private LocalDateTime evaluatedAt;
}
