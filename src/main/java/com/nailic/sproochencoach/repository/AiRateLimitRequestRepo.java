package com.nailic.sproochencoach.repository;

import com.nailic.sproochencoach.model.AiRateLimitRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface AiRateLimitRequestRepo extends JpaRepository<AiRateLimitRequest, Long> {
    long countByUserIdAndCreatedAtGreaterThanEqual(Integer userId, LocalDateTime cutoff);
    void deleteByUserIdAndCreatedAtLessThan(Integer userId, LocalDateTime cutoff);
}
