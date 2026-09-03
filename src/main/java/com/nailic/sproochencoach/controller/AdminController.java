package com.nailic.sproochencoach.controller;

import com.nailic.sproochencoach.dto.AdminExerciseConfigDto;
import com.nailic.sproochencoach.dto.AdminAiUsageDto;
import com.nailic.sproochencoach.dto.AdminAiUsageSummaryDto;
import com.nailic.sproochencoach.dto.AdminUserDetailDto;
import com.nailic.sproochencoach.dto.AdminUserProgressDto;
import com.nailic.sproochencoach.dto.AdminUserStatusUpdateRequest;
import com.nailic.sproochencoach.dto.ApiResponse;
import com.nailic.sproochencoach.dto.PageResponseDto;
import com.nailic.sproochencoach.dto.ResponseUserDto;
import com.nailic.sproochencoach.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @GetMapping("/users/{id}/ai-usage")
    public ResponseEntity<ApiResponse<PageResponseDto<AdminAiUsageDto>>> getUserAiUsage(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResponseDto<AdminAiUsageDto> usage = adminService.getUserAiUsage(id, page, size);

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
}
