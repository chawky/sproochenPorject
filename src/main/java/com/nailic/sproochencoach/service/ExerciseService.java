package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.dto.ExerciseRequestDto;
import com.nailic.sproochencoach.dto.GeneratedExerciseDto;
import com.nailic.sproochencoach.exceptions.AiProviderException;
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
    private static final String PROMPT_KEY = "exercise-generation";

    @Value("${ai.prompts.exercise-generation}")
    private Resource exerciseGenerationPromptResource;
    private final AiChatClient aiChatClient;
    private final ObjectMapper objectMapper;
    private final PromptFileService promptFileService;
    private final UserProgressService userProgressService;
    private final ExerciseConfigService exerciseConfigService;

    public ExerciseService(
            AiChatClient aiChatClient,
            ObjectMapper objectMapper,
            PromptFileService promptFileService,
            UserProgressService userProgressService,
            ExerciseConfigService exerciseConfigService
    ) {
        this.aiChatClient = aiChatClient;
        this.objectMapper = objectMapper;
        this.promptFileService = promptFileService;
        this.userProgressService = userProgressService;
        this.exerciseConfigService = exerciseConfigService;
    }

    public GeneratedExerciseDto generateExercise(ExerciseRequestDto exerciseRequestDto) {
        ExerciseRequestDto request = exerciseConfigService.normalizedRequest(exerciseRequestDto);
        String promptTemplate = promptFileService.readWithAdminGuidance(PROMPT_KEY, exerciseGenerationPromptResource);
        String prompt = promptTemplate.formatted(
                request.getType(),
                request.getLevel(),
                exerciseConfigService.topicLabel(request.getTopic())
        );

        String content = aiChatClient.complete(prompt, "text exercise");

        try {
            GeneratedExerciseDto exercise = objectMapper.readValue(
                    content,
                    GeneratedExerciseDto.class
            );
            exercise.setAttemptId(userProgressService.recordGeneratedExercise("TEXT_EXERCISE", request));
            return exercise;
        } catch (JacksonException exception) {
            log.error("Failed to parse AI provider text exercise JSON. contentLength={}, jacksonMessage={}", content == null ? 0 : content.length(), exception.getMessage());

            throw new AiProviderException(
                    HttpStatus.BAD_GATEWAY.value(),
                    "AI provider returned invalid exercise JSON"
            );
        }
    }
}
