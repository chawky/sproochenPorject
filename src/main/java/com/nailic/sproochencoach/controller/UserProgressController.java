package com.nailic.sproochencoach.controller;

import com.nailic.sproochencoach.dto.ApiResponse;
import com.nailic.sproochencoach.dto.ProgressDashboardDto;
import com.nailic.sproochencoach.service.UserProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
public class UserProgressController {
    private final UserProgressService userProgressService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<ProgressDashboardDto>> getMyProgress() {
        ProgressDashboardDto progress = userProgressService.getCurrentUserProgress();

        ApiResponse<ProgressDashboardDto> response = new ApiResponse<>(
                true,
                "Progress retrieved successfully.",
                progress
        );

        return ResponseEntity.ok(response);
    }
}
