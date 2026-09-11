package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.dto.AdminAuditLogDto;
import com.nailic.sproochencoach.dto.PageResponseDto;
import com.nailic.sproochencoach.model.AdminAuditLog;
import com.nailic.sproochencoach.model.AppUser;
import com.nailic.sproochencoach.repository.AdminAuditLogRepo;
import com.nailic.sproochencoach.repository.AppUserRepo;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AdminAuditService {
    private final AdminAuditLogRepo adminAuditLogRepo;
    private final AppUserRepo appUserRepo;

    public void recordUserStatusChange(
            Integer actorUserId,
            Integer targetUserId,
            boolean oldAdminDisabled,
            boolean newAdminDisabled,
            String reason
    ) {
        recordAction(
                actorUserId,
                targetUserId,
                "USER",
                targetUserId.toString(),
                "USER_STATUS_CHANGED",
                "adminDisabled=" + oldAdminDisabled,
                "adminDisabled=" + newAdminDisabled,
                reason
        );
    }

    public void recordAction(
            Integer actorUserId,
            Integer targetUserId,
            String targetType,
            String targetId,
            String action,
            String oldValue,
            String newValue,
            String reason
    ) {
        AdminAuditLog log = new AdminAuditLog();
        log.setActorUserId(actorUserId);
        log.setTargetUserId(targetUserId);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setAction(action);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        log.setReason(cleanReason(reason));

        adminAuditLogRepo.save(log);
    }

    public PageResponseDto<AdminAuditLogDto> getAuditLogs(
            Integer actorUserId,
            Integer targetUserId,
            String targetType,
            String targetId,
            String action,
            int page,
            int size
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<AdminAuditLog> logs = adminAuditLogRepo.findAll(
                auditFilters(actorUserId, targetUserId, targetType, targetId, action),
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "id"))
        );

        return new PageResponseDto<>(
                logs.getContent().stream().map(this::toDto).toList(),
                logs.getNumber(),
                logs.getSize(),
                logs.getTotalElements(),
                logs.getTotalPages()
        );
    }

    private Specification<AdminAuditLog> auditFilters(
            Integer actorUserId,
            Integer targetUserId,
            String targetType,
            String targetId,
            String action
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            String normalizedAction = cleanReason(action);
            String normalizedTargetType = cleanReason(targetType);
            String normalizedTargetId = cleanReason(targetId);

            if (actorUserId != null) {
                predicates.add(criteriaBuilder.equal(root.get("actorUserId"), actorUserId));
            }

            if (targetUserId != null) {
                predicates.add(criteriaBuilder.equal(root.get("targetUserId"), targetUserId));
            }

            if (normalizedTargetType != null) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("targetType")),
                        normalizedTargetType.toLowerCase(Locale.ROOT)
                ));
            }

            if (normalizedTargetId != null) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("targetId")),
                        normalizedTargetId.toLowerCase(Locale.ROOT)
                ));
            }

            if (normalizedAction != null) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("action")),
                        normalizedAction.toLowerCase(Locale.ROOT)
                ));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private AdminAuditLogDto toDto(AdminAuditLog log) {
        return new AdminAuditLogDto(
                log.getId(),
                log.getActorUserId(),
                log.getTargetUserId(),
                log.getTargetType(),
                log.getTargetId(),
                log.getAction(),
                log.getOldValue(),
                log.getNewValue(),
                log.getReason(),
                log.getCreatedAt(),
                userLabel(log.getActorUserId()),
                targetLabel(log),
                actionLabel(log.getAction()),
                changeSummary(log)
        );
    }

    private String userLabel(Integer userId) {
        if (userId == null) {
            return "System";
        }

        return appUserRepo.findById(userId)
                .map(this::displayName)
                .orElse("Deleted user");
    }

    private String displayName(AppUser user) {
        String fullName = cleanReason((user.getFirstName() == null ? "" : user.getFirstName())
                + " "
                + (user.getLastName() == null ? "" : user.getLastName()));
        if (fullName != null) {
            return fullName;
        }

        String username = cleanReason(user.getUsername());
        if (username != null) {
            return username;
        }

        String email = cleanReason(user.getEmail());
        if (email != null) {
            return email;
        }

        return "User " + user.getId();
    }

    private String targetLabel(AdminAuditLog log) {
        if ("USER".equals(log.getTargetType()) && log.getTargetUserId() != null) {
            return userLabel(log.getTargetUserId());
        }

        return readableActionPart(log.getTargetType()) + " " + log.getTargetId();
    }

    private String actionLabel(String action) {
        return switch (action) {
            case "USER_STATUS_CHANGED" -> "User status changed";
            case "EXERCISE_LEVEL_UPDATED" -> "Exercise level updated";
            case "EXERCISE_LEVEL_DELETED" -> "Exercise level deleted";
            case "EXERCISE_TOPIC_UPDATED" -> "Exercise topic updated";
            case "EXERCISE_TOPIC_DELETED" -> "Exercise topic deleted";
            case "EXERCISE_TYPE_UPDATED" -> "Exercise type updated";
            case "EXERCISE_TYPE_DELETED" -> "Exercise type deleted";
            case "PROMPT_UPDATED" -> "Prompt updated";
            case "PROMPT_DELETED" -> "Prompt deleted";
            default -> readableActionPart(action);
        };
    }

    private String changeSummary(AdminAuditLog log) {
        if ("USER_STATUS_CHANGED".equals(log.getAction())) {
            if ("adminDisabled=true".equals(log.getNewValue())) {
                return "Account disabled";
            }

            if ("adminDisabled=false".equals(log.getNewValue())) {
                return "Account enabled";
            }
        }

        if (log.getReason() != null) {
            return log.getReason();
        }

        if (log.getNewValue() == null || log.getNewValue().isBlank()) {
            return log.getOldValue();
        }

        return log.getNewValue();
    }

    private String readableActionPart(String value) {
        if (!StringUtils.hasText(value)) {
            return "Unknown";
        }

        String lowerCase = value.toLowerCase(Locale.ROOT).replace('_', ' ');
        return lowerCase.substring(0, 1).toUpperCase(Locale.ROOT) + lowerCase.substring(1);
    }

    private String cleanReason(String reason) {
        if (!StringUtils.hasText(reason)) {
            return null;
        }

        return reason.strip();
    }
}
