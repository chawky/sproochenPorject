package com.nailic.sproochencoach.controller;

import com.nailic.sproochencoach.dto.AdminExerciseConfigDto;
import com.nailic.sproochencoach.dto.AdminAuditLogDto;
import com.nailic.sproochencoach.dto.AdminAiUsageDto;
import com.nailic.sproochencoach.dto.AdminAiUsageSummaryDto;
import com.nailic.sproochencoach.dto.AdminExerciseTypeConfigRequest;
import com.nailic.sproochencoach.dto.AdminExerciseTypeOptionDto;
import com.nailic.sproochencoach.dto.AdminLevelConfigRequest;
import com.nailic.sproochencoach.dto.AdminLevelOptionDto;
import com.nailic.sproochencoach.dto.AdminTopicConfigRequest;
import com.nailic.sproochencoach.dto.AdminTopicOptionDto;
import com.nailic.sproochencoach.dto.AdminUserDetailDto;
import com.nailic.sproochencoach.dto.AdminUserProgressDto;
import com.nailic.sproochencoach.dto.AdminUserStatusUpdateRequest;
import com.nailic.sproochencoach.dto.AiQuotaStatusDto;
import com.nailic.sproochencoach.dto.ApiResponse;
import com.nailic.sproochencoach.dto.PageResponseDto;
import com.nailic.sproochencoach.dto.ResponseUserDto;
import com.nailic.sproochencoach.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<PageResponseDto<ResponseUserDto>>> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean subscribed,
            @RequestParam(required = false) Boolean adminDisabled,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResponseDto<ResponseUserDto> users =
                adminService.getUsers(search, subscribed, adminDisabled, page, size);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Admin users retrieved successfully",
                        users
                )
        );
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<AdminUserDetailDto>> getUser(
            @PathVariable Integer id
    ) {
        AdminUserDetailDto user = adminService.getUser(id);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Admin user retrieved successfully",
                        user
                )
        );
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<ApiResponse<AdminUserDetailDto>> updateUserStatus(
            @PathVariable Integer id,
            @Valid @RequestBody AdminUserStatusUpdateRequest request
    ) {
        AdminUserDetailDto user = adminService.updateUserStatus(id, request);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Admin user status updated successfully",
                        user
                )
        );
    }

    @GetMapping("/users/{id}/progress")
    public ResponseEntity<ApiResponse<PageResponseDto<AdminUserProgressDto>>> getUserProgress(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResponseDto<AdminUserProgressDto> progress = adminService.getUserProgress(id, page, size);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Admin user progress retrieved successfully",
                        progress
                )
        );
    }

    @GetMapping("/users/{id}/ai-usage/summary")
    public ResponseEntity<ApiResponse<AdminAiUsageSummaryDto>> getUserAiUsageSummary(
            @PathVariable Integer id
    ) {
        AdminAiUsageSummaryDto usage = adminService.getUserAiUsageSummary(id);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Admin user AI usage summary retrieved successfully",
                        usage
                )
        );
    }

    @GetMapping("/users/{id}/ai-quota")
    public ResponseEntity<ApiResponse<AiQuotaStatusDto>> getUserAiQuota(
            @PathVariable Integer id
    ) {
        AiQuotaStatusDto quota = adminService.getUserAiQuota(id);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Admin user AI quota retrieved successfully",
                        quota
                )
        );
    }

    @GetMapping("/users/{id}/ai-usage")
    public ResponseEntity<ApiResponse<PageResponseDto<AdminAiUsageDto>>> getUserAiUsage(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        PageResponseDto<AdminAiUsageDto> usage = adminService.getUserAiUsage(id, page, size, provider, model, from, to);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Admin user AI usage retrieved successfully",
                        usage
                )
        );
    }

    @GetMapping("/exercise-config")
    public ResponseEntity<ApiResponse<AdminExerciseConfigDto>> getExerciseConfig() {
        AdminExerciseConfigDto config = adminService.getExerciseConfig();

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Admin exercise configuration retrieved successfully",
                        config
                )
        );
    }

    @PostMapping("/exercise-config/levels")
    public ResponseEntity<ApiResponse<AdminLevelOptionDto>> createLevel(
            @Valid @RequestBody AdminLevelConfigRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Exercise level created successfully", adminService.createLevel(request)));
    }

    @PutMapping("/exercise-config/levels/{code}")
    public ResponseEntity<ApiResponse<AdminLevelOptionDto>> updateLevel(
            @PathVariable String code,
            @Valid @RequestBody AdminLevelConfigRequest request
    ) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Exercise level updated successfully", adminService.updateLevel(code, request))
        );
    }

    @DeleteMapping("/exercise-config/levels/{code}")
    public ResponseEntity<ApiResponse<Void>> deleteLevel(@PathVariable String code) {
        adminService.deleteLevel(code);
        return ResponseEntity.ok(new ApiResponse<>(true, "Exercise level deleted successfully", null));
    }

    @PostMapping("/exercise-config/topics")
    public ResponseEntity<ApiResponse<AdminTopicOptionDto>> createTopic(
            @Valid @RequestBody AdminTopicConfigRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Exercise topic created successfully", adminService.createTopic(request)));
    }

    @PutMapping("/exercise-config/topics/{code}")
    public ResponseEntity<ApiResponse<AdminTopicOptionDto>> updateTopic(
            @PathVariable String code,
            @Valid @RequestBody AdminTopicConfigRequest request
    ) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Exercise topic updated successfully", adminService.updateTopic(code, request))
        );
    }

    @DeleteMapping("/exercise-config/topics/{code}")
    public ResponseEntity<ApiResponse<Void>> deleteTopic(@PathVariable String code) {
        adminService.deleteTopic(code);
        return ResponseEntity.ok(new ApiResponse<>(true, "Exercise topic deleted successfully", null));
    }

    @PostMapping("/exercise-config/types")
    public ResponseEntity<ApiResponse<AdminExerciseTypeOptionDto>> createType(
            @Valid @RequestBody AdminExerciseTypeConfigRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Exercise type created successfully", adminService.createType(request)));
    }

    @PutMapping("/exercise-config/types/{code}")
    public ResponseEntity<ApiResponse<AdminExerciseTypeOptionDto>> updateType(
            @PathVariable String code,
            @Valid @RequestBody AdminExerciseTypeConfigRequest request
    ) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Exercise type updated successfully", adminService.updateType(code, request))
        );
    }

    @DeleteMapping("/exercise-config/types/{code}")
    public ResponseEntity<ApiResponse<Void>> deleteType(@PathVariable String code) {
        adminService.deleteType(code);
        return ResponseEntity.ok(new ApiResponse<>(true, "Exercise type deleted successfully", null));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<PageResponseDto<AdminAuditLogDto>>> getAuditLogs(
            @RequestParam(required = false) Integer actorUserId,
            @RequestParam(required = false) Integer targetUserId,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String targetId,
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResponseDto<AdminAuditLogDto> logs =
                adminService.getAuditLogs(actorUserId, targetUserId, targetType, targetId, action, page, size);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Admin audit logs retrieved successfully",
                        logs
                )
        );
    }
}
