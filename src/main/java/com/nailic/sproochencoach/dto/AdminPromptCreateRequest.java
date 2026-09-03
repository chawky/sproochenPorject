package com.nailic.sproochencoach.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminPromptCreateRequest {
    @NotBlank
    private String key;

    @Size(max = 120)
    private String title;

    @Size(max = 2000)
    private String editableContent;
}
