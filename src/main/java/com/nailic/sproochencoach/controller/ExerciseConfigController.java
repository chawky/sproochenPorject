package com.nailic.sproochencoach.controller;

import com.nailic.sproochencoach.dto.ApiResponse;
import com.nailic.sproochencoach.dto.ExerciseConfigDto;
import com.nailic.sproochencoach.service.ExerciseConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exercise-config")
@RequiredArgsConstructor
public class ExerciseConfigController {
    private final ExerciseConfigService exerciseConfigService;

    @GetMapping
    public ResponseEntity<ApiResponse<ExerciseConfigDto>> getExerciseConfig() {
        ExerciseConfigDto config = exerciseConfigService.getEnabledConfig();

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Exercise configuration retrieved successfully",
                        config
                )
        );
    }
}
