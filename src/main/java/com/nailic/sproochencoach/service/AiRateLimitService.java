package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.config.AiRateLimitProperties;
import com.nailic.sproochencoach.exceptions.AiRateLimitExceededException;
import com.nailic.sproochencoach.exceptions.UserNotFoundException;
import com.nailic.sproochencoach.model.AiRateLimitRequest;
import com.nailic.sproochencoach.repository.AiRateLimitRequestRepo;
import com.nailic.sproochencoach.repository.AppUserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AiRateLimitService {
    private final AiRateLimitProperties properties;
    private final AiRateLimitRequestRepo requestRepo;
    private final AppUserRepo appUserRepo;
    private final LoggedInUser loggedInUser;
    private final Clock clock;

    @Transactional
    public void checkAndRecordCurrentUserRequest() {
        Integer userId = loggedInUser.getId();
        appUserRepo.findByIdForUpdate(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime cutoff = now.minusMinutes(1);
        requestRepo.deleteByUserIdAndCreatedAtLessThan(userId, cutoff);
        if (requestRepo.countByUserIdAndCreatedAtGreaterThanEqual(userId, cutoff)
                >= properties.getMaxRequestsPerMinute()) {
            throw new AiRateLimitExceededException("Too many AI requests. Please wait a minute and try again.");
        }

        AiRateLimitRequest request = new AiRateLimitRequest();
        request.setUserId(userId);
        request.setCreatedAt(now);
        requestRepo.save(request);
    }
}
