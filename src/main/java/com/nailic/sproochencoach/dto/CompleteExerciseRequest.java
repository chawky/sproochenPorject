package com.nailic.sproochencoach.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompleteExerciseRequest {
    @Size(max = 2000)
    private String learnerAnswer;
}
