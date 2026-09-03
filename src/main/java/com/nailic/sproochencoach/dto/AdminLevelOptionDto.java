package com.nailic.sproochencoach.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AdminLevelOptionDto {
    private String code;
    private String label;
    private String description;
    private boolean enabled = true;
}
