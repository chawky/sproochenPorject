package com.nailic.sproochencoach.repository;

import com.nailic.sproochencoach.model.AppUser;
import com.nailic.sproochencoach.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepo extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByUser(AppUser user);
}
