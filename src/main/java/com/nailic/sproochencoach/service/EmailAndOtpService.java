package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.constants.AppConstants;
import com.nailic.sproochencoach.dto.VerifyOtpRequest;
import com.nailic.sproochencoach.exceptions.OtpRateLimitExceededException;
import com.nailic.sproochencoach.model.AppUser;
import com.nailic.sproochencoach.model.Otp;
import com.nailic.sproochencoach.repository.AppUserRepo;
import com.nailic.sproochencoach.repository.OtpRepo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@Transactional
@RequiredArgsConstructor
public class EmailAndOtpService {
    private static final Logger log = LoggerFactory.getLogger(EmailAndOtpService.class);
    private final SecureRandom secureRandom = new SecureRandom();

    private final OtpRepo otpRepo;
    private final AppUserRepo appUserRepo;
    private final EmailSender emailSender;
    private final Clock clock;
    private final ConcurrentMap<String, OtpRequestHistory> emailRequestHistories = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, OtpRequestHistory> ipRequestHistories = new ConcurrentHashMap<>();
    @Value(AppConstants.PropertyPlaceholders.SECURITY_OTP_EXPIRATION_MS)
    private long expirationOtp;
    @Value(AppConstants.PropertyPlaceholders.SECURITY_OTP_RESEND_COOLDOWN_MS)
    private long resendCooldownMs;
    @Value(AppConstants.PropertyPlaceholders.SECURITY_OTP_MAX_REQUESTS_PER_HOUR)
    private int maxRequestsPerHour;
    @Value(AppConstants.PropertyPlaceholders.SECURITY_OTP_MAX_IP_REQUESTS_PER_HOUR)
    private int maxIpRequestsPerHour;

    public void sendEmailAndSaveOtp(String to) {
        sendEmailAndSaveOtp(to, null);
    }

    public void sendEmailAndSaveOtp(String to, String clientIp) {
        enforceOtpRateLimit(to, clientIp);
        AppUser user = appUserRepo.findByEmail(to).orElse(null);

        if (user == null) {
            return;
        }

        int randomOtp = randomSixDigitCode();

        saveOtp(user, randomOtp);

        long expirationMinutes = expirationOtp / 60_000;

        String subject = "SproochenCoach - Email Verification";
        String text = """
                Hello,
                
                Thank you for registering with SproochenCoach!
                
                Your verification code is:
                
                ====================================
                        %d
                ====================================
                
                This code is valid for %d minute%s.
                
                If you did not create an account, you can safely ignore this email.
                
                Happy learning!
                
                The SproochenCoach Team
                """
                .formatted(
                        randomOtp,
                        expirationMinutes,
                        expirationMinutes == 1 ? "" : "s"
                );

        try {
            emailSender.send(to, subject, text);
        } catch (RuntimeException exception) {
            log.error("Failed to send verification OTP email for user id {}", user.getId(), exception);
            throw exception;
        }
    }

    public Boolean verifyOtp(VerifyOtpRequest request) {
        AppUser user = appUserRepo.findByEmail(request.getEmail()).orElse(null);
        if (user == null) {
            log.warn("OTP verification failed because user does not exist: {}", maskEmail(request.getEmail()));
            return false;
        }
        Otp otp = otpRepo.findByUser(user)
                .orElse(null);
        if (otp == null) {
            log.warn("OTP verification failed because no OTP exists for user id {}", user.getId());
            return false;
        }
        if (otp.getAttempts() >= 5) {
            log.warn("OTP verification rejected because max attempts reached for user id {}", user.getId());
            return false;
        }
        LocalDateTime expirationTime =
                otp.getOtpCreationDate()
                        .plus(Duration.ofMillis(expirationOtp));
        if (LocalDateTime.now(clock).isAfter(expirationTime)) {
            log.warn("OTP verification failed because OTP expired for user id {}", user.getId());
            return false;
        }
        if (otp.getOtp() != request.getOtp()) {
            otp.setAttempts(otp.getAttempts() + 1);
            otpRepo.save(otp);
            log.warn("OTP verification failed because code was invalid for user id {}. Attempts: {}", user.getId(), otp.getAttempts());
            return false;
        }
        user.setEnabled(true);
        appUserRepo.save(user);
        otpRepo.delete(otp);

        return true;
    }

    public void resendEmailAndSaveOtp(String email) {
        resendEmailAndSaveOtp(email, null);
    }

    public void resendEmailAndSaveOtp(String email, String clientIp) {
        enforceOtpRateLimit(email, clientIp);
        AppUser user = appUserRepo.findByEmail(email).orElse(null);

        if (user == null) {
            return;
        }

        int randomOtp = randomSixDigitCode();

        saveOtp(user, randomOtp);

        long expirationMinutes = expirationOtp / 60_000;

        String subject = "SproochenCoach - New Verification Code";
        String text = """
            Hello,

            You requested a new verification code for SproochenCoach.

            Your new verification code is:

            ====================================
                    %d
            ====================================

            This code is valid for %d minute%s.

            Your previous verification code is no longer valid.

            If you did not request a new code, you can safely ignore this email.

            Happy learning!

            The SproochenCoach Team
            """
                .formatted(
                        randomOtp,
                        expirationMinutes,
                        expirationMinutes == 1 ? "" : "s"
                );

        try {
            emailSender.send(email, subject, text);
        } catch (RuntimeException exception) {
            log.error("Failed to send OTP resend email for user id {}", user.getId(), exception);
            throw exception;
        }
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

    private void enforceOtpRateLimit(String email, String clientIp) {
        LocalDateTime now = LocalDateTime.now(clock);
        enforceIpRateLimit(clientIp, now);
        enforceEmailRateLimit(email, now);
    }

    private void enforceEmailRateLimit(String email, LocalDateTime now) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail.isBlank()) {
            return;
        }

        OtpRequestHistory history = emailRequestHistories.computeIfAbsent(
                normalizedEmail,
                ignored -> new OtpRequestHistory()
        );
        if (!history.tryRecord(now, Duration.ofMillis(resendCooldownMs), maxRequestsPerHour)) {
            log.warn("OTP request rate limited for email {}", maskEmail(normalizedEmail));
            throw new OtpRateLimitExceededException("OTP request rate limit exceeded");
        }
    }

    private void enforceIpRateLimit(String clientIp, LocalDateTime now) {
        String normalizedClientIp = normalizeClientIp(clientIp);
        if (normalizedClientIp.isBlank()) {
            return;
        }

        OtpRequestHistory history = ipRequestHistories.computeIfAbsent(
                normalizedClientIp,
                ignored -> new OtpRequestHistory()
        );
        if (!history.tryRecord(now, Duration.ZERO, maxIpRequestsPerHour)) {
            log.warn("OTP request rate limited for client IP");
            throw new OtpRateLimitExceededException("OTP request rate limit exceeded");
        }
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

    private void saveOtp(AppUser user, int otpCode) {
        Otp otp = otpRepo.findByUser(user).orElseGet(Otp::new);
        otp.setAttempts(0);
        otp.setUser(user);
        otp.setOtp(otpCode);
        otp.setOtpCreationDate(LocalDateTime.now(clock));

        otpRepo.save(otp);
    }

    private int randomSixDigitCode() {
        return secureRandom.nextInt(900_000) + 100_000;
    }

    private static final class OtpRequestHistory {
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
