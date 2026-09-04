package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.dto.AdminAiUsageDto;
import com.nailic.sproochencoach.dto.AdminAiUsageSummaryDto;
import com.nailic.sproochencoach.dto.PageResponseDto;
import com.nailic.sproochencoach.model.AiUsage;
import com.nailic.sproochencoach.model.AppUser;
import com.nailic.sproochencoach.repository.AiUsageRepo;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AiUsageService {
    private static final Logger log = LoggerFactory.getLogger(AiUsageService.class);
    private static final String TOKEN_UNIT = "TOKEN";
    private static final String IMAGE_UNIT = "IMAGE";
    private static final String CHARACTER_UNIT = "CHARACTER";
    private static final String AUDIO_SECOND_UNIT = "AUDIO_SECOND";
    private static final String AUDIO_BYTE_UNIT = "AUDIO_BYTE";

    private final AiUsageRepo aiUsageRepo;
    private final AiUsageCostService aiUsageCostService;

    public void recordChatUsage(
            String provider,
            String model,
            String requestName,
            Integer inputTokens,
            Integer outputTokens,
            Integer totalTokens
    ) {
        recordUsage(
                provider,
                model,
                requestName,
                inputTokens,
                outputTokens,
                totalTokens,
                TOKEN_UNIT,
                totalTokens == null ? null : totalTokens.longValue()
        );
    }

    public long countUserQuotaUsage(
            Integer userId,
            AiQuotaCategory category,
            LocalDateTime fromInclusive,
            LocalDateTime toExclusive
    ) {
        return aiUsageRepo.count(quotaUsageFilters(userId, category, fromInclusive, toExclusive));
    }

    public void recordImageUsage(String provider, String model, String requestName) {
        recordUsage(provider, model, requestName, null, null, null, IMAGE_UNIT, 1L);
    }

    public void recordCharacterUsage(String provider, String model, String requestName, String text) {
        long characterCount = text == null ? 0 : text.length();
        recordUsage(provider, model, requestName, null, null, null, CHARACTER_UNIT, characterCount);
    }

    public void recordAudioUploadUsage(String provider, String model, String requestName, long audioBytes) {
        recordUsage(provider, model, requestName, null, null, null, AUDIO_BYTE_UNIT, audioBytes);
    }

    public void recordAudioDurationUsage(String provider, String model, String requestName, long durationSeconds) {
        recordUsage(provider, model, requestName, null, null, null, AUDIO_SECOND_UNIT, durationSeconds);
    }

    private void recordUsage(
            String provider,
            String model,
            String requestName,
            Integer inputTokens,
            Integer outputTokens,
            Integer totalTokens,
            String usageUnit,
            Long usageAmount
    ) {
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
            usage.setUsageUnit(usageUnit);
            usage.setUsageAmount(usageAmount);
            usage.setEstimatedCostUsd(aiUsageCostService.estimateCostUsd(
                    provider,
                    model,
                    inputTokens,
                    outputTokens,
                    usageUnit,
                    usageAmount
            ));

            aiUsageRepo.save(usage);
        } catch (RuntimeException exception) {
            log.error("Failed to record AI usage. userId={}, provider={}, model={}, request={}", user.getId(), provider, model, requestName, exception);
        }
    }

    public PageResponseDto<AdminAiUsageDto> getUserAiUsage(
            Integer userId,
            int page,
            int size,
            String provider,
            String model,
            LocalDateTime from,
            LocalDateTime to
    ) {
        int safePage = safePage(page);
        int safeSize = safeSize(size);
        Page<AiUsage> usagePage = aiUsageRepo.findAll(
                usageFilters(userId, provider, model, from, to),
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
                sumTotalTokens(usageRecords),
                sumEstimatedCostUsd(usageRecords)
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
                usage.getUsageUnit(),
                usage.getUsageAmount(),
                usage.getEstimatedCostUsd(),
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

    private BigDecimal sumEstimatedCostUsd(List<AiUsage> usageRecords) {
        return usageRecords.stream()
                .map(AiUsage::getEstimatedCostUsd)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
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

    private String textOrNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return value.trim();
    }

    private Specification<AiUsage> usageFilters(
            Integer userId,
            String provider,
            String model,
            LocalDateTime from,
            LocalDateTime to
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            String normalizedProvider = textOrNull(provider);
            String normalizedModel = textOrNull(model);

            predicates.add(criteriaBuilder.equal(root.get("userId"), userId));

            if (normalizedProvider != null) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("provider")),
                        normalizedProvider.toLowerCase(Locale.ROOT)
                ));
            }

            if (normalizedModel != null) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("model")),
                        normalizedModel.toLowerCase(Locale.ROOT)
                ));
            }

            if (from != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), from));
            }

            if (to != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), to));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Specification<AiUsage> quotaUsageFilters(
            Integer userId,
            AiQuotaCategory category,
            LocalDateTime fromInclusive,
            LocalDateTime toExclusive
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("userId"), userId));
            predicates.add(quotaCategoryPredicate(category, root, criteriaBuilder));

            if (fromInclusive != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), fromInclusive));
            }

            if (toExclusive != null) {
                predicates.add(criteriaBuilder.lessThan(root.get("createdAt"), toExclusive));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Predicate quotaCategoryPredicate(
            AiQuotaCategory category,
            jakarta.persistence.criteria.Root<AiUsage> root,
            jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder
    ) {
        return switch (category) {
            case CHAT -> criteriaBuilder.lower(root.get("provider")).in(List.of("openrouter", "kimi"));
            case TTS -> criteriaBuilder.equal(criteriaBuilder.lower(root.get("provider")), "elevenlabs");
            case STT -> criteriaBuilder.equal(criteriaBuilder.lower(root.get("provider")), "groq");
            case IMAGE -> criteriaBuilder.lower(root.get("provider")).in(List.of("openrouter-image", "kimi-image"));
        };
    }
}
