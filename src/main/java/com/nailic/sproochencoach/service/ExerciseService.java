package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.dto.ExerciseRequestDto;
import com.nailic.sproochencoach.dto.GeneratedExerciseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class ExerciseService {
    private static final String PROMPT_KEY = "exercise-generation";

    @Value("${ai.prompts.exercise-generation}")
    private Resource exerciseGenerationPromptResource;
    private final AiChatClient aiChatClient;
    private final AiJsonParser aiJsonParser;
    private final PromptFileService promptFileService;
    private final UserProgressService userProgressService;
    private final ExerciseConfigService exerciseConfigService;

    public ExerciseService(
            AiChatClient aiChatClient,
            AiJsonParser aiJsonParser,
            PromptFileService promptFileService,
            UserProgressService userProgressService,
            ExerciseConfigService exerciseConfigService
    ) {
        this.aiChatClient = aiChatClient;
        this.aiJsonParser = aiJsonParser;
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
        GeneratedExerciseDto exercise = aiJsonParser.parseObject(content, GeneratedExerciseDto.class, "exercise");
        exercise.setAttemptId(userProgressService.recordGeneratedExercise("TEXT_EXERCISE", request));
        return exercise;
    }
}
