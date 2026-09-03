package com.nailic.sproochencoach.dto;
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
    private String levelCode;
    private boolean enabled = true;
}
