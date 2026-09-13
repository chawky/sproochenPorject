package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.dto.ResetPasswordRequest;
import com.nailic.sproochencoach.exceptions.PasswordResetRateLimitExceededException;
import com.nailic.sproochencoach.model.AppUser;
import com.nailic.sproochencoach.model.PasswordResetToken;
import com.nailic.sproochencoach.repository.AppUserRepo;
import com.nailic.sproochencoach.repository.PasswordResetTokenRepo;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasswordResetServiceTest {
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-09-12T10:00:00Z"),
            ZoneId.of("Europe/Paris")
    );

    @Test
    void requestResetSilentlyReturnsForUnknownEmail() {
        AppUserRepo appUserRepo = mock(AppUserRepo.class);
        EmailSender emailSender = mock(EmailSender.class);
        when(appUserRepo.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        PasswordResetService service = service(appUserRepo, mock(PasswordResetTokenRepo.class), emailSender);

        assertThatCode(() -> service.requestReset("missing@example.com", "203.0.113.10"))
                .doesNotThrowAnyException();
        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void requestResetRejectsSecondRequestInsideCooldown() {
        AppUserRepo appUserRepo = mock(AppUserRepo.class);
        PasswordResetTokenRepo tokenRepo = mock(PasswordResetTokenRepo.class);
        AppUser user = new AppUser();
        user.setId(42);
        user.setEmail("learner@example.com");

        when(appUserRepo.findByEmail("learner@example.com")).thenReturn(Optional.of(user));
        when(tokenRepo.findByUser(user)).thenReturn(Optional.empty());

        PasswordResetService service = service(appUserRepo, tokenRepo, mock(EmailSender.class));
        service.requestReset("learner@example.com", "203.0.113.10");

        assertThatThrownBy(() -> service.requestReset("learner@example.com", "203.0.113.10"))
                .isInstanceOf(PasswordResetRateLimitExceededException.class);
    }

    @Test
    void resetPasswordUpdatesPasswordAndInvalidatesToken() {
        AppUser user = new AppUser();
        user.setId(42);
        user.setEmail("learner@example.com");
        user.setPassword("old-password");
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setCodeHash(passwordEncoder.encode("123456"));
        resetToken.setResetCreationDate(LocalDateTime.now(CLOCK));

        AppUserRepo appUserRepo = mock(AppUserRepo.class);
        PasswordResetTokenRepo tokenRepo = mock(PasswordResetTokenRepo.class);
        when(appUserRepo.findByEmail("learner@example.com")).thenReturn(Optional.of(user));
        when(tokenRepo.findByUser(user)).thenReturn(Optional.of(resetToken));

        PasswordResetService service = service(
                appUserRepo,
                tokenRepo,
                mock(EmailSender.class),
                passwordEncoder
        );

        boolean reset = service.resetPassword(resetRequest("learner@example.com", "123456", "new-password"));

        assertThat(reset).isTrue();
        assertThat(passwordEncoder.matches("new-password", user.getPassword())).isTrue();
        assertThat(user.getTokenVersion()).isEqualTo(1);
        verify(appUserRepo).save(user);
        verify(tokenRepo).delete(resetToken);
    }

    @Test
    void resetPasswordIncrementsAttemptsForWrongCode() {
        AppUser user = new AppUser();
        user.setId(42);
        user.setEmail("learner@example.com");
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setCodeHash(passwordEncoder.encode("123456"));
        resetToken.setResetCreationDate(LocalDateTime.now(CLOCK));

        AppUserRepo appUserRepo = mock(AppUserRepo.class);
        PasswordResetTokenRepo tokenRepo = mock(PasswordResetTokenRepo.class);
        when(appUserRepo.findByEmail("learner@example.com")).thenReturn(Optional.of(user));
        when(tokenRepo.findByUser(user)).thenReturn(Optional.of(resetToken));

        PasswordResetService service = service(
                appUserRepo,
                tokenRepo,
                mock(EmailSender.class),
                passwordEncoder
        );

        boolean reset = service.resetPassword(resetRequest("learner@example.com", "654321", "new-password"));

        assertThat(reset).isFalse();
        assertThat(resetToken.getAttempts()).isEqualTo(1);
        verify(tokenRepo).save(resetToken);
        verify(tokenRepo, never()).delete(any(PasswordResetToken.class));
    }

    private ResetPasswordRequest resetRequest(String email, String code, String newPassword) {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail(email);
        request.setCode(code);
        request.setNewPassword(newPassword);
        return request;
    }

    private PasswordResetService service(
            AppUserRepo appUserRepo,
            PasswordResetTokenRepo tokenRepo,
            EmailSender emailSender
    ) {
        return service(appUserRepo, tokenRepo, emailSender, new BCryptPasswordEncoder());
    }

    private PasswordResetService service(
            AppUserRepo appUserRepo,
            PasswordResetTokenRepo tokenRepo,
            EmailSender emailSender,
            PasswordEncoder passwordEncoder
    ) {
        PasswordResetService service = new PasswordResetService(
                tokenRepo,
                appUserRepo,
                emailSender,
                passwordEncoder,
                CLOCK
        );
        ReflectionTestUtils.setField(service, "expirationMs", 900000L);
        ReflectionTestUtils.setField(service, "resendCooldownMs", 60000L);
        ReflectionTestUtils.setField(service, "maxRequestsPerHour", 5);
        ReflectionTestUtils.setField(service, "maxIpRequestsPerHour", 30);
        return service;
    }
}
