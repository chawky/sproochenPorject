package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.constants.AppConstants;
import com.nailic.sproochencoach.dto.ExerciseRequestDto;
import com.nailic.sproochencoach.dto.GeneratedImageDto;
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
public class ImageDescriptionService {

    private static final Logger log = LoggerFactory.getLogger(ImageDescriptionService.class);
    private static final String IMAGE_GENERATION_PROMPT_KEY = PromptTemplateKey.IMAGE_GENERATION.getKey();
    private static final String IMAGE_EVALUATION_PROMPT_KEY = PromptTemplateKey.IMAGE_DESCRIPTION_EVALUATION.getKey();

    @Value(AppConstants.PropertyPlaceholders.AI_PROMPTS_IMAGE_GENERATION)
    private Resource imageGenerationPromptResource;

    @Value(AppConstants.PropertyPlaceholders.AI_PROMPTS_IMAGE_DESCRIPTION_EVALUATION)
    private Resource imageDescriptionEvaluationPromptResource;
    private final PromptFileService promptFileService;
    private final AiChatClient aiChatClient;
    private final AiImageClient aiImageClient;
    private final SpeakingService speakingService;
    private final ObjectMapper objectMapper;
    private final UserProgressService userProgressService;
    private final ExerciseConfigService exerciseConfigService;

    public ImageDescriptionService(
            AiChatClient aiChatClient,
            AiImageClient aiImageClient,
            PromptFileService promptFileService,
            SpeakingService speakingService,
            ObjectMapper objectMapper,
            UserProgressService userProgressService,
            ExerciseConfigService exerciseConfigService
    ) {
        this.promptFileService = promptFileService;
        this.aiChatClient = aiChatClient;
        this.aiImageClient = aiImageClient;
        this.speakingService = speakingService;
        this.objectMapper = objectMapper;
        this.userProgressService = userProgressService;
        this.exerciseConfigService = exerciseConfigService;
    }

    public GeneratedImageDto generateImage(ExerciseRequestDto request) {
        ExerciseRequestDto normalizedRequest = exerciseConfigService.normalizedRequest(request);
        String promptInstruction = promptFileService.readWithAdminGuidance(IMAGE_GENERATION_PROMPT_KEY, imageGenerationPromptResource)
                .formatted(
                normalizedRequest.getLevel(),
                exerciseConfigService.topicLabel(normalizedRequest.getTopic())
        );

        String imageDescription = aiChatClient.complete(
                promptInstruction,
                "image description prompt"
        );

        GeneratedImageDto generatedImageDto = new GeneratedImageDto();
        generatedImageDto.setImage(aiImageClient.generateImage(imageDescription));
        generatedImageDto.setImageDescription(imageDescription);
        generatedImageDto.setAttemptId(userProgressService.recordGeneratedExercise(AppConstants.ExerciseAttemptTypes.IMAGE_DESCRIPTION, normalizedRequest));
        return generatedImageDto;
    }

    public SpeakingEvaluation generateEvaluation(MultipartFile audio, String imageDescription) {
        return generateEvaluation(audio, imageDescription, null);
    }

    public SpeakingEvaluation generateEvaluation(MultipartFile audio, String imageDescription, Long audioDurationSeconds) {
        return generateEvaluation(audio, imageDescription, audioDurationSeconds, null);
    }

    public SpeakingEvaluation generateEvaluation(MultipartFile audio, String imageDescription, Long audioDurationSeconds, Long attemptId) {
        String transcription = speakingService.transcribeAudio(audio, audioDurationSeconds);

        String content = aiChatClient.complete(
                promptFileService.readWithAdminGuidance(IMAGE_EVALUATION_PROMPT_KEY, imageDescriptionEvaluationPromptResource)
                        .formatted(imageDescription, transcription),
                "image description evaluation"
        );

        try {
            SpeakingEvaluation evaluation = objectMapper.readValue(
                    content,
                    SpeakingEvaluation.class
            );
            userProgressService.recordEvaluation(
                    AppConstants.ExerciseAttemptTypes.IMAGE_DESCRIPTION,
                    "image description evaluation",
                    evaluation.getScore(),
                    attemptId,
                    evaluation.getTranscript()
            );
            return evaluation;
        } catch (JacksonException exception) {
            log.error("Failed to parse AI provider image description evaluation JSON. contentLength={}, jacksonMessage={}", content == null ? 0 : content.length(), exception.getMessage());

            throw new AiProviderException(
                    HttpStatus.BAD_GATEWAY.value(),
                    "AI provider returned invalid image description evaluation JSON"
            );
        }
    }
}
