package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.dto.AdminAiUsageDto;
import com.nailic.sproochencoach.dto.AdminAiUsageSummaryDto;
import com.nailic.sproochencoach.dto.PageResponseDto;
import com.nailic.sproochencoach.model.AiUsage;
import com.nailic.sproochencoach.model.AppUser;
import com.nailic.sproochencoach.repository.AiUsageRepo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AiUsageService {
    private static final Logger log = LoggerFactory.getLogger(AiUsageService.class);

    private final AiUsageRepo aiUsageRepo;

    public void recordChatUsage(
            String provider,
            String model,
            String requestName,
            Integer inputTokens,
            Integer outputTokens,
            Integer totalTokens
    ) {
        if (inputTokens == null && outputTokens == null && totalTokens == null) {
            return;
        }

        AppUser user = currentUser();
        if (user == null) {
            log.debug("Skipping AI usage recording because no authenticated user is available. request={}", requestName);
            return;
        }

        try {
            AiUsage usage = new AiUsage();
            usage.setUserId(user.getId());
            usage.setProvider(provider);
            usage.setModel(model);
            usage.setRequestName(requestName);
            usage.setInputTokens(inputTokens);
            usage.setOutputTokens(outputTokens);
            usage.setTotalTokens(totalTokens);

            aiUsageRepo.save(usage);
        } catch (RuntimeException exception) {
            log.error("Failed to record AI usage. userId={}, provider={}, model={}, request={}", user.getId(), provider, model, requestName, exception);
        }
    }

    public PageResponseDto<AdminAiUsageDto> getUserAiUsage(Integer userId, int page, int size) {
        int safePage = safePage(page);
        int safeSize = safeSize(size);
        Page<AiUsage> usagePage = aiUsageRepo.findByUserId(
                userId,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "id"))
        );
        List<AdminAiUsageDto> items = usagePage.getContent()
                .stream()
                .map(this::toAdminAiUsageDto)
                .toList();

        return new PageResponseDto<>(
                items,
                usagePage.getNumber(),
                usagePage.getSize(),
                usagePage.getTotalElements(),
                usagePage.getTotalPages()
        );
    }

    public AdminAiUsageSummaryDto getUserAiUsageSummary(Integer userId) {
        List<AiUsage> usageRecords = aiUsageRepo.findAllByUserId(userId);

        return new AdminAiUsageSummaryDto(
                userId,
                usageRecords.size(),
                sumInputTokens(usageRecords),
                sumOutputTokens(usageRecords),
                sumTotalTokens(usageRecords)
        );
    }

    private AdminAiUsageDto toAdminAiUsageDto(AiUsage usage) {
        return new AdminAiUsageDto(
                usage.getId(),
                usage.getProvider(),
                usage.getModel(),
                usage.getRequestName(),
                usage.getInputTokens(),
                usage.getOutputTokens(),
                usage.getTotalTokens(),
                usage.getCreatedAt()
        );
    }

    private long sumInputTokens(List<AiUsage> usageRecords) {
        return usageRecords.stream()
                .map(AiUsage::getInputTokens)
                .filter(Objects::nonNull)
                .mapToLong(Integer::longValue)
                .sum();
    }

    private long sumOutputTokens(List<AiUsage> usageRecords) {
        return usageRecords.stream()
                .map(AiUsage::getOutputTokens)
                .filter(Objects::nonNull)
                .mapToLong(Integer::longValue)
                .sum();
    }

    private long sumTotalTokens(List<AiUsage> usageRecords) {
        return usageRecords.stream()
                .map(AiUsage::getTotalTokens)
                .filter(Objects::nonNull)
                .mapToLong(Integer::longValue)
                .sum();
    }

    private AppUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof AppUser user) {
            return user;
        }

        return null;
    }

    private int safePage(int page) {
        return Math.max(page, 0);
    }

    private int safeSize(int size) {
        int minimumSize = Math.max(size, 1);
        return Math.min(minimumSize, 100);
    }
}
