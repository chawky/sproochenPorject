package com.nailic.sproochencoach.controller;

import com.nailic.sproochencoach.dto.ApiResponse;
import com.nailic.sproochencoach.dto.CompleteExerciseRequest;
import com.nailic.sproochencoach.dto.ExerciseAttemptDto;
import com.nailic.sproochencoach.dto.ProgressDashboardDto;
import com.nailic.sproochencoach.service.UserProgressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @PostMapping("/exercises/{attemptId}/complete")
    public ResponseEntity<ApiResponse<ExerciseAttemptDto>> completeExercise(
            @PathVariable Long attemptId,
            @Valid @RequestBody(required = false) CompleteExerciseRequest request
    ) {
        ExerciseAttemptDto attempt = userProgressService.completeCurrentUserExercise(attemptId, request);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Exercise completed successfully.",
                        attempt
                )
        );
    }
}
