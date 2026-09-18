package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.exceptions.SupportRateLimitExceededException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SupportRateLimitServiceTest {
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-09-12T10:00:00Z"),
            ZoneId.of("Europe/Paris")
    );

    @Test
    void rejectsRequestsOverIpLimit() {
        SupportRateLimitService service = service();
        ReflectionTestUtils.setField(service, "maxIpRequestsPerWindow", 2);

        service.checkAllowed("first@example.com", "203.0.113.10");
        service.checkAllowed("second@example.com", "203.0.113.10");

        assertThatThrownBy(() -> service.checkAllowed("third@example.com", "203.0.113.10"))
                .isInstanceOf(SupportRateLimitExceededException.class);
    }

    @Test
    void rejectsRequestsOverEmailLimit() {
        SupportRateLimitService service = service();
        ReflectionTestUtils.setField(service, "maxEmailRequestsPerWindow", 2);

        service.checkAllowed("learner@example.com", "203.0.113.10");
        service.checkAllowed("learner@example.com", "203.0.113.11");

        assertThatThrownBy(() -> service.checkAllowed("learner@example.com", "203.0.113.12"))
                .isInstanceOf(SupportRateLimitExceededException.class);
    }

    private SupportRateLimitService service() {
        SupportRateLimitService service = new SupportRateLimitService(CLOCK);
        ReflectionTestUtils.setField(service, "windowMs", 3_600_000L);
        ReflectionTestUtils.setField(service, "maxEmailRequestsPerWindow", 5);
        ReflectionTestUtils.setField(service, "maxIpRequestsPerWindow", 20);
        return service;
    }
}
