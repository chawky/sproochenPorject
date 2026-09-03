package com.nailic.sproochencoach.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GeneratedExerciseDto {
    private Long attemptId;
    private String question;
    private String type;
    private List<String> options;
    private String expectedAnswer;
    private String hint;
}
