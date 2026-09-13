package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.exceptions.LoginRateLimitExceededException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginRateLimitServiceTest {
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-09-12T10:00:00Z"),
            ZoneId.of("Europe/Paris")
    );

    @Test
    void checkAllowedRejectsEmailAfterMaxFailedAttempts() {
        LoginRateLimitService service = service(2, 20);

        service.recordFailure("learner@example.com", "203.0.113.10");
        service.recordFailure("learner@example.com", "203.0.113.11");

        assertThatThrownBy(() -> service.checkAllowed("learner@example.com", "203.0.113.12"))
                .isInstanceOf(LoginRateLimitExceededException.class);
    }

    @Test
    void checkAllowedRejectsIpAfterMaxFailedAttempts() {
        LoginRateLimitService service = service(5, 2);

        service.recordFailure("first@example.com", "203.0.113.10");
        service.recordFailure("second@example.com", "203.0.113.10");

        assertThatThrownBy(() -> service.checkAllowed("third@example.com", "203.0.113.10"))
                .isInstanceOf(LoginRateLimitExceededException.class);
    }

    @Test
    void recordSuccessClearsEmailFailures() {
        LoginRateLimitService service = service(2, 20);

        service.recordFailure("learner@example.com", "203.0.113.10");
        service.recordFailure("learner@example.com", "203.0.113.10");
        service.recordSuccess("learner@example.com");

        assertThatCode(() -> service.checkAllowed("learner@example.com", "203.0.113.10"))
                .doesNotThrowAnyException();
    }

    private LoginRateLimitService service(int maxFailedEmailAttempts, int maxFailedIpAttempts) {
        LoginRateLimitService service = new LoginRateLimitService(CLOCK);
        ReflectionTestUtils.setField(service, "failedAttemptWindowMs", 900000L);
        ReflectionTestUtils.setField(service, "maxFailedEmailAttempts", maxFailedEmailAttempts);
        ReflectionTestUtils.setField(service, "maxFailedIpAttempts", maxFailedIpAttempts);
        return service;
    }
}
