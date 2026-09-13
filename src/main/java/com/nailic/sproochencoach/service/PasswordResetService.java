package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.constants.AppConstants;
import com.nailic.sproochencoach.dto.ResetPasswordRequest;
import com.nailic.sproochencoach.exceptions.PasswordResetRateLimitExceededException;
import com.nailic.sproochencoach.model.AppUser;
import com.nailic.sproochencoach.model.PasswordResetToken;
import com.nailic.sproochencoach.repository.AppUserRepo;
import com.nailic.sproochencoach.repository.PasswordResetTokenRepo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Transactional
@RequiredArgsConstructor
public class PasswordResetService {
    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final int MAX_RESET_ATTEMPTS = 5;

    private final PasswordResetTokenRepo passwordResetTokenRepo;
    private final AppUserRepo appUserRepo;
    private final EmailSender emailSender;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final ConcurrentMap<String, ResetRequestHistory> emailRequestHistories = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ResetRequestHistory> ipRequestHistories = new ConcurrentHashMap<>();
    @Value(AppConstants.PropertyPlaceholders.SECURITY_PASSWORD_RESET_EXPIRATION_MS)
    private long expirationMs;
    @Value(AppConstants.PropertyPlaceholders.SECURITY_PASSWORD_RESET_RESEND_COOLDOWN_MS)
    private long resendCooldownMs;
    @Value(AppConstants.PropertyPlaceholders.SECURITY_PASSWORD_RESET_MAX_REQUESTS_PER_HOUR)
    private int maxRequestsPerHour;
    @Value(AppConstants.PropertyPlaceholders.SECURITY_PASSWORD_RESET_MAX_IP_REQUESTS_PER_HOUR)
    private int maxIpRequestsPerHour;

    public void requestReset(String email, String clientIp) {
        enforceResetRateLimit(email, clientIp);
        AppUser user = appUserRepo.findByEmail(trimEmail(email)).orElse(null);

        if (user == null) {
            return;
        }

        String resetCode = "%06d".formatted(ThreadLocalRandom.current().nextInt(100000, 1_000_000));
        saveResetToken(user, resetCode);

        long expirationMinutes = expirationMs / 60_000;
        String subject = "SproochenCoach - Password Reset";
        String text = """
                Hello,
                
                You requested a password reset for SproochenCoach.
                
                Your password reset code is:
                
                ====================================
                        %s
                ====================================
                
                This code is valid for %d minute%s.
                
                If you did not request a password reset, you can safely ignore this email.
                
                The SproochenCoach Team
                """
                .formatted(
                        resetCode,
                        expirationMinutes,
                        expirationMinutes == 1 ? "" : "s"
                );

        try {
            emailSender.send(user.getEmail(), subject, text);
        } catch (RuntimeException exception) {
            log.error("Failed to send password reset email for user id {}", user.getId(), exception);
            throw exception;
        }
    }

    public boolean resetPassword(ResetPasswordRequest request) {
        AppUser user = appUserRepo.findByEmail(trimEmail(request.getEmail())).orElse(null);
        if (user == null) {
            log.warn("Password reset failed because user does not exist: {}", maskEmail(request.getEmail()));
            return false;
        }

        PasswordResetToken resetToken = passwordResetTokenRepo.findByUser(user).orElse(null);
        if (resetToken == null) {
            log.warn("Password reset failed because no reset token exists for user id {}", user.getId());
            return false;
        }

        if (resetToken.getAttempts() >= MAX_RESET_ATTEMPTS) {
            log.warn("Password reset rejected because max attempts reached for user id {}", user.getId());
            return false;
        }

        LocalDateTime expirationTime = resetToken.getResetCreationDate()
                .plus(Duration.ofMillis(expirationMs));
        if (LocalDateTime.now(clock).isAfter(expirationTime)) {
            log.warn("Password reset failed because reset token expired for user id {}", user.getId());
            return false;
        }

        if (!passwordEncoder.matches(request.getCode(), resetToken.getCodeHash())) {
            resetToken.setAttempts(resetToken.getAttempts() + 1);
            passwordResetTokenRepo.save(resetToken);
            log.warn(
                    "Password reset failed because code was invalid for user id {}. Attempts: {}",
                    user.getId(),
                    resetToken.getAttempts()
            );
            return false;
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        appUserRepo.save(user);
        passwordResetTokenRepo.delete(resetToken);

        return true;
    }

    private void saveResetToken(AppUser user, String resetCode) {
        PasswordResetToken resetToken = passwordResetTokenRepo.findByUser(user).orElseGet(PasswordResetToken::new);
        resetToken.setAttempts(0);
        resetToken.setUser(user);
        resetToken.setCodeHash(passwordEncoder.encode(resetCode));
        resetToken.setResetCreationDate(LocalDateTime.now(clock));

        passwordResetTokenRepo.save(resetToken);
    }

    private void enforceResetRateLimit(String email, String clientIp) {
        LocalDateTime now = LocalDateTime.now(clock);
        enforceIpRateLimit(clientIp, now);
        enforceEmailRateLimit(email, now);
    }

    private void enforceEmailRateLimit(String email, LocalDateTime now) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail.isBlank()) {
            return;
        }

        ResetRequestHistory history = emailRequestHistories.computeIfAbsent(
                normalizedEmail,
                ignored -> new ResetRequestHistory()
        );
        if (!history.tryRecord(now, Duration.ofMillis(resendCooldownMs), maxRequestsPerHour)) {
            log.warn("Password reset request rate limited for email {}", maskEmail(normalizedEmail));
            throw new PasswordResetRateLimitExceededException("Password reset request rate limit exceeded");
        }
    }

    private void enforceIpRateLimit(String clientIp, LocalDateTime now) {
        String normalizedClientIp = normalizeClientIp(clientIp);
        if (normalizedClientIp.isBlank()) {
            return;
        }

        ResetRequestHistory history = ipRequestHistories.computeIfAbsent(
                normalizedClientIp,
                ignored -> new ResetRequestHistory()
        );
        if (!history.tryRecord(now, Duration.ZERO, maxIpRequestsPerHour)) {
            log.warn("Password reset request rate limited for client IP");
            throw new PasswordResetRateLimitExceededException("Password reset request rate limit exceeded");
        }
    }

    private String trimEmail(String email) {
        return email == null
                ? ""
                : email.trim();
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

    private static final class ResetRequestHistory {
        private final Deque<LocalDateTime> requestTimes = new ArrayDeque<>();

        private synchronized boolean tryRecord(
                LocalDateTime now,
                Duration cooldown,
                int maxRequestsPerHour
        ) {
            LocalDateTime hourlyWindowStart = now.minusHours(1);
            while (!requestTimes.isEmpty() && requestTimes.peekFirst().isBefore(hourlyWindowStart)) {
                requestTimes.removeFirst();
            }

            LocalDateTime lastRequestTime = requestTimes.peekLast();
            if (lastRequestTime != null && Duration.between(lastRequestTime, now).compareTo(cooldown) < 0) {
                return false;
            }

            if (requestTimes.size() >= maxRequestsPerHour) {
                return false;
            }

            requestTimes.addLast(now);
            return true;
        }
    }
}
