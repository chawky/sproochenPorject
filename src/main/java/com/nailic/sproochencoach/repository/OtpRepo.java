package com.nailic.sproochencoach.repository;

import com.nailic.sproochencoach.model.Otp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OtpRepo extends JpaRepository<Otp, Long> {
}
