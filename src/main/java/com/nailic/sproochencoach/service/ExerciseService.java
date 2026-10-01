package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.constants.AppConstants;
import com.nailic.sproochencoach.dto.ExerciseRequestDto;
import com.nailic.sproochencoach.dto.GeneratedExerciseDto;
import com.nailic.sproochencoach.model.PromptTemplateKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class ExerciseService {
    private static final String PROMPT_KEY = PromptTemplateKey.EXERCISE_GENERATION.getKey();

    @Value(AppConstants.PropertyPlaceholders.AI_PROMPTS_EXERCISE_GENERATION)
    private Resource exerciseGenerationPromptResource;
    private final AiChatClient aiChatClient;
    private final AiJsonParser aiJsonParser;
    private final PromptFileService promptFileService;
    private final UserProgressService userProgressService;
    private final ExerciseConfigService exerciseConfigService;
    private final AiQuotaService aiQuotaService;
    private final AiRateLimitService aiRateLimitService;

    public ExerciseService(
            AiChatClient aiChatClient,
            AiJsonParser aiJsonParser,
            PromptFileService promptFileService,
            UserProgressService userProgressService,
            ExerciseConfigService exerciseConfigService,
            AiQuotaService aiQuotaService,
            AiRateLimitService aiRateLimitService
    ) {
        this.aiChatClient = aiChatClient;
        this.aiJsonParser = aiJsonParser;
        this.promptFileService = promptFileService;
        this.userProgressService = userProgressService;
        this.exerciseConfigService = exerciseConfigService;
        this.aiQuotaService = aiQuotaService;
        this.aiRateLimitService = aiRateLimitService;
    }

    public GeneratedExerciseDto generateExercise(ExerciseRequestDto exerciseRequestDto) {
        aiRateLimitService.checkAndRecordCurrentUserRequest();
        AiQuotaService.QuotaReservation reservation = aiQuotaService.reserveCurrentUserQuota(AiQuotaFeature.TOPIC_EXERCISE);
        try {
        ExerciseRequestDto request = exerciseConfigService.normalizedRequest(exerciseRequestDto);
        String promptTemplate = promptFileService.readWithAdminGuidance(PROMPT_KEY, exerciseGenerationPromptResource);
        String prompt = promptTemplate.formatted(
                request.getType(),
                request.getLevel(),
                exerciseConfigService.topicLabel(request.getTopic())
        );

        String content = aiChatClient.complete(prompt, "text exercise");
        GeneratedExerciseDto exercise = aiJsonParser.parseObject(content, GeneratedExerciseDto.class, "exercise");
        exercise.setAttemptId(userProgressService.recordGeneratedExercise(AppConstants.ExerciseAttemptTypes.TEXT_EXERCISE, request, reservation));
        return exercise;
        } catch (RuntimeException exception) {
            aiQuotaService.releaseReservation(reservation);
            throw exception;
        }
    }
}
