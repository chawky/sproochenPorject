package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.constants.AppConstants;
import com.nailic.sproochencoach.dto.ExerciseRequestDto;
import com.nailic.sproochencoach.dto.SpeakingDto;
import com.nailic.sproochencoach.dto.SpeakingEvaluation;
import com.nailic.sproochencoach.exceptions.AiProviderException;
import com.nailic.sproochencoach.model.PromptTemplateKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class SpeakingService {
    private static final Logger log = LoggerFactory.getLogger(SpeakingService.class);
    private static final String SPEAKING_GENERATION_PROMPT_KEY = PromptTemplateKey.SPEAKING_GENERATION.getKey();
    private static final String SPEAKING_EVALUATION_PROMPT_KEY = PromptTemplateKey.SPEAKING_EVALUATION.getKey();

    @Value(AppConstants.PropertyPlaceholders.AI_PROMPTS_SPEAKING_GENERATION)
    private Resource speakingGenerationPromptResource;

    @Value(AppConstants.PropertyPlaceholders.AI_PROMPTS_SPEAKING_EVALUATION)
    private Resource speakingEvaluationPromptResource;

    private final AiChatClient aiChatClient;
    private final ObjectMapper objectMapper;
    private final PromptFileService promptFileService;
    private final AudioExerciseGenerationService audioExerciseGenerationService;
    private final UserProgressService userProgressService;
    private final ExerciseConfigService exerciseConfigService;
    private final AiQuotaService aiQuotaService;
    private final SpeechTranscriptionService speechTranscriptionService;

    public SpeakingService(
            AiChatClient aiChatClient,
            ObjectMapper objectMapper,
            PromptFileService promptFileService,
            AudioExerciseGenerationService audioExerciseGenerationService,
            UserProgressService userProgressService,
            ExerciseConfigService exerciseConfigService,
            AiQuotaService aiQuotaService,
            SpeechTranscriptionService speechTranscriptionService
    ) {
        this.aiChatClient = aiChatClient;
        this.objectMapper = objectMapper;
        this.promptFileService = promptFileService;
        this.audioExerciseGenerationService = audioExerciseGenerationService;
        this.userProgressService = userProgressService;
        this.exerciseConfigService = exerciseConfigService;
        this.aiQuotaService = aiQuotaService;
        this.speechTranscriptionService = speechTranscriptionService;
    }

    public SpeakingDto generateSpeakingPrompt(ExerciseRequestDto exerciseRequestDto) {
        aiQuotaService.checkCurrentUserQuota(AiQuotaFeature.SPEAKING);
        ExerciseRequestDto request = exerciseConfigService.normalizedRequest(exerciseRequestDto);
        SpeakingDto exercise = audioExerciseGenerationService.generateAudioExercise(
                request,
                promptFileService.readWithAdminGuidance(SPEAKING_GENERATION_PROMPT_KEY, speakingGenerationPromptResource),
                SpeakingDto.class,
                "speaking prompt"
        );
        exercise.setAttemptId(userProgressService.recordGeneratedExercise(AppConstants.ExerciseAttemptTypes.SPEAKING, request));
        return exercise;
    }

    public SpeakingEvaluation generateEvaluation(MultipartFile audio, Long audioDurationSeconds, Long attemptId) {
        String transcription = speechTranscriptionService.transcribeForEvaluation(
                audio,
                audioDurationSeconds,
                attemptId,
                AppConstants.ExerciseAttemptTypes.SPEAKING
        );

        String content = aiChatClient.complete(
                promptFileService.readWithAdminGuidance(SPEAKING_EVALUATION_PROMPT_KEY, speakingEvaluationPromptResource)
                        .formatted(transcription),
                "speaking evaluation"
        );

        try {
            SpeakingEvaluation evaluation = objectMapper.readValue(
                    content,
                    SpeakingEvaluation.class
            );
            userProgressService.recordEvaluation(
                    AppConstants.ExerciseAttemptTypes.SPEAKING,
                    "speaking evaluation",
                    evaluation.getScore(),
                    attemptId,
                    evaluation.getTranscript()
            );
            return evaluation;
        } catch (JacksonException exception) {
            log.error("Failed to parse AI provider speaking evaluation JSON. contentLength={}, jacksonMessage={}", content == null ? 0 : content.length(), exception.getMessage());

            throw new AiProviderException(
                    HttpStatus.BAD_GATEWAY.value(),
                    "AI provider returned invalid evaluation JSON"
            );
        }
    }

}
