package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.exceptions.OtpRateLimitExceededException;
import com.nailic.sproochencoach.model.AppUser;
import com.nailic.sproochencoach.model.Otp;
import com.nailic.sproochencoach.repository.AppUserRepo;
import com.nailic.sproochencoach.repository.OtpRepo;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailAndOtpServiceTest {
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-09-12T10:00:00Z"),
            ZoneId.of("Europe/Paris")
    );

    @Test
    void sendOtpRejectsSecondRequestInsideCooldown() {
        AppUserRepo appUserRepo = mock(AppUserRepo.class);
        EmailSender emailSender = mock(EmailSender.class);
        AppUser user = new AppUser();
        user.setId(42);

        when(appUserRepo.findByEmail("learner@example.com")).thenReturn(Optional.of(user));

        EmailAndOtpService service = service(appUserRepo, emailSender);
        service.sendEmailAndSaveOtp("learner@example.com");

        assertThatThrownBy(() -> service.sendEmailAndSaveOtp("learner@example.com"))
                .isInstanceOf(OtpRateLimitExceededException.class);
    }

    @Test
    void resendOtpSilentlyReturnsForUnknownEmail() {
        AppUserRepo appUserRepo = mock(AppUserRepo.class);
        EmailSender emailSender = mock(EmailSender.class);
        when(appUserRepo.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        EmailAndOtpService service = service(appUserRepo, emailSender);

        assertThatCode(() -> service.resendEmailAndSaveOtp("missing@example.com"))
                .doesNotThrowAnyException();
        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void sendOtpRejectsWhenIpHourlyLimitIsReached() {
        AppUserRepo appUserRepo = mock(AppUserRepo.class);
        when(appUserRepo.findByEmail(anyString())).thenReturn(Optional.empty());

        EmailAndOtpService service = service(appUserRepo, mock(EmailSender.class));
        ReflectionTestUtils.setField(service, "maxIpRequestsPerHour", 2);

        service.sendEmailAndSaveOtp("first@example.com", "203.0.113.10");
        service.sendEmailAndSaveOtp("second@example.com", "203.0.113.10");

        assertThatThrownBy(() -> service.sendEmailAndSaveOtp("third@example.com", "203.0.113.10"))
                .isInstanceOf(OtpRateLimitExceededException.class);
    }

    @Test
    void verifyOtpUsesInjectedClockForExpiry() {
        AppUser user = new AppUser();
        user.setId(42);
        user.setEmail("learner@example.com");
        Otp otp = new Otp();
        otp.setUser(user);
        otp.setOtp(123456);
        otp.setOtpCreationDate(LocalDateTime.of(2026, 9, 12, 11, 50));

        AppUserRepo appUserRepo = mock(AppUserRepo.class);
        OtpRepo otpRepo = mock(OtpRepo.class);
        when(appUserRepo.findByEmail("learner@example.com")).thenReturn(Optional.of(user));
        when(otpRepo.findByUser(user)).thenReturn(Optional.of(otp));

        EmailAndOtpService service = new EmailAndOtpService(
                otpRepo,
                appUserRepo,
                mock(EmailSender.class),
                CLOCK
        );
        ReflectionTestUtils.setField(service, "expirationOtp", 300000L);

        assertThat(service.verifyOtp(request("learner@example.com", 123456))).isFalse();
        verify(otpRepo, never()).delete(any(Otp.class));
    }

    private EmailAndOtpService service(AppUserRepo appUserRepo, EmailSender emailSender) {
        OtpRepo otpRepo = mock(OtpRepo.class);
        when(otpRepo.findByUser(any(AppUser.class))).thenReturn(Optional.empty());

        EmailAndOtpService service = new EmailAndOtpService(
                otpRepo,
                appUserRepo,
                emailSender,
                CLOCK
        );
        ReflectionTestUtils.setField(service, "expirationOtp", 300000L);
        ReflectionTestUtils.setField(service, "resendCooldownMs", 60000L);
        ReflectionTestUtils.setField(service, "maxRequestsPerHour", 5);
        ReflectionTestUtils.setField(service, "maxIpRequestsPerHour", 30);
        return service;
    }

    private com.nailic.sproochencoach.dto.VerifyOtpRequest request(String email, int otp) {
        com.nailic.sproochencoach.dto.VerifyOtpRequest request = new com.nailic.sproochencoach.dto.VerifyOtpRequest();
        request.setEmail(email);
        request.setOtp(otp);
        return request;
    }
}
