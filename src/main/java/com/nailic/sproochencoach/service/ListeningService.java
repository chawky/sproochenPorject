package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.dto.AudioExerciseDto;
import com.nailic.sproochencoach.dto.ExerciseRequestDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class ListeningService {
    private static final String PROMPT_KEY = "listening-generation";

    @Value("${ai.prompts.listening-generation}")
    private Resource listeningGenerationPromptResource;

    private final AudioExerciseGenerationService audioExerciseGenerationService;
    private final PromptFileService promptFileService;
    private final UserProgressService userProgressService;
    private final ExerciseConfigService exerciseConfigService;

    public ListeningService(
            AudioExerciseGenerationService audioExerciseGenerationService,
            PromptFileService promptFileService,
            UserProgressService userProgressService,
            ExerciseConfigService exerciseConfigService
    ) {
        this.audioExerciseGenerationService = audioExerciseGenerationService;
        this.promptFileService = promptFileService;
        this.userProgressService = userProgressService;
        this.exerciseConfigService = exerciseConfigService;
    }

    public AudioExerciseDto generateListeningExercise(ExerciseRequestDto exerciseRequestDto) {
        ExerciseRequestDto request = exerciseConfigService.normalizedRequest(exerciseRequestDto);
        AudioExerciseDto exercise = audioExerciseGenerationService.generateAudioExercise(
                request,
                promptFileService.readWithAdminGuidance(PROMPT_KEY, listeningGenerationPromptResource),
                AudioExerciseDto.class,
                "listening exercise"
        );

        exercise.setAttemptId(userProgressService.recordGeneratedExercise("LISTENING", request));

        return exercise;
    }
}
