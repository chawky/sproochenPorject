package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.constants.AppConstants;
import com.nailic.sproochencoach.model.PromptTemplateKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SpeechTranscriptionService {
    private static final Logger log = LoggerFactory.getLogger(SpeechTranscriptionService.class);
    private static final String TRANSCRIPTION_PROMPT_KEY = PromptTemplateKey.GROQ_TRANSCRIPTION.getKey();

    @Value(AppConstants.PropertyPlaceholders.AI_PROMPTS_TRANSCRIPTION)
    private Resource transcriptionPromptResource;

    private final RestClient groqRestClient;
    private final PromptFileService promptFileService;
    private final AiUsageService aiUsageService;
    private final UserProgressService userProgressService;

    public SpeechTranscriptionService(
            @Qualifier(AppConstants.RestClientBeans.GROQ) RestClient groqRestClient,
            PromptFileService promptFileService,
            AiUsageService aiUsageService,
            UserProgressService userProgressService
    ) {
        this.groqRestClient = groqRestClient;
        this.promptFileService = promptFileService;
        this.aiUsageService = aiUsageService;
        this.userProgressService = userProgressService;
    }

    public String transcribeForEvaluation(
            MultipartFile audio,
            Long audioDurationSeconds,
            Long attemptId,
            String expectedExerciseType
    ) {
        userProgressService.requireUnevaluatedAttempt(attemptId, expectedExerciseType);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add(AppConstants.GroqRequestFields.FILE, audio.getResource());
        body.add(AppConstants.GroqRequestFields.MODEL, AppConstants.Models.WHISPER_LARGE_V3);
        body.add(AppConstants.GroqRequestFields.RESPONSE_FORMAT, AppConstants.GroqRequestFields.TEXT_RESPONSE_FORMAT);
        body.add(AppConstants.GroqRequestFields.LANGUAGE, AppConstants.GroqRequestFields.LUXEMBOURGISH_LANGUAGE);
        body.add(AppConstants.GroqRequestFields.PROMPT, promptFileService.readWithAdminGuidance(TRANSCRIPTION_PROMPT_KEY, transcriptionPromptResource));

        try {
            String transcription = groqRestClient.post()
                    .uri(AppConstants.ApiPaths.GROQ_AUDIO_TRANSCRIPTIONS)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            recordUsage(audio, audioDurationSeconds);
            return transcription;
        } catch (RuntimeException exception) {
            log.error("Groq transcription request failed. audioName={}, audioSize={}, reason={}", audio.getOriginalFilename(), audio.getSize(), exception.getMessage());
            throw exception;
        }
    }

    private void recordUsage(MultipartFile audio, Long audioDurationSeconds) {
        if (audioDurationSeconds != null && audioDurationSeconds > 0) {
            aiUsageService.recordAudioDurationUsage(
                    AppConstants.Providers.GROQ,
                    AppConstants.Models.WHISPER_LARGE_V3,
                    "transcription",
                    audioDurationSeconds
            );
            return;
        }

        aiUsageService.recordAudioUploadUsage(
                AppConstants.Providers.GROQ,
                AppConstants.Models.WHISPER_LARGE_V3,
                "transcription",
                audio.getSize()
        );
    }
}
