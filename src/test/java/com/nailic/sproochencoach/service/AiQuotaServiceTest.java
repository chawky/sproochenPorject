package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.config.AiQuotaProperties;
import com.nailic.sproochencoach.dto.AiQuotaFeatureStatusDto;
import com.nailic.sproochencoach.dto.AiQuotaStatusDto;
import com.nailic.sproochencoach.exceptions.AiQuotaExceededException;
import com.nailic.sproochencoach.model.AppRole;
import com.nailic.sproochencoach.model.AppUser;
import com.nailic.sproochencoach.repository.AppUserRepo;
import com.nailic.sproochencoach.repository.ExerciseAttemptRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiQuotaServiceTest {
    private static final Integer USER_ID = 42;
    private static final ZoneId ZONE = ZoneId.of("Europe/Paris");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-10T08:00:00Z"), ZONE);
    private static final LocalDate ANCHOR = LocalDate.of(2026, 9, 1);
    private static final LocalDateTime WINDOW_START = LocalDateTime.of(2026, 9, 8, 0, 0);
    private static final LocalDateTime WINDOW_END = LocalDateTime.of(2026, 9, 15, 0, 0);

    @Mock private ExerciseAttemptRepo exerciseAttemptRepo;
    @Mock private LoggedInUser loggedInUser;
    @Mock private UserPlanTierResolver userPlanTierResolver;
    @Mock private AppUserRepo appUserRepo;

    @Test
    void basicFeatureLimitsMatchProductRules() {
        AppUser user = basicUser();
        stubCurrentUser(user, UserPlanTier.BASIC);

        AiQuotaStatusDto status = quotaService(CLOCK).getCurrentUserQuotaStatus();
        Map<String, AiQuotaFeatureStatusDto> features = status.getFeatures().stream()
                .collect(Collectors.toMap(AiQuotaFeatureStatusDto::getFeature, Function.identity()));

        assertThat(features).hasSize(5);
        assertThat(features.get("SPEAKING").getWeeklyLimit()).isEqualTo(15);
        assertThat(features.get("LISTENING").getWeeklyLimit()).isEqualTo(10);
        assertThat(features.get("IMAGE_DESCRIPTION").getWeeklyLimit()).isEqualTo(5);
        assertThat(features.get("VOCABULARY").getWeeklyLimit()).isEqualTo(20);
        assertThat(features.get("TOPIC_EXERCISE").getWeeklyLimit()).isEqualTo(15);
        assertThat(features.values()).allSatisfy(feature -> {
            assertThat(feature.getWindow()).isEqualTo("weekly");
            assertThat(feature.getWindowStart()).isEqualTo(WINDOW_START);
            assertThat(feature.getWindowEnd()).isEqualTo(WINDOW_END);
        });
    }

    @Test
    void exhaustedSpeakingBlocksOnlySpeaking() {
        AppUser user = basicUser();
        stubCurrentUser(user, UserPlanTier.BASIC);
        when(count(user, AiQuotaFeature.SPEAKING, WINDOW_START, WINDOW_END)).thenReturn(15L);
        when(count(user, AiQuotaFeature.LISTENING, WINDOW_START, WINDOW_END)).thenReturn(0L);

        AiQuotaService service = quotaService(CLOCK);

        assertThatThrownBy(() -> service.checkCurrentUserQuota(AiQuotaFeature.SPEAKING))
                .isInstanceOf(AiQuotaExceededException.class)
                .hasMessage("You have reached your weekly Speaking limit. Your allowance resets on 2026-09-15.");
        service.checkCurrentUserQuota(AiQuotaFeature.LISTENING);
    }

    @Test
    void weeklyWindowResetsFromPersistedAnchor() {
        AppUser user = basicUser();
        Clock nextWindowClock = Clock.fixed(Instant.parse("2026-09-15T08:00:00Z"), ZONE);
        stubCurrentUser(user, UserPlanTier.BASIC);
        LocalDateTime nextStart = LocalDateTime.of(2026, 9, 15, 0, 0);
        LocalDateTime nextEnd = LocalDateTime.of(2026, 9, 22, 0, 0);
        when(count(user, AiQuotaFeature.SPEAKING, nextStart, nextEnd)).thenReturn(0L);

        quotaService(nextWindowClock).checkCurrentUserQuota(AiQuotaFeature.SPEAKING);

        verify(exerciseAttemptRepo).countByUser_IdAndExerciseTypeAndGeneratedAtGreaterThanEqualAndGeneratedAtLessThan(
                USER_ID, AiQuotaFeature.SPEAKING.exerciseType(), nextStart, nextEnd
        );
        verify(exerciseAttemptRepo, never()).countByUser_IdAndExerciseTypeAndGeneratedAtGreaterThanEqualAndGeneratedAtLessThan(
                USER_ID, AiQuotaFeature.SPEAKING.exerciseType(), WINDOW_START, WINDOW_END
        );
    }

    @Test
    void premiumIsUnlimited() {
        AppUser user = basicUser();
        stubCurrentUser(user, UserPlanTier.PREMIUM);

        AiQuotaService service = quotaService(CLOCK);
        service.checkCurrentUserQuota(AiQuotaFeature.SPEAKING);
        AiQuotaStatusDto status = service.getCurrentUserQuotaStatus();

        assertThat(status.getTier()).isEqualTo("PREMIUM");
        assertThat(status.getFeatures()).allSatisfy(feature -> {
            assertThat(feature.getWindow()).isEqualTo("unlimited");
            assertThat(feature.getWeeklyLimit()).isNull();
            assertThat(feature.getRemaining()).isNull();
            assertThat(feature.getWindowStart()).isNull();
            assertThat(feature.getWindowEnd()).isNull();
        });
        verifyNoInteractions(exerciseAttemptRepo);
    }

    @Test
    void adminIsUnlimitedBecauseAdminResolvesAsPremium() {
        AppUser user = basicUser();
        AppRole admin = new AppRole();
        admin.setName("ADMIN");
        user.getRoles().add(admin);
        stubCurrentUser(user, UserPlanTier.PREMIUM);

        quotaService(CLOCK).checkCurrentUserQuota(AiQuotaFeature.IMAGE_DESCRIPTION);

        verifyNoInteractions(exerciseAttemptRepo);
    }

    private Long count(AppUser user, AiQuotaFeature feature, LocalDateTime start, LocalDateTime end) {
        return exerciseAttemptRepo.countByUser_IdAndExerciseTypeAndGeneratedAtGreaterThanEqualAndGeneratedAtLessThan(
                user.getId(), feature.exerciseType(), start, end
        );
    }

    private void stubCurrentUser(AppUser user, UserPlanTier tier) {
        when(loggedInUser.getId()).thenReturn(USER_ID);
        when(appUserRepo.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userPlanTierResolver.resolve(user)).thenReturn(tier);
    }

    private AppUser basicUser() {
        AppUser user = new AppUser();
        user.setId(USER_ID);
        user.setAiQuotaAnchor(ANCHOR);
        return user;
    }

    private AiQuotaService quotaService(Clock clock) {
        return new AiQuotaService(
                quotaProperties(), exerciseAttemptRepo, loggedInUser,
                userPlanTierResolver, appUserRepo, clock
        );
    }

    private AiQuotaProperties quotaProperties() {
        AiQuotaProperties properties = new AiQuotaProperties();
        properties.getBasic().getSpeaking().setWeeklyLimit(15);
        properties.getBasic().getListening().setWeeklyLimit(10);
        properties.getBasic().getImageDescription().setWeeklyLimit(5);
        properties.getBasic().getVocabulary().setWeeklyLimit(20);
        properties.getBasic().getTopicExercise().setWeeklyLimit(15);
        return properties;
    }
}
