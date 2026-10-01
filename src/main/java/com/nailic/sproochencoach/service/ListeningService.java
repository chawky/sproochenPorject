package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.constants.AppConstants;
import com.nailic.sproochencoach.dto.AudioExerciseDto;
import com.nailic.sproochencoach.dto.ExerciseRequestDto;
import com.nailic.sproochencoach.model.PromptTemplateKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class ListeningService {
    private static final String PROMPT_KEY = PromptTemplateKey.LISTENING_GENERATION.getKey();

    @Value(AppConstants.PropertyPlaceholders.AI_PROMPTS_LISTENING_GENERATION)
    private Resource listeningGenerationPromptResource;

    private final AudioExerciseGenerationService audioExerciseGenerationService;
    private final PromptFileService promptFileService;
    private final UserProgressService userProgressService;
    private final ExerciseConfigService exerciseConfigService;
    private final AiQuotaService aiQuotaService;
    private final AiRateLimitService aiRateLimitService;

    public ListeningService(
            AudioExerciseGenerationService audioExerciseGenerationService,
            PromptFileService promptFileService,
            UserProgressService userProgressService,
            ExerciseConfigService exerciseConfigService,
            AiQuotaService aiQuotaService,
            AiRateLimitService aiRateLimitService
    ) {
        this.audioExerciseGenerationService = audioExerciseGenerationService;
        this.promptFileService = promptFileService;
        this.userProgressService = userProgressService;
        this.exerciseConfigService = exerciseConfigService;
        this.aiQuotaService = aiQuotaService;
        this.aiRateLimitService = aiRateLimitService;
    }

    public AudioExerciseDto generateListeningExercise(ExerciseRequestDto exerciseRequestDto) {
        aiRateLimitService.checkAndRecordCurrentUserRequest();
        AiQuotaService.QuotaReservation reservation = aiQuotaService.reserveCurrentUserQuota(AiQuotaFeature.LISTENING);
        try {
        ExerciseRequestDto request = exerciseConfigService.normalizedRequest(exerciseRequestDto);
        AudioExerciseDto exercise = audioExerciseGenerationService.generateAudioExercise(
                request,
                promptFileService.readWithAdminGuidance(PROMPT_KEY, listeningGenerationPromptResource),
                AudioExerciseDto.class,
                "listening exercise"
        );

        exercise.setAttemptId(userProgressService.recordGeneratedExercise(AppConstants.ExerciseAttemptTypes.LISTENING, request, reservation));

        return exercise;
        } catch (RuntimeException exception) {
            aiQuotaService.releaseReservation(reservation);
            throw exception;
        }
    }
}
