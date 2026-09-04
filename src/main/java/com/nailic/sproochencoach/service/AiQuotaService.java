package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.config.AiQuotaProperties;
import com.nailic.sproochencoach.config.AiQuotaProperties.FeatureQuota;
import com.nailic.sproochencoach.config.AiQuotaProperties.TierQuota;
import com.nailic.sproochencoach.dto.AiQuotaCategoryStatusDto;
import com.nailic.sproochencoach.dto.AiQuotaStatusDto;
import com.nailic.sproochencoach.exceptions.AiQuotaExceededException;
import com.nailic.sproochencoach.exceptions.UserNotFoundException;
import com.nailic.sproochencoach.model.AppUser;
import com.nailic.sproochencoach.repository.AppUserRepo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AiQuotaService {
    private static final Logger log = LoggerFactory.getLogger(AiQuotaService.class);

    private final AiQuotaProperties aiQuotaProperties;
    private final AiUsageService aiUsageService;
    private final LoggedInUser loggedInUser;
    private final UserPlanTierResolver userPlanTierResolver;
    private final AppUserRepo appUserRepo;
    private final Clock clock;

    @Transactional(readOnly = true)
    public void checkCurrentUserQuota(AiQuotaCategory category) {
        Integer userId = loggedInUser.getId();
        UserPlanTier tier = userPlanTierResolver.currentUserTier();
        QuotaWindow quotaWindow = quotaWindow(tier, category);

        if (quotaWindow.limit() == null) {
            return;
        }

        long used = aiUsageService.countUserQuotaUsage(
                userId,
                category,
                quotaWindow.fromInclusive(),
                quotaWindow.toExclusive()
        );

        if (used < quotaWindow.limit()) {
            return;
        }

        log.warn(
                "AI quota rejected. userId={}, category={}, tier={}, window={}, used={}, limit={}",
                userId,
                category,
                tier,
                quotaWindow.windowName(),
                used,
                quotaWindow.limit()
        );

        throw new AiQuotaExceededException(errorMessage(tier, category, quotaWindow.windowName()));
    }

    @Transactional(readOnly = true)
    public AiQuotaStatusDto getCurrentUserQuotaStatus() {
        AppUser user = appUserRepo.findById(loggedInUser.getId())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        return getQuotaStatus(user);
    }

    @Transactional(readOnly = true)
    public AiQuotaStatusDto getUserQuotaStatus(Integer userId) {
        AppUser user = appUserRepo.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        return getQuotaStatus(user);
    }

    private AiQuotaStatusDto getQuotaStatus(AppUser user) {
        UserPlanTier tier = userPlanTierResolver.resolve(user);
        return new AiQuotaStatusDto(
                tier.name(),
                Arrays.stream(AiQuotaCategory.values())
                        .map(category -> categoryStatus(user.getId(), tier, category))
                        .toList()
        );
    }

    private AiQuotaCategoryStatusDto categoryStatus(Integer userId, UserPlanTier tier, AiQuotaCategory category) {
        QuotaWindow quotaWindow = quotaWindow(tier, category);
        long used = aiUsageService.countUserQuotaUsage(
                userId,
                category,
                quotaWindow.fromInclusive(),
                quotaWindow.toExclusive()
        );
        Long remaining = quotaWindow.limit() == null
                ? null
                : Math.max(quotaWindow.limit() - used, 0);

        return new AiQuotaCategoryStatusDto(
                category.name(),
                quotaWindow.windowName(),
                quotaWindow.limit(),
                used,
                remaining,
                quotaWindow.fromInclusive(),
                quotaWindow.toExclusive()
        );
    }

    private QuotaWindow quotaWindow(UserPlanTier tier, AiQuotaCategory category) {
        FeatureQuota featureQuota = featureQuota(tier, category);

        if (tier == UserPlanTier.PREMIUM) {
            if (featureQuota.getMonthlyLimit() != null) {
                LocalDateTime fromInclusive = YearMonth.now(clock).atDay(1).atStartOfDay();
                return new QuotaWindow(
                        "monthly",
                        featureQuota.getMonthlyLimit(),
                        fromInclusive,
                        fromInclusive.plusMonths(1)
                );
            }

            return dailyQuotaWindow(featureQuota);
        }

        if (featureQuota.getDailyLimit() != null) {
            return dailyQuotaWindow(featureQuota);
        }

        if (featureQuota.getMonthlyLimit() != null) {
            LocalDateTime fromInclusive = YearMonth.now(clock).atDay(1).atStartOfDay();
            return new QuotaWindow(
                    "monthly",
                    featureQuota.getMonthlyLimit(),
                    fromInclusive,
                    fromInclusive.plusMonths(1)
            );
        }

        return new QuotaWindow("unlimited", null, null, null);
    }

    private QuotaWindow dailyQuotaWindow(FeatureQuota featureQuota) {
        LocalDateTime fromInclusive = LocalDate.now(clock).atStartOfDay();
        return new QuotaWindow(
                "daily",
                featureQuota.getDailyLimit(),
                fromInclusive,
                fromInclusive.plusDays(1)
        );
    }

    private FeatureQuota featureQuota(UserPlanTier tier, AiQuotaCategory category) {
        TierQuota tierQuota = tier == UserPlanTier.PREMIUM
                ? aiQuotaProperties.getPremium()
                : aiQuotaProperties.getBasic();

        return switch (category) {
            case CHAT -> tierQuota.getChat();
            case TTS -> tierQuota.getTts();
            case STT -> tierQuota.getStt();
            case IMAGE -> tierQuota.getImage();
        };
    }

    private String errorMessage(UserPlanTier tier, AiQuotaCategory category, String windowName) {
        String readableCategory = category.name().toLowerCase(Locale.ROOT);
        String readableWindow = windowName.substring(0, 1).toUpperCase(Locale.ROOT) + windowName.substring(1);

        if (tier == UserPlanTier.PREMIUM) {
            return readableWindow + " premium AI limit reached for " + readableCategory;
        }

        return readableWindow + " AI limit reached for " + readableCategory;
    }

    private record QuotaWindow(
            String windowName,
            Integer limit,
            LocalDateTime fromInclusive,
            LocalDateTime toExclusive
    ) {
    }
}
