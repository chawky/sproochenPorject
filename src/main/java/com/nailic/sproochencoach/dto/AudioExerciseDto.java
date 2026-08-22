package com.nailic.sproochencoach.dto;

import lombok.Data;

@Data
public class AudioExerciseDto extends GeneratedExerciseDto {
    private String questionTranslation;
    private byte[] audio;
}
