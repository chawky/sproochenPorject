package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.constants.AppConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpeechTranscriptionRouterTest {

    @Mock
    private SpeechTranscriptionService groqSpeechTranscriptionService;

    @Mock
    private OpenAiSpeechTranscriptionService openAiSpeechTranscriptionService;

    @Test
    void usesOpenAiWhenConfigured() {
        SpeechTranscriptionRouter router = new SpeechTranscriptionRouter(
                groqSpeechTranscriptionService,
                openAiSpeechTranscriptionService
        );
        ReflectionTestUtils.setField(router, "provider", AppConstants.Providers.OPENAI);

        MockMultipartFile audio = new MockMultipartFile(
                "audio", "recording.webm", "audio/webm", new byte[]{1, 2, 3}
        );
        when(openAiSpeechTranscriptionService.transcribeForEvaluation(
                audio, 4L, 12L, AppConstants.ExerciseAttemptTypes.SPEAKING
        )).thenReturn("Ech gesinn d'Fra moies iessen.");

        String result = router.transcribeForEvaluation(
                audio, 4L, 12L, AppConstants.ExerciseAttemptTypes.SPEAKING
        );

        assertThat(result).isEqualTo("Ech gesinn d'Fra moies iessen.");
        verify(openAiSpeechTranscriptionService).transcribeForEvaluation(
                audio, 4L, 12L, AppConstants.ExerciseAttemptTypes.SPEAKING
        );
        verifyNoInteractions(groqSpeechTranscriptionService);
    }

    @Test
    void usesGroqWhenConfigured() {
        SpeechTranscriptionRouter router = new SpeechTranscriptionRouter(
                groqSpeechTranscriptionService,
                openAiSpeechTranscriptionService
        );
        ReflectionTestUtils.setField(router, "provider", AppConstants.Providers.GROQ);

        MockMultipartFile audio = new MockMultipartFile(
                "audio", "recording.webm", "audio/webm", new byte[]{1, 2, 3}
        );
        when(groqSpeechTranscriptionService.transcribeForEvaluation(
                audio, 4L, 13L, AppConstants.ExerciseAttemptTypes.IMAGE_DESCRIPTION
        )).thenReturn("Ech gesinn eng Fra.");

        String result = router.transcribeForEvaluation(
                audio, 4L, 13L, AppConstants.ExerciseAttemptTypes.IMAGE_DESCRIPTION
        );

        assertThat(result).isEqualTo("Ech gesinn eng Fra.");
        verify(groqSpeechTranscriptionService).transcribeForEvaluation(
                audio, 4L, 13L, AppConstants.ExerciseAttemptTypes.IMAGE_DESCRIPTION
        );
        verifyNoInteractions(openAiSpeechTranscriptionService);
    }
}
