package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.config.AiRateLimitProperties;
import com.nailic.sproochencoach.exceptions.AiRateLimitExceededException;
import com.nailic.sproochencoach.model.AppUser;
import com.nailic.sproochencoach.repository.AiRateLimitRequestRepo;
import com.nailic.sproochencoach.repository.AppUserRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiRateLimitServiceTest {
    private static final int USER_ID = 42;
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-01T10:00:00Z"), ZoneOffset.UTC);

    @Mock private AiRateLimitRequestRepo requestRepo;
    @Mock private AppUserRepo appUserRepo;
    @Mock private LoggedInUser loggedInUser;

    @Test
    void technicalLimitAppliesBeforeAnotherPaidOperationForEveryTier() {
        AiRateLimitProperties properties = new AiRateLimitProperties();
        properties.setMaxRequestsPerMinute(1);
        when(loggedInUser.getId()).thenReturn(USER_ID);
        when(appUserRepo.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(new AppUser()));
        when(requestRepo.countByUserIdAndCreatedAtGreaterThanEqual(
                USER_ID, LocalDateTime.of(2026, 10, 1, 9, 59)
        )).thenReturn(1L);

        AiRateLimitService service = new AiRateLimitService(
                properties, requestRepo, appUserRepo, loggedInUser, CLOCK
        );

        assertThatThrownBy(service::checkAndRecordCurrentUserRequest)
                .isInstanceOf(AiRateLimitExceededException.class)
                .hasMessage("Too many AI requests. Please wait a minute and try again.");
        verify(requestRepo, never()).save(any());
    }
}
