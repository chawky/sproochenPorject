package com.nailic.sproochencoach.controller;

import com.nailic.sproochencoach.dto.ApiResponse;
import com.nailic.sproochencoach.dto.ExerciseRequestDto;
import com.nailic.sproochencoach.dto.GeneratedExerciseDto;
import com.nailic.sproochencoach.dto.RetryExerciseResponse;
import com.nailic.sproochencoach.service.UserProgressService;
import com.nailic.sproochencoach.service.ExerciseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exercises")
@RequiredArgsConstructor
public class ExerciseController {
    private final ExerciseService exerciseService;
    private final UserProgressService userProgressService;

    @PostMapping("/{attemptId}/retry")
    public ResponseEntity<ApiResponse<RetryExerciseResponse>> retryExercise(@PathVariable Long attemptId) {
        return ResponseEntity.ok(new ApiResponse<>(
                true, "Exercise retry created successfully.",
                userProgressService.retryCurrentUserExercise(attemptId)
        ));
    }

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<GeneratedExerciseDto>> generateExercise(
            @Valid @RequestBody ExerciseRequestDto request
    ) {
        GeneratedExerciseDto exercise = exerciseService.generateExercise(request);

        ApiResponse<GeneratedExerciseDto> response =
                new ApiResponse<>(
                        true,
                        "Exercise generated successfully.",
                        exercise
                );

        return ResponseEntity.ok(response);
    }

}
