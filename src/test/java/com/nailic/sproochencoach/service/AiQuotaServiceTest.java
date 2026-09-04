package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.config.AiQuotaProperties;
import com.nailic.sproochencoach.dto.AiQuotaStatusDto;
import com.nailic.sproochencoach.exceptions.AiQuotaExceededException;
import com.nailic.sproochencoach.model.AppUser;
import com.nailic.sproochencoach.repository.AppUserRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiQuotaServiceTest {
    private static final Integer USER_ID = 42;
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-09-04T08:00:00Z"),
            ZoneId.of("Europe/Paris")
    );

    @Mock
    private AiUsageService aiUsageService;

    @Mock
    private LoggedInUser loggedInUser;

    @Mock
    private UserPlanTierResolver userPlanTierResolver;

    @Mock
    private AppUserRepo appUserRepo;

    @Test
    void basicDailyQuotaThrowsWhenLimitIsReached() {
        AiQuotaService service = quotaService();
        when(loggedInUser.getId()).thenReturn(USER_ID);
        when(userPlanTierResolver.currentUserTier()).thenReturn(UserPlanTier.BASIC);
        when(aiUsageService.countUserQuotaUsage(
                eq(USER_ID),
                eq(AiQuotaCategory.CHAT),
                eq(LocalDateTime.of(2026, 9, 4, 0, 0)),
                eq(LocalDateTime.of(2026, 9, 5, 0, 0))
        )).thenReturn(20L);

        assertThatThrownBy(() -> service.checkCurrentUserQuota(AiQuotaCategory.CHAT))
                .isInstanceOf(AiQuotaExceededException.class)
                .hasMessage("Daily AI limit reached for chat");
    }

    @Test
    void premiumQuotaUsesMonthlyWindow() {
        AiQuotaService service = quotaService();
        when(loggedInUser.getId()).thenReturn(USER_ID);
        when(userPlanTierResolver.currentUserTier()).thenReturn(UserPlanTier.PREMIUM);
        when(aiUsageService.countUserQuotaUsage(
                eq(USER_ID),
                eq(AiQuotaCategory.CHAT),
                eq(LocalDateTime.of(2026, 9, 1, 0, 0)),
                eq(LocalDateTime.of(2026, 10, 1, 0, 0))
        )).thenReturn(299L);

        service.checkCurrentUserQuota(AiQuotaCategory.CHAT);

        verify(aiUsageService).countUserQuotaUsage(
                eq(USER_ID),
                eq(AiQuotaCategory.CHAT),
                eq(LocalDateTime.of(2026, 9, 1, 0, 0)),
                eq(LocalDateTime.of(2026, 10, 1, 0, 0))
        );
    }

    @Test
    void quotaStatusReturnsRemainingUsagePerCategory() {
        AiQuotaService service = quotaService();
        AppUser user = new AppUser();
        user.setId(USER_ID);

        when(loggedInUser.getId()).thenReturn(USER_ID);
        when(appUserRepo.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userPlanTierResolver.resolve(user)).thenReturn(UserPlanTier.BASIC);
        when(aiUsageService.countUserQuotaUsage(
                eq(USER_ID),
                eq(AiQuotaCategory.CHAT),
                eq(LocalDateTime.of(2026, 9, 4, 0, 0)),
                eq(LocalDateTime.of(2026, 9, 5, 0, 0))
        )).thenReturn(7L);

        AiQuotaStatusDto status = service.getCurrentUserQuotaStatus();

        assertThat(status.getTier()).isEqualTo("BASIC");
        assertThat(status.getCategories())
                .filteredOn(category -> category.getCategory().equals("CHAT"))
                .singleElement()
                .satisfies(category -> {
                    assertThat(category.getWindow()).isEqualTo("daily");
                    assertThat(category.getLimit()).isEqualTo(20);
                    assertThat(category.getUsed()).isEqualTo(7);
                    assertThat(category.getRemaining()).isEqualTo(13);
                });
    }

    private AiQuotaService quotaService() {
        return new AiQuotaService(
                quotaProperties(),
                aiUsageService,
                loggedInUser,
                userPlanTierResolver,
                appUserRepo,
                CLOCK
        );
    }

    private AiQuotaProperties quotaProperties() {
        AiQuotaProperties properties = new AiQuotaProperties();
        properties.getBasic().getChat().setDailyLimit(20);
        properties.getBasic().getTts().setDailyLimit(5);
        properties.getBasic().getStt().setDailyLimit(5);
        properties.getBasic().getImage().setDailyLimit(0);
        properties.getPremium().getChat().setMonthlyLimit(300);
        properties.getPremium().getTts().setMonthlyLimit(75);
        properties.getPremium().getStt().setMonthlyLimit(75);
        properties.getPremium().getImage().setMonthlyLimit(10);
        return properties;
    }
}
