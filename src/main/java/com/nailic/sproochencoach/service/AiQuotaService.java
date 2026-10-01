package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.config.AiQuotaProperties;
import com.nailic.sproochencoach.config.AiQuotaProperties.BasicQuota;
import com.nailic.sproochencoach.dto.AiQuotaFeatureStatusDto;
import com.nailic.sproochencoach.dto.AiQuotaStatusDto;
import com.nailic.sproochencoach.exceptions.AiQuotaExceededException;
import com.nailic.sproochencoach.exceptions.UserNotFoundException;
import com.nailic.sproochencoach.model.AppUser;
import com.nailic.sproochencoach.repository.AppUserRepo;
import com.nailic.sproochencoach.repository.ExerciseAttemptRepo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class AiQuotaService {
    private static final Logger log = LoggerFactory.getLogger(AiQuotaService.class);
    private static final int WINDOW_DAYS = 7;

    private final AiQuotaProperties aiQuotaProperties;
    private final ExerciseAttemptRepo exerciseAttemptRepo;
    private final LoggedInUser loggedInUser;
    private final UserPlanTierResolver userPlanTierResolver;
    private final AppUserRepo appUserRepo;
    private final Clock clock;

    @Transactional(readOnly = true)
    public void checkCurrentUserQuota(AiQuotaFeature feature) {
        AppUser user = findUser(loggedInUser.getId());
        if (userPlanTierResolver.resolve(user) == UserPlanTier.PREMIUM) {
            return;
        }

        QuotaWindow window = weeklyWindow(user);
        int limit = weeklyLimit(feature);
        long used = countUsage(user.getId(), feature, window);
        if (used < limit) {
            return;
        }

        log.warn(
                "AI product quota rejected. userId={}, feature={}, tier=BASIC, windowStart={}, windowEnd={}, used={}, limit={}",
                user.getId(), feature, window.start(), window.end(), used, limit
        );
        throw new AiQuotaExceededException(
                "You have reached your weekly " + feature.displayName()
                        + " limit. Your allowance resets on " + window.end().toLocalDate() + "."
        );
    }

    @Transactional(readOnly = true)
    public AiQuotaStatusDto getCurrentUserQuotaStatus() {
        return getQuotaStatus(findUser(loggedInUser.getId()));
    }

    @Transactional(readOnly = true)
    public AiQuotaStatusDto getUserQuotaStatus(Integer userId) {
        return getQuotaStatus(findUser(userId));
    }

    private AppUser findUser(Integer userId) {
        return appUserRepo.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    private AiQuotaStatusDto getQuotaStatus(AppUser user) {
        UserPlanTier tier = userPlanTierResolver.resolve(user);
        return new AiQuotaStatusDto(
                tier.name(),
                Arrays.stream(AiQuotaFeature.values())
                        .map(feature -> featureStatus(user, tier, feature))
                        .toList()
        );
    }

    private AiQuotaFeatureStatusDto featureStatus(AppUser user, UserPlanTier tier, AiQuotaFeature feature) {
        if (tier == UserPlanTier.PREMIUM) {
            return new AiQuotaFeatureStatusDto(feature.name(), "unlimited", null, 0, null, null, null);
        }

        QuotaWindow window = weeklyWindow(user);
        int limit = weeklyLimit(feature);
        long used = countUsage(user.getId(), feature, window);
        return new AiQuotaFeatureStatusDto(
                feature.name(),
                "weekly",
                limit,
                used,
                Math.max(limit - used, 0),
                window.start(),
                window.end()
        );
    }

    private long countUsage(Integer userId, AiQuotaFeature feature, QuotaWindow window) {
        return exerciseAttemptRepo.countByUser_IdAndExerciseTypeAndGeneratedAtGreaterThanEqualAndGeneratedAtLessThan(
                userId,
                feature.exerciseType(),
                window.start(),
                window.end()
        );
    }

    private QuotaWindow weeklyWindow(AppUser user) {
        LocalDate anchor = user.getAiQuotaAnchor();
        if (anchor == null) {
            throw new IllegalStateException("AI quota anchor is missing for user " + user.getId());
        }

        LocalDate today = LocalDate.now(clock);
        long elapsedDays = Math.max(ChronoUnit.DAYS.between(anchor, today), 0);
        LocalDate windowStart = anchor.plusDays((elapsedDays / WINDOW_DAYS) * WINDOW_DAYS);
        return new QuotaWindow(windowStart.atStartOfDay(), windowStart.plusDays(WINDOW_DAYS).atStartOfDay());
    }

    private int weeklyLimit(AiQuotaFeature feature) {
        BasicQuota basic = aiQuotaProperties.getBasic();
        return switch (feature) {
            case SPEAKING -> basic.getSpeaking().getWeeklyLimit();
            case LISTENING -> basic.getListening().getWeeklyLimit();
            case IMAGE_DESCRIPTION -> basic.getImageDescription().getWeeklyLimit();
            case VOCABULARY -> basic.getVocabulary().getWeeklyLimit();
            case TOPIC_EXERCISE -> basic.getTopicExercise().getWeeklyLimit();
        };
    }

    private record QuotaWindow(LocalDateTime start, LocalDateTime end) {
    }
}
