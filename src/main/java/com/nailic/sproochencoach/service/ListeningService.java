package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.dto.AudioExerciseDto;
import com.nailic.sproochencoach.dto.ExerciseRequestDto;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class ListeningService {
    @Value("${ai.prompts.listening-generation}")
    private Resource listeningGenerationPromptResource;

    private final AudioExerciseGenerationService audioExerciseGenerationService;
    private final PromptFileService promptFileService;
    private final UserProgressService userProgressService;
    private String listeningGenerationPrompt;

    public ListeningService(
            AudioExerciseGenerationService audioExerciseGenerationService,
            PromptFileService promptFileService,
            UserProgressService userProgressService
    ) {
        this.audioExerciseGenerationService = audioExerciseGenerationService;
        this.promptFileService = promptFileService;
        this.userProgressService = userProgressService;
    }

    @PostConstruct
    void loadPromptFiles() {
        listeningGenerationPrompt = promptFileService.read(listeningGenerationPromptResource);
    }

    public AudioExerciseDto generateListeningExercise(ExerciseRequestDto exerciseRequestDto) {
        AudioExerciseDto exercise = audioExerciseGenerationService.generateAudioExercise(
                exerciseRequestDto,
                listeningGenerationPrompt,
                AudioExerciseDto.class,
                "listening exercise"
        );

        userProgressService.recordGeneratedExercise("LISTENING", exerciseRequestDto);

        return exercise;
    }
}
