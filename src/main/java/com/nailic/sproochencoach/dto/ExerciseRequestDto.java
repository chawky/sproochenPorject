package com.nailic.sproochencoach.dto;

import com.nailic.sproochencoach.model.ExerciseTypeEnum;
import com.nailic.sproochencoach.model.LevelEnum;
import com.nailic.sproochencoach.model.TopicEnum;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExerciseRequestDto {
    private LevelEnum level;
    private TopicEnum topic;
    private ExerciseTypeEnum type;
}
