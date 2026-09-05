package com.nailic.sproochencoach.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VocabularyRequestDto {
    @NotBlank(message = "level must not be blank")
    private String level;

    @NotBlank(message = "topic must not be blank")
    private String topic;
}
