package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.dto.AdminAuditLogDto;
import com.nailic.sproochencoach.dto.PageResponseDto;
import com.nailic.sproochencoach.model.AdminAuditLog;
import com.nailic.sproochencoach.repository.AdminAuditLogRepo;
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
                log.getCreatedAt()
        );
    }

    private String cleanReason(String reason) {
        if (!StringUtils.hasText(reason)) {
            return null;
        }

        return reason.strip();
    }
}
