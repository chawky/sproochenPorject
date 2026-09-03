package com.nailic.sproochencoach.controller;

import com.nailic.sproochencoach.dto.ApiResponse;
import com.nailic.sproochencoach.dto.AudioExerciseDto;
import com.nailic.sproochencoach.dto.ExerciseRequestDto;
import com.nailic.sproochencoach.service.ListeningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/exercises")
public class ListeningController {
    private final ListeningService listeningService;

    @PostMapping("/listening")
    public ResponseEntity<ApiResponse<AudioExerciseDto>> generateListeningAudio(
            @Valid @RequestBody ExerciseRequestDto request
    ) {
        AudioExerciseDto exercise = listeningService.generateListeningExercise(request);

        ApiResponse<AudioExerciseDto> response =
                new ApiResponse<>(
                        true,
                        "Listening exercise generated successfully.",
                        exercise
                );

        return ResponseEntity.ok(response);
    }
}
