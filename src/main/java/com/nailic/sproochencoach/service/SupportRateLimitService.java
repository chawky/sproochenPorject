package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.constants.AppConstants;
import com.nailic.sproochencoach.exceptions.SupportRateLimitExceededException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
public class SupportRateLimitService {
    private static final Logger log = LoggerFactory.getLogger(SupportRateLimitService.class);

    private final Clock clock;
    private final ConcurrentMap<String, RequestHistory> emailRequestHistories = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, RequestHistory> ipRequestHistories = new ConcurrentHashMap<>();
    @Value(AppConstants.PropertyPlaceholders.SUPPORT_RATE_LIMIT_WINDOW_MS)
    private long windowMs;
    @Value(AppConstants.PropertyPlaceholders.SUPPORT_RATE_LIMIT_MAX_EMAIL_REQUESTS_PER_WINDOW)
    private int maxEmailRequestsPerWindow;
    @Value(AppConstants.PropertyPlaceholders.SUPPORT_RATE_LIMIT_MAX_IP_REQUESTS_PER_WINDOW)
    private int maxIpRequestsPerWindow;

    public void checkAllowed(String email, String clientIp) {
        LocalDateTime now = LocalDateTime.now(clock);
        Duration window = Duration.ofMillis(windowMs);

        cleanupStaleHistories(emailRequestHistories, now, window);
        cleanupStaleHistories(ipRequestHistories, now, window);

        enforce(ipRequestHistories, normalizeClientIp(clientIp), maxIpRequestsPerWindow, now, window, false);
        enforce(emailRequestHistories, normalizeEmail(email), maxEmailRequestsPerWindow, now, window, true);
    }

    private void enforce(
            ConcurrentMap<String, RequestHistory> histories,
            String key,
            int maxRequests,
            LocalDateTime now,
            Duration window,
            boolean emailKey
    ) {
        if (key.isBlank()) {
            return;
        }

        RequestHistory history = histories.computeIfAbsent(key, ignored -> new RequestHistory());
        if (!history.tryRecord(now, window, maxRequests)) {
            if (emailKey) {
                log.warn("Support request rate limited for email {}", maskEmail(key));
            } else {
                log.warn("Support request rate limited for client IP");
            }
            throw new SupportRateLimitExceededException("Support request rate limit exceeded");
        }
    }

    private String normalizeEmail(String email) {
        return email == null
                ? ""
                : email.trim().toLowerCase(Locale.ROOT);
    }

    private void cleanupStaleHistories(
            ConcurrentMap<String, RequestHistory> histories,
            LocalDateTime now,
            Duration window
    ) {
        histories.entrySet().removeIf(entry -> entry.getValue().isStale(now, window));
    }

    private String normalizeClientIp(String clientIp) {
        return clientIp == null
                ? ""
                : clientIp.trim();
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "<blank>";
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return "***";
        }

        return email.charAt(0) + "***" + email.substring(atIndex);
    }

    private static final class RequestHistory {
        private final Deque<LocalDateTime> requestTimes = new ArrayDeque<>();

        private synchronized boolean tryRecord(
                LocalDateTime now,
                Duration window,
                int maxRequests
        ) {
            prune(now, window);
            if (requestTimes.size() >= maxRequests) {
                return false;
            }

            requestTimes.addLast(now);
            return true;
        }

        private void prune(LocalDateTime now, Duration window) {
            LocalDateTime windowStart = now.minus(window);
            while (!requestTimes.isEmpty() && requestTimes.peekFirst().isBefore(windowStart)) {
                requestTimes.removeFirst();
            }
        }

        private synchronized boolean isStale(LocalDateTime now, Duration window) {
            prune(now, window);
            return requestTimes.isEmpty();
        }
    }
}
