package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.constants.AppConstants;
import com.nailic.sproochencoach.exceptions.AiProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

@Service
public class OpenAiSpeechTranscriptionService {
    private static final Logger log = LoggerFactory.getLogger(OpenAiSpeechTranscriptionService.class);

    @Value(AppConstants.PropertyPlaceholders.AI_PROMPTS_TRANSCRIPTION)
    private Resource transcriptionPromptResource;

    private final RestClient openAiRestClient;
    private final PromptFileService promptFileService;
    private final AiUsageService aiUsageService;
    private final UserProgressService userProgressService;

    public OpenAiSpeechTranscriptionService(
            @Qualifier(AppConstants.RestClientBeans.OPENAI) RestClient openAiRestClient,
            PromptFileService promptFileService,
            AiUsageService aiUsageService,
            UserProgressService userProgressService
    ) {
        this.openAiRestClient = openAiRestClient;
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
        userProgressService.requireClaimedEvaluation(attemptId, expectedExerciseType);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add(AppConstants.OpenAiRequestFields.FILE, audio.getResource());
        body.add(AppConstants.OpenAiRequestFields.MODEL, AppConstants.Models.GPT_4O_TRANSCRIBE);
        body.add(AppConstants.OpenAiRequestFields.RESPONSE_FORMAT, AppConstants.OpenAiRequestFields.JSON_RESPONSE_FORMAT);
        body.add(AppConstants.OpenAiRequestFields.PROMPT, promptFileService.read(transcriptionPromptResource));

        try {
            OpenAiTranscriptionResponse response = openAiRestClient.post()
                    .uri(AppConstants.ApiPaths.OPENAI_AUDIO_TRANSCRIPTIONS)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(OpenAiTranscriptionResponse.class);

            if (response == null || !StringUtils.hasText(response.text())) {
                throw new AiProviderException(
                        HttpStatus.BAD_GATEWAY.value(),
                        "OpenAI returned an empty transcription"
                );
            }

            recordUsage(audio, audioDurationSeconds);
            return response.text();
        } catch (RuntimeException exception) {
            log.error(
                    "OpenAI transcription request failed. audioName={}, audioSize={}, reason={}",
                    audio.getOriginalFilename(),
                    audio.getSize(),
                    exception.getMessage()
            );
            throw exception;
        }
    }

    private void recordUsage(MultipartFile audio, Long audioDurationSeconds) {
        if (audioDurationSeconds != null && audioDurationSeconds > 0) {
            aiUsageService.recordAudioDurationUsage(
                    AppConstants.Providers.OPENAI,
                    AppConstants.Models.GPT_4O_TRANSCRIBE,
                    "transcription",
                    audioDurationSeconds
            );
            return;
        }

        aiUsageService.recordAudioUploadUsage(
                AppConstants.Providers.OPENAI,
                AppConstants.Models.GPT_4O_TRANSCRIBE,
                "transcription",
                audio.getSize()
        );
    }

    private record OpenAiTranscriptionResponse(String text) {
    }
}
