package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.dto.VerifyOtpRequest;
import com.nailic.sproochencoach.model.AppUser;
import com.nailic.sproochencoach.model.Otp;
import com.nailic.sproochencoach.repository.AppUserRepo;
import com.nailic.sproochencoach.repository.OtpRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Transactional
@RequiredArgsConstructor
public class EmailAndOtpService {
    private final OtpRepo otpRepo;
    private final AppUserRepo appUserRepo;
    private final JavaMailSender mailSender;
    @Value("${spring.mail.username}")
    private String from;
    @Value("${security.otp.expiration-ms}")
    private long expirationOtp;

    public void sendEmailAndSaveOtp(String to) {
        AppUser user = appUserRepo.findByEmail(to).orElse(null);

        if (user == null) {
            return;
        }

        int randomOtp = ThreadLocalRandom.current()
                .nextInt(100000, 1_000_000);

        Otp otp = new Otp();
        otp.setAttempts(0);
        otp.setUser(user);
        otp.setOtp(randomOtp);
        otp.setOtpCreationDate(LocalDateTime.now());

        otpRepo.save(otp);

        long expirationMinutes = expirationOtp / 60_000;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setFrom(from);
        message.setSubject("SproochenCoach - Email Verification");

        message.setText("""
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
                ));

        mailSender.send(message);
    }

    public Boolean verifyOtp(VerifyOtpRequest request) {
        AppUser user = appUserRepo.findByEmail(request.getEmail()).orElse(null);
        if (user == null) {
            return false;
        }
        Otp otp = otpRepo.findByUser(user)
                .orElse(null);
        if (otp == null) {
            return false;
        }
        if (otp.getAttempts() >= 5) {
            return false;
        }
        LocalDateTime expirationTime =
                otp.getOtpCreationDate()
                        .plus(Duration.ofMillis(expirationOtp));
        if (LocalDateTime.now().isAfter(expirationTime)) {
            return false;
        }
        if (otp.getOtp() != request.getOtp()) {
            otp.setAttempts(otp.getAttempts() + 1);
            otpRepo.save(otp);
            return false;
        }
        user.setEnabled(true);
        appUserRepo.save(user);
        otpRepo.delete(otp);

        return true;
    }

    public void resendEmailAndSaveOtp(String email) {
        AppUser user = appUserRepo.findByEmail(email).orElse(null);

        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }

        Otp otp = otpRepo.findByUser(user)
                .orElseThrow(() ->
                        new IllegalStateException("OTP not found")
                );

        int randomOtp = ThreadLocalRandom.current()
                .nextInt(100000, 1_000_000);

        otp.setOtp(randomOtp);
        otp.setAttempts(0);
        otp.setOtpCreationDate(LocalDateTime.now());

        otpRepo.save(otp);

        long expirationMinutes = expirationOtp / 60_000;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setFrom(from);
        message.setSubject("SproochenCoach - New Verification Code");

        message.setText("""
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
                ));

        mailSender.send(message);
    }
}
