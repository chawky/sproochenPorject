package com.nailic.sproochencoach.repository;

import com.nailic.sproochencoach.model.SupportEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupportEmailRepo extends JpaRepository<SupportEmail, Long> {
    boolean existsByResendEmailId(String resendEmailId);

    Optional<SupportEmail> findByResendEmailId(String resendEmailId);

    List<SupportEmail> findAllByOrderByReceivedAtDesc();
}
