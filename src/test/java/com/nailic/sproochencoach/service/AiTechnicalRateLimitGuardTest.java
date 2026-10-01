package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.dto.ExerciseRequestDto;
import com.nailic.sproochencoach.exceptions.AiRateLimitExceededException;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class AiTechnicalRateLimitGuardTest {
    @Test
    void generationRateLimitRejectsBeforeQuotaOrPaidProviders() {
        AiRateLimitService rateLimit = mock(AiRateLimitService.class);
        AiQuotaService quota = mock(AiQuotaService.class);
        AudioExerciseGenerationService paidProviderFlow = mock(AudioExerciseGenerationService.class);
        doThrow(new AiRateLimitExceededException("rate limit"))
                .when(rateLimit).checkAndRecordCurrentUserRequest();
        SpeakingService service = speakingService(rateLimit, quota, paidProviderFlow, mock(UserProgressService.class));

        assertThatThrownBy(() -> service.generateSpeakingPrompt(new ExerciseRequestDto()))
                .isInstanceOf(AiRateLimitExceededException.class);

        verifyNoInteractions(quota, paidProviderFlow);
    }

    @Test
    void evaluationRateLimitRejectsBeforeClaimGroqOrKimi() {
        AiRateLimitService rateLimit = mock(AiRateLimitService.class);
        UserProgressService progress = mock(UserProgressService.class);
        SpeechTranscriptionService groq = mock(SpeechTranscriptionService.class);
        AiChatClient kimi = mock(AiChatClient.class);
        doThrow(new AiRateLimitExceededException("rate limit"))
                .when(rateLimit).checkAndRecordCurrentUserRequest();
        SpeakingService service = new SpeakingService(
                kimi, new ObjectMapper(), mock(PromptFileService.class),
                mock(AudioExerciseGenerationService.class), progress,
                mock(ExerciseConfigService.class), mock(AiQuotaService.class), groq, rateLimit
        );

        assertThatThrownBy(() -> service.generateEvaluation(mock(MultipartFile.class), 5L, 7L))
                .isInstanceOf(AiRateLimitExceededException.class);

        verifyNoInteractions(progress, groq, kimi);
    }

    private SpeakingService speakingService(
            AiRateLimitService rateLimit,
            AiQuotaService quota,
            AudioExerciseGenerationService provider,
            UserProgressService progress
    ) {
        return new SpeakingService(
                mock(AiChatClient.class), new ObjectMapper(), mock(PromptFileService.class),
                provider, progress, mock(ExerciseConfigService.class), quota,
                mock(SpeechTranscriptionService.class), rateLimit
        );
    }
}
