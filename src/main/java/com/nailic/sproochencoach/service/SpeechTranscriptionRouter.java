package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.constants.AppConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;

@Service
public class SpeechTranscriptionRouter {

    @Value(AppConstants.PropertyPlaceholders.AI_TRANSCRIPTION_PROVIDER)
    private String provider;

    private final SpeechTranscriptionService groqSpeechTranscriptionService;
    private final OpenAiSpeechTranscriptionService openAiSpeechTranscriptionService;

    public SpeechTranscriptionRouter(
            SpeechTranscriptionService groqSpeechTranscriptionService,
            OpenAiSpeechTranscriptionService openAiSpeechTranscriptionService
    ) {
        this.groqSpeechTranscriptionService = groqSpeechTranscriptionService;
        this.openAiSpeechTranscriptionService = openAiSpeechTranscriptionService;
    }

    public String transcribeForEvaluation(
            MultipartFile audio,
            Long audioDurationSeconds,
            Long attemptId,
            String expectedExerciseType
    ) {
        String selectedProvider = provider == null
                ? ""
                : provider.trim().toLowerCase(Locale.ROOT);

        return switch (selectedProvider) {
            case AppConstants.Providers.OPENAI -> openAiSpeechTranscriptionService.transcribeForEvaluation(
                    audio, audioDurationSeconds, attemptId, expectedExerciseType
            );
            case AppConstants.Providers.GROQ -> groqSpeechTranscriptionService.transcribeForEvaluation(
                    audio, audioDurationSeconds, attemptId, expectedExerciseType
            );
            default -> throw new IllegalStateException(
                    "Unsupported transcription provider: " + provider
            );
        };
    }
}
