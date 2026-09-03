package com.nailic.sproochencoach.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AdminExerciseConfigDto {
    private boolean editable = false;
    private List<AdminLevelOptionDto> levels = new ArrayList<>();
    private List<AdminTopicOptionDto> topics = new ArrayList<>();
    private List<AdminExerciseTypeOptionDto> exerciseTypes = new ArrayList<>();
}
