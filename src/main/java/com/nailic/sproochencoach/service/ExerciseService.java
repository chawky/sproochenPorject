package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.dto.ExerciseRequestDto;
import com.nailic.sproochencoach.dto.GeneratedExerciseDto;
import com.nailic.sproochencoach.exceptions.AiProviderException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class ExerciseService {
    private static final Logger log = LoggerFactory.getLogger(ExerciseService.class);

    @Value("${ai.prompts.exercise-generation}")
    private Resource exerciseGenerationPromptResource;
    private final AiChatClient aiChatClient;
    private final ObjectMapper objectMapper;
    private final PromptFileService promptFileService;
    private final UserProgressService userProgressService;
    private String exerciseGenerationPrompt;

    public ExerciseService(
            AiChatClient aiChatClient,
            ObjectMapper objectMapper,
            PromptFileService promptFileService,
            UserProgressService userProgressService
    ) {
        this.aiChatClient = aiChatClient;
        this.objectMapper = objectMapper;
        this.promptFileService = promptFileService;
        this.userProgressService = userProgressService;
    }

    @PostConstruct
    void loadPromptFiles() {
        exerciseGenerationPrompt = promptFileService.read(exerciseGenerationPromptResource);
        log.debug("Exercise generation prompt loaded. characters={}", exerciseGenerationPrompt.length());
    }

    public GeneratedExerciseDto generateExercise(ExerciseRequestDto exerciseRequestDto) {
        log.debug(
                "Generating text exercise. level={}, topic={}, type={}",
                exerciseRequestDto.getLevel(),
                exerciseRequestDto.getTopic(),
                exerciseRequestDto.getType()
        );

        String prompt = exerciseGenerationPrompt.formatted(
                exerciseRequestDto.getType(),
                exerciseRequestDto.getLevel(),
                exerciseRequestDto.getTopic()
        );

        String content = aiChatClient.complete(prompt, "text exercise");

        try {
            GeneratedExerciseDto exercise = objectMapper.readValue(
                    content,
                    GeneratedExerciseDto.class
            );
            userProgressService.recordGeneratedExercise("TEXT_EXERCISE", exerciseRequestDto);
            log.debug("Text exercise generated successfully. type={}", exercise.getType());
            return exercise;
        } catch (JacksonException exception) {
            log.error(
                    "Failed to parse AI provider text exercise JSON. jacksonMessage={}, aiReturned={}",
                    exception.getMessage(),
                    content,
                    exception
            );

            throw new AiProviderException(
                    HttpStatus.BAD_GATEWAY.value(),
                    "AI provider returned invalid exercise JSON"
            );
        }
    }
}
