package com.nailic.sproochencoach.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminExerciseTypeConfigRequest {
    @Size(max = 80)
    private String code;

    @NotBlank
    @Size(max = 120)
    private String label;

    private Boolean enabled;
}
