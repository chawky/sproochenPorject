package com.nailic.sproochencoach.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExerciseRequestDto {
    @NotBlank
    private String level;

    @NotBlank
    private String topic;

    @NotBlank
    private String type;
}
