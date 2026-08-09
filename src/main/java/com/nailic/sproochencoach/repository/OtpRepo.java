package com.nailic.sproochencoach.repository;

import com.nailic.sproochencoach.model.AppUser;
import com.nailic.sproochencoach.model.Otp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpRepo extends JpaRepository<Otp, Long> {
    Otp findByOtp(int otp);

    Optional<Otp> findByUser(AppUser user);
}
