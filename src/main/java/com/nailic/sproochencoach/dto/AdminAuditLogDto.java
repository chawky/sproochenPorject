package com.nailic.sproochencoach.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AdminAuditLogDto {
    private Long id;
    private Integer actorUserId;
    private Integer targetUserId;
    private String targetType;
    private String targetId;
    private String action;
    private String oldValue;
    private String newValue;
    private String reason;
    private LocalDateTime createdAt;
}
