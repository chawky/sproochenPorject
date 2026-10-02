package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.constants.AppConstants;
import com.nailic.sproochencoach.config.AiLeaseProperties;
import com.nailic.sproochencoach.dto.SpeakingEvaluation;
import com.nailic.sproochencoach.exceptions.BadRequestException;
import com.nailic.sproochencoach.exceptions.AiProviderException;
import com.nailic.sproochencoach.model.AppUser;
import com.nailic.sproochencoach.model.ExerciseAttempt;
import com.nailic.sproochencoach.model.ExerciseAttemptStatus;
import com.nailic.sproochencoach.repository.AppUserRepo;
import com.nailic.sproochencoach.repository.ExerciseAttemptRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvaluationAttemptGuardTest {
    private static final Integer USER_ID = 42;
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-09-10T08:00:00Z"), ZoneId.of("Europe/Paris")
    );
    private static final LocalDateTime CLAIMED_AT = LocalDateTime.of(2026, 9, 10, 10, 0);
    private static final LocalDateTime CLAIM_EXPIRY_CUTOFF = LocalDateTime.of(2026, 9, 10, 9, 50);

    @Mock private LoggedInUser loggedInUser;
    @Mock private ExerciseAttemptRepo exerciseAttemptRepo;
    @Mock private UserLoginDayService userLoginDayService;
    @Mock private AppUserRepo appUserRepo;
    @Mock private AiChatClient aiChatClient;
    @Mock private PromptFileService promptFileService;
    @Mock private MultipartFile audio;
    @Mock private AiRateLimitService aiRateLimitService;
    @Mock private AiQuotaService aiQuotaService;

    @Test
    void firstEvaluationSucceeds() {
        ExerciseAttempt attempt = attempt(1L, USER_ID, AppConstants.ExerciseAttemptTypes.SPEAKING);
        UserProgressService progressService = progressService();
        SpeechTranscriptionService transcriptionService = mock(SpeechTranscriptionService.class);
        stubSuccessfulClaim(attempt);
        when(exerciseAttemptRepo.findById(1L)).thenReturn(Optional.of(attempt));
        when(transcriptionService.transcribeForEvaluation(
                audio, 5L, 1L, AppConstants.ExerciseAttemptTypes.SPEAKING
        )).thenReturn("Ech schwätzen.");
        stubEvaluationResponse();

        SpeakingEvaluation result = speakingService(progressService, transcriptionService)
                .generateEvaluation(audio, 5L, 1L);

        assertThat(result.getScore()).isEqualTo(8);
        assertThat(attempt.getStatus()).isEqualTo(ExerciseAttemptStatus.EVALUATED);
        assertThat(attempt.getEvaluatedAt()).isNotNull();
        assertThat(attempt.getEvaluationClaimedAt()).isNull();
        verify(exerciseAttemptRepo).save(attempt);
    }

    @Test
    void secondEvaluationIsRejectedBeforeGroqOrKimi() {
        UserProgressService progressService = progressService();
        RestClient groqClient = mock(RestClient.class, RETURNS_DEEP_STUBS);
        SpeechTranscriptionService transcriptionService = new SpeechTranscriptionService(
                groqClient, promptFileService, mock(AiUsageService.class), progressService
        );

        assertThatThrownBy(() -> speakingService(progressService, transcriptionService)
                .generateEvaluation(audio, 5L, 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Exercise attempt cannot be evaluated");

        verifyNoInteractions(groqClient, aiChatClient);
    }

    @Test
    void providerFailureReleasesEvaluationClaimForRetry() {
        ExerciseAttempt attempt = attempt(1L, USER_ID, AppConstants.ExerciseAttemptTypes.SPEAKING);
        UserProgressService progressService = progressService();
        SpeechTranscriptionService transcriptionService = mock(SpeechTranscriptionService.class);
        stubSuccessfulClaim(attempt);
        when(exerciseAttemptRepo.releaseEvaluation(
                1L, USER_ID, AppConstants.ExerciseAttemptTypes.SPEAKING, CLAIMED_AT
        )).thenAnswer(invocation -> {
            attempt.setStatus(ExerciseAttemptStatus.GENERATED);
            attempt.setEvaluationClaimedAt(null);
            return 1;
        });
        when(transcriptionService.transcribeForEvaluation(
                audio, 5L, 1L, AppConstants.ExerciseAttemptTypes.SPEAKING
        )).thenThrow(new AiProviderException(502, "Groq failed"));

        assertThatThrownBy(() -> speakingService(progressService, transcriptionService)
                .generateEvaluation(audio, 5L, 1L))
                .isInstanceOf(AiProviderException.class);

        verify(exerciseAttemptRepo).releaseEvaluation(
                1L, USER_ID, AppConstants.ExerciseAttemptTypes.SPEAKING, CLAIMED_AT
        );
        assertThat(attempt.getStatus()).isEqualTo(ExerciseAttemptStatus.GENERATED);
        assertThat(attempt.getEvaluationClaimedAt()).isNull();
        verifyNoInteractions(aiChatClient);
    }

    @Test
    void anotherValidAttemptCanStillBeEvaluated() {
        ExerciseAttempt first = attempt(1L, USER_ID, AppConstants.ExerciseAttemptTypes.SPEAKING);
        ExerciseAttempt second = attempt(2L, USER_ID, AppConstants.ExerciseAttemptTypes.SPEAKING);
        stubSuccessfulClaim(first);
        stubSuccessfulClaim(second);
        when(exerciseAttemptRepo.findById(1L)).thenReturn(Optional.of(first));
        when(exerciseAttemptRepo.findById(2L)).thenReturn(Optional.of(second));
        SpeechTranscriptionService transcriptionService = mock(SpeechTranscriptionService.class);
        when(transcriptionService.transcribeForEvaluation(
                audio, 5L, 1L, AppConstants.ExerciseAttemptTypes.SPEAKING
        )).thenReturn("Éischt Äntwert");
        when(transcriptionService.transcribeForEvaluation(
                audio, 5L, 2L, AppConstants.ExerciseAttemptTypes.SPEAKING
        )).thenReturn("Zweet Äntwert");
        stubEvaluationResponse();

        SpeakingService service = speakingService(progressService(), transcriptionService);
        service.generateEvaluation(audio, 5L, 1L);
        service.generateEvaluation(audio, 5L, 2L);

        assertThat(first.getStatus()).isEqualTo(ExerciseAttemptStatus.EVALUATED);
        assertThat(second.getStatus()).isEqualTo(ExerciseAttemptStatus.EVALUATED);
        verify(exerciseAttemptRepo).save(first);
        verify(exerciseAttemptRepo).save(second);
    }

    @Test
    void userCannotEvaluateAnotherUsersAttempt() {
        assertThatThrownBy(() -> progressService().claimEvaluation(
                1L, AppConstants.ExerciseAttemptTypes.SPEAKING
        )).isInstanceOf(BadRequestException.class)
                .hasMessage("Exercise attempt cannot be evaluated");
    }

    @Test
    void wrongAttemptTypeIsRejected() {
        assertThatThrownBy(() -> progressService().claimEvaluation(
                1L, AppConstants.ExerciseAttemptTypes.SPEAKING
        )).isInstanceOf(BadRequestException.class)
                .hasMessage("Exercise attempt cannot be evaluated");
    }

    @Test
    void imageDescriptionEvaluationRequiresImageDescriptionAttempt() {
        SpeechTranscriptionService transcriptionService = mock(SpeechTranscriptionService.class);
        UserProgressService progressService = mock(UserProgressService.class);
        when(progressService.claimEvaluation(
                7L, AppConstants.ExerciseAttemptTypes.IMAGE_DESCRIPTION
        )).thenReturn(CLAIMED_AT);
        when(transcriptionService.transcribeForEvaluation(
                audio, 5L, 7L, AppConstants.ExerciseAttemptTypes.IMAGE_DESCRIPTION
        )).thenReturn("Eng Beschreiwung");
        stubEvaluationResponse();

        ImageDescriptionService service = new ImageDescriptionService(
                aiChatClient,
                mock(AiImageClient.class),
                promptFileService,
                transcriptionService,
                new ObjectMapper(),
                progressService,
                mock(ExerciseConfigService.class),
                aiQuotaService,
                aiRateLimitService
        );
        service.generateEvaluation(audio, "Bild", 5L, 7L);

        verify(transcriptionService).transcribeForEvaluation(
                audio, 5L, 7L, AppConstants.ExerciseAttemptTypes.IMAGE_DESCRIPTION
        );
        verify(progressService).recordEvaluation(
                AppConstants.ExerciseAttemptTypes.IMAGE_DESCRIPTION,
                "image description evaluation",
                8,
                7L,
                "Ech schwätzen.",
                CLAIMED_AT
        );
    }

    private void stubEvaluationResponse() {
        when(promptFileService.readWithAdminGuidance(anyString(), nullable(Resource.class)))
                .thenReturn("Evaluate %s");
        when(aiChatClient.complete(anyString(), anyString())).thenReturn("""
                {"transcript":"Ech schwätzen.","score":8,"feedback":"Good","corrections":[]}
                """);
    }

    private SpeakingService speakingService(
            UserProgressService progressService,
            SpeechTranscriptionService transcriptionService
    ) {
        return new SpeakingService(
                aiChatClient,
                new ObjectMapper(),
                promptFileService,
                mock(AudioExerciseGenerationService.class),
                progressService,
                mock(ExerciseConfigService.class),
                aiQuotaService,
                transcriptionService,
                aiRateLimitService
        );
    }

    private UserProgressService progressService() {
        AppUser currentUser = new AppUser();
        currentUser.setId(USER_ID);
        org.mockito.Mockito.lenient().when(loggedInUser.get()).thenReturn(currentUser);
        org.mockito.Mockito.lenient().when(loggedInUser.getId()).thenReturn(USER_ID);
        AiLeaseProperties leaseProperties = new AiLeaseProperties();
        leaseProperties.setEvaluationClaimTimeout(Duration.ofMinutes(10));
        return new UserProgressService(
                loggedInUser,
                exerciseAttemptRepo,
                userLoginDayService,
                appUserRepo,
                aiQuotaService,
                leaseProperties,
                CLOCK
        );
    }

    private void stubSuccessfulClaim(ExerciseAttempt attempt) {
        when(exerciseAttemptRepo.claimEvaluation(
                attempt.getId(), USER_ID, attempt.getExerciseType(), CLAIMED_AT, CLAIM_EXPIRY_CUTOFF
        ))
                .thenAnswer(invocation -> {
                    attempt.setStatus(ExerciseAttemptStatus.EVALUATING);
                    attempt.setEvaluationClaimedAt(CLAIMED_AT);
                    return 1;
                });
    }

    private ExerciseAttempt attempt(Long id, Integer userId, String type) {
        AppUser owner = new AppUser();
        owner.setId(userId);
        ExerciseAttempt attempt = new ExerciseAttempt();
        attempt.setId(id);
        attempt.setUser(owner);
        attempt.setExerciseType(type);
        attempt.setExerciseName(type);
        return attempt;
    }
}
