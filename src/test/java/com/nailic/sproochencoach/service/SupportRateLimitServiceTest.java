package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.exceptions.SupportRateLimitExceededException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.ConcurrentMap;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Test
    void doesNotRecordNewEmailWhenIpIsAlreadyRateLimited() {
        SupportRateLimitService service = service();
        ReflectionTestUtils.setField(service, "maxIpRequestsPerWindow", 2);

        service.checkAllowed("first@example.com", "203.0.113.10");
        service.checkAllowed("second@example.com", "203.0.113.10");

        assertThatThrownBy(() -> service.checkAllowed("third@example.com", "203.0.113.10"))
                .isInstanceOf(SupportRateLimitExceededException.class);

        assertThat(historySize(service, "emailRequestHistories")).isEqualTo(2);
    }

    @Test
    void removesExpiredHistoriesBeforeRecordingNewRequest() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-12T10:00:00Z"));
        SupportRateLimitService service = service(clock);

        service.checkAllowed("old@example.com", "203.0.113.10");
        clock.advanceMillis(3_600_001L);
        service.checkAllowed("new@example.com", "203.0.113.11");

        assertThat(historySize(service, "emailRequestHistories")).isEqualTo(1);
        assertThat(historySize(service, "ipRequestHistories")).isEqualTo(1);
    }

    private SupportRateLimitService service() {
        return service(CLOCK);
    }

    private SupportRateLimitService service(Clock clock) {
        SupportRateLimitService service = new SupportRateLimitService(clock);
        ReflectionTestUtils.setField(service, "windowMs", 3_600_000L);
        ReflectionTestUtils.setField(service, "maxEmailRequestsPerWindow", 5);
        ReflectionTestUtils.setField(service, "maxIpRequestsPerWindow", 20);
        return service;
    }

    private int historySize(SupportRateLimitService service, String fieldName) {
        ConcurrentMap<?, ?> histories = (ConcurrentMap<?, ?>) ReflectionTestUtils.getField(service, fieldName);
        return histories.size();
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        private void advanceMillis(long millis) {
            instant = instant.plusMillis(millis);
        }
    }
}
