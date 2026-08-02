package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.model.AppUser;
import com.nailic.sproochencoach.model.Otp;
import com.nailic.sproochencoach.repository.AppUserRepo;
import com.nailic.sproochencoach.repository.OtpRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private int expirationOtp;

    public void sendEmailAndSaveOtp(String to) {
        AppUser user = appUserRepo.findByEmail(to);

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

}
