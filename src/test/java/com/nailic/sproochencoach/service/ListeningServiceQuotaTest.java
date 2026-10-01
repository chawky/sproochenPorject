package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.constants.AppConstants;
import com.nailic.sproochencoach.dto.AudioExerciseDto;
import com.nailic.sproochencoach.dto.ExerciseRequestDto;
import com.nailic.sproochencoach.exceptions.AiProviderException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListeningServiceQuotaTest {
    @Mock private AudioExerciseGenerationService audioExerciseGenerationService;
    @Mock private PromptFileService promptFileService;
    @Mock private UserProgressService userProgressService;
    @Mock private ExerciseConfigService exerciseConfigService;
    @Mock private AiQuotaService aiQuotaService;

    @Test
    void oneSuccessfulListeningGenerationRecordsOneListeningUsage() {
        ExerciseRequestDto request = request();
        AudioExerciseDto generated = new AudioExerciseDto();
        stubGeneration(request, generated);
        when(userProgressService.recordGeneratedExercise(AppConstants.ExerciseAttemptTypes.LISTENING, request))
                .thenReturn(99L);

        listeningService().generateListeningExercise(request);

        verify(aiQuotaService).checkCurrentUserQuota(AiQuotaFeature.LISTENING);
        verify(userProgressService).recordGeneratedExercise(AppConstants.ExerciseAttemptTypes.LISTENING, request);
    }

    @Test
    void failedProviderRequestDoesNotRecordListeningUsage() {
        ExerciseRequestDto request = request();
        when(exerciseConfigService.normalizedRequest(request)).thenReturn(request);
        when(promptFileService.readWithAdminGuidance(anyString(), nullable(Resource.class))).thenReturn("prompt");
        when(audioExerciseGenerationService.generateAudioExercise(
                eq(request), eq("prompt"), eq(AudioExerciseDto.class), eq("listening exercise")
        )).thenThrow(new AiProviderException(502, "provider failed"));

        assertThatThrownBy(() -> listeningService().generateListeningExercise(request))
                .isInstanceOf(AiProviderException.class);

        verify(userProgressService, never()).recordGeneratedExercise(
                AppConstants.ExerciseAttemptTypes.LISTENING, request
        );
    }

    @Test
    void exhaustedListeningQuotaStopsBeforeAnyProviderCall() {
        ExerciseRequestDto request = request();
        doThrow(new com.nailic.sproochencoach.exceptions.AiQuotaExceededException("limit"))
                .when(aiQuotaService).checkCurrentUserQuota(AiQuotaFeature.LISTENING);

        assertThatThrownBy(() -> listeningService().generateListeningExercise(request))
                .isInstanceOf(com.nailic.sproochencoach.exceptions.AiQuotaExceededException.class);

        verify(audioExerciseGenerationService, never()).generateAudioExercise(
                eq(request), anyString(), eq(AudioExerciseDto.class), eq("listening exercise")
        );
    }

    private void stubGeneration(ExerciseRequestDto request, AudioExerciseDto generated) {
        when(exerciseConfigService.normalizedRequest(request)).thenReturn(request);
        when(promptFileService.readWithAdminGuidance(anyString(), nullable(Resource.class))).thenReturn("prompt");
        when(audioExerciseGenerationService.generateAudioExercise(
                request, "prompt", AudioExerciseDto.class, "listening exercise"
        )).thenReturn(generated);
    }

    private ListeningService listeningService() {
        return new ListeningService(
                audioExerciseGenerationService, promptFileService, userProgressService,
                exerciseConfigService, aiQuotaService
        );
    }

    private ExerciseRequestDto request() {
        ExerciseRequestDto request = new ExerciseRequestDto();
        request.setLevel("A1");
        request.setTopic("FAMILY");
        request.setType("MULTIPLE_CHOICE");
        return request;
    }
}
