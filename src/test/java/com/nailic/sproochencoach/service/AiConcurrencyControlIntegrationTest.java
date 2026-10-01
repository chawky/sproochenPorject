package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.constants.AppConstants;
import com.nailic.sproochencoach.exceptions.AiQuotaExceededException;
import com.nailic.sproochencoach.exceptions.BadRequestException;
import com.nailic.sproochencoach.model.AppUser;
import com.nailic.sproochencoach.model.ExerciseAttempt;
import com.nailic.sproochencoach.repository.AiFeatureQuotaReservationRepo;
import com.nailic.sproochencoach.repository.AppUserRepo;
import com.nailic.sproochencoach.repository.ExerciseAttemptRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AiConcurrencyControlIntegrationTest {
    @Autowired private AppUserRepo appUserRepo;
    @Autowired private ExerciseAttemptRepo exerciseAttemptRepo;
    @Autowired private AiFeatureQuotaReservationRepo reservationRepo;
    @Autowired private UserProgressService userProgressService;
    @Autowired private AiQuotaService aiQuotaService;

    private AppUser user;

    @BeforeEach
    void createUser() {
        AppUser newUser = new AppUser();
        newUser.setUsername("concurrency-" + System.nanoTime());
        newUser.setEmail(newUser.getUsername() + "@example.com");
        newUser.setPassword("test");
        newUser.setEnabled(true);
        Integer userId = appUserRepo.saveAndFlush(newUser).getId();
        user = appUserRepo.findById(userId).orElseThrow();
        assertThat(user.getAiQuotaAnchor()).isNotNull();
    }

    @Test
    void simultaneousEvaluationClaimsAllowOnlyOneProviderFlow() throws Exception {
        ExerciseAttempt attempt = saveAttempt(AppConstants.ExerciseAttemptTypes.SPEAKING);
        CyclicBarrier start = new CyclicBarrier(2);
        AtomicInteger providerFlows = new AtomicInteger();

        Callable<Boolean> evaluation = () -> authenticated(() -> {
            start.await();
            try {
                userProgressService.claimEvaluation(attempt.getId(), AppConstants.ExerciseAttemptTypes.SPEAKING);
                providerFlows.incrementAndGet();
                return true;
            } catch (BadRequestException rejected) {
                return false;
            }
        });

        List<Boolean> results = runConcurrently(evaluation, evaluation);

        assertThat(results).containsExactlyInAnyOrder(true, false);
        assertThat(providerFlows).hasValue(1);
    }

    @Test
    void failedEvaluationCanReleaseItsClaimAndRetry() throws Exception {
        ExerciseAttempt attempt = saveAttempt(AppConstants.ExerciseAttemptTypes.SPEAKING);

        authenticated(() -> {
            userProgressService.claimEvaluation(attempt.getId(), AppConstants.ExerciseAttemptTypes.SPEAKING);
            userProgressService.releaseEvaluationClaim(attempt.getId(), AppConstants.ExerciseAttemptTypes.SPEAKING);
            userProgressService.claimEvaluation(attempt.getId(), AppConstants.ExerciseAttemptTypes.SPEAKING);
            return null;
        });

        assertThat(exerciseAttemptRepo.findById(attempt.getId()).orElseThrow().getStatus().name())
                .isEqualTo("EVALUATING");
    }

    @Test
    void oneRemainingBasicQuotaUnitAllowsOnlyOneConcurrentReservation() throws Exception {
        for (int i = 0; i < 14; i++) {
            saveAttempt(AppConstants.ExerciseAttemptTypes.SPEAKING);
        }
        CyclicBarrier start = new CyclicBarrier(2);

        Callable<Boolean> generation = () -> authenticated(() -> {
            start.await();
            try {
                aiQuotaService.reserveCurrentUserQuota(AiQuotaFeature.SPEAKING);
                return true;
            } catch (AiQuotaExceededException rejected) {
                return false;
            }
        });

        List<Boolean> results = runConcurrently(generation, generation);

        assertThat(results).containsExactlyInAnyOrder(true, false);
        assertThat(reservationRepo.countByUserIdAndFeatureAndWindowStart(
                user.getId(), AiQuotaFeature.SPEAKING.name(), user.getAiQuotaAnchor().atStartOfDay()
        )).isEqualTo(1);
    }

    private ExerciseAttempt saveAttempt(String type) {
        ExerciseAttempt attempt = new ExerciseAttempt();
        attempt.setUser(user);
        attempt.setExerciseType(type);
        attempt.setExerciseName(type);
        attempt.setGeneratedAt(LocalDateTime.now());
        return exerciseAttemptRepo.saveAndFlush(attempt);
    }

    private <T> T authenticated(CheckedSupplier<T> operation) throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, "", user.getAuthorities())
        );
        try {
            return operation.get();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private List<Boolean> runConcurrently(Callable<Boolean> first, Callable<Boolean> second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> firstResult = executor.submit(first);
            Future<Boolean> secondResult = executor.submit(second);
            return List.of(firstResult.get(), secondResult.get());
        } finally {
            executor.shutdownNow();
        }
    }

    @FunctionalInterface
    private interface CheckedSupplier<T> {
        T get() throws Exception;
    }
}
