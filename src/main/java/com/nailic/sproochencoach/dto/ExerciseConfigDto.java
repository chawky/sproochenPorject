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
public class ExerciseConfigDto {
    private List<LevelOptionDto> levels = new ArrayList<>();
    private List<TopicOptionDto> topics = new ArrayList<>();
    private List<ExerciseTypeOptionDto> exerciseTypes = new ArrayList<>();
}
