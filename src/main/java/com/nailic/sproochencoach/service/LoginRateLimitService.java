package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.constants.AppConstants;
import com.nailic.sproochencoach.exceptions.LoginRateLimitExceededException;
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
public class LoginRateLimitService {
    private static final Logger log = LoggerFactory.getLogger(LoginRateLimitService.class);

    private final Clock clock;
    private final ConcurrentMap<String, FailedLoginHistory> emailFailures = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, FailedLoginHistory> ipFailures = new ConcurrentHashMap<>();
    @Value(AppConstants.PropertyPlaceholders.SECURITY_LOGIN_FAILED_ATTEMPT_WINDOW_MS)
    private long failedAttemptWindowMs;
    @Value(AppConstants.PropertyPlaceholders.SECURITY_LOGIN_MAX_FAILED_EMAIL_ATTEMPTS)
    private int maxFailedEmailAttempts;
    @Value(AppConstants.PropertyPlaceholders.SECURITY_LOGIN_MAX_FAILED_IP_ATTEMPTS)
    private int maxFailedIpAttempts;

    public void checkAllowed(String email, String clientIp) {
        LocalDateTime now = LocalDateTime.now(clock);
        Duration window = Duration.ofMillis(failedAttemptWindowMs);

        FailedLoginHistory emailHistory = emailFailures.get(normalizeEmail(email));
        if (emailHistory != null && emailHistory.countRecent(now, window) >= maxFailedEmailAttempts) {
            log.warn("Login rate limited for email {}", maskEmail(email));
            throw new LoginRateLimitExceededException("Login rate limit exceeded");
        }

        FailedLoginHistory ipHistory = ipFailures.get(normalizeClientIp(clientIp));
        if (ipHistory != null && ipHistory.countRecent(now, window) >= maxFailedIpAttempts) {
            log.warn("Login rate limited for client IP");
            throw new LoginRateLimitExceededException("Login rate limit exceeded");
        }
    }

    public void recordFailure(String email, String clientIp) {
        LocalDateTime now = LocalDateTime.now(clock);
        recordFailure(emailFailures, normalizeEmail(email), now);
        recordFailure(ipFailures, normalizeClientIp(clientIp), now);
    }

    public void recordSuccess(String email) {
        emailFailures.remove(normalizeEmail(email));
    }

    private void recordFailure(
            ConcurrentMap<String, FailedLoginHistory> histories,
            String key,
            LocalDateTime now
    ) {
        if (key.isBlank()) {
            return;
        }

        histories.computeIfAbsent(key, ignored -> new FailedLoginHistory())
                .record(now, Duration.ofMillis(failedAttemptWindowMs));
    }

    private String normalizeEmail(String email) {
        return email == null
                ? ""
                : email.trim().toLowerCase(Locale.ROOT);
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

    private static final class FailedLoginHistory {
        private final Deque<LocalDateTime> failureTimes = new ArrayDeque<>();

        private synchronized int countRecent(LocalDateTime now, Duration window) {
            prune(now, window);
            return failureTimes.size();
        }

        private synchronized void record(LocalDateTime now, Duration window) {
            prune(now, window);
            failureTimes.addLast(now);
        }

        private void prune(LocalDateTime now, Duration window) {
            LocalDateTime windowStart = now.minus(window);
            while (!failureTimes.isEmpty() && failureTimes.peekFirst().isBefore(windowStart)) {
                failureTimes.removeFirst();
            }
        }
    }
}
