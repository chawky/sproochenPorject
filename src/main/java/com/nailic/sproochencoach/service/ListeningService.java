package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.dto.AudioExerciseDto;
import com.nailic.sproochencoach.dto.ExerciseRequestDto;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ListeningService {
    private static final Logger log = LoggerFactory.getLogger(ListeningService.class);

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
        log.debug("Listening generation prompt loaded. characters={}", listeningGenerationPrompt.length());
    }

    public AudioExerciseDto generateListeningExercise(ExerciseRequestDto exerciseRequestDto) {
        log.debug(
                "Generating listening exercise. level={}, topic={}, type={}",
                exerciseRequestDto.getLevel(),
                exerciseRequestDto.getTopic(),
                exerciseRequestDto.getType()
        );

        AudioExerciseDto exercise = audioExerciseGenerationService.generateAudioExercise(
                exerciseRequestDto,
                listeningGenerationPrompt,
                AudioExerciseDto.class,
                "listening exercise"
        );

        userProgressService.recordGeneratedExercise("LISTENING", exerciseRequestDto);
        log.debug("Listening exercise generated successfully. audioBytes={}", exercise.getAudio() == null ? 0 : exercise.getAudio().length);

        return exercise;
    }
}
