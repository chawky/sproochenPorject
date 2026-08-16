package com.nailic.sproochencoach.controller;

import com.nailic.sproochencoach.dto.ApiResponse;
import com.nailic.sproochencoach.dto.ExerciseRequestDto;
import com.nailic.sproochencoach.dto.GeneratedExerciseDto;
import com.nailic.sproochencoach.service.ExerciseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exercises")
@RequiredArgsConstructor
public class ExerciseController {
    private final ExerciseService exerciseService;
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<GeneratedExerciseDto>> generateExercise(
            @RequestBody  ExerciseRequestDto request
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
