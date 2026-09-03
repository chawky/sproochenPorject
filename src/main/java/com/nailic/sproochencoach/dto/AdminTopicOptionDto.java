package com.nailic.sproochencoach.dto;

import com.nailic.sproochencoach.model.LevelEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AdminTopicOptionDto {
    private String code;
    private String label;
    private LevelEnum level;
    private boolean enabled = true;
}
