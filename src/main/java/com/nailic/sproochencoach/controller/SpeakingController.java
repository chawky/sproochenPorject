package com.nailic.sproochencoach.controller;

import com.nailic.sproochencoach.dto.ApiResponse;
import com.nailic.sproochencoach.dto.ExerciseRequestDto;
import com.nailic.sproochencoach.dto.SpeakingDto;
import com.nailic.sproochencoach.dto.SpeakingEvaluation;
import com.nailic.sproochencoach.service.SpeakingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/exercises")
@RequiredArgsConstructor
public class SpeakingController {
    private final SpeakingService speakingService;
    @PostMapping("/practice")
    public ResponseEntity<ApiResponse<SpeakingDto>> generateSpeakingPrompt(
            @Valid @RequestBody ExerciseRequestDto request
    ) {
        SpeakingDto exercise = speakingService.generateSpeakingPrompt(request);

        ApiResponse<SpeakingDto> response =
                new ApiResponse<>(
                        true,
                        "Exercise generated successfully.",
                        exercise
                );

        return ResponseEntity.ok(response);
    }

    @PostMapping(
            value = "/recording",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<SpeakingEvaluation>> receiveRecording(
            @RequestParam("audio") MultipartFile audio,
            @RequestParam(value = "durationSeconds", required = false) Long durationSeconds,
            @RequestParam(value = "attemptId", required = false) Long attemptId
    ) {

        SpeakingEvaluation exercise = speakingService.generateEvaluation(audio, durationSeconds, attemptId);

        ApiResponse<SpeakingEvaluation> response =
                new ApiResponse<>(
                        true,
                        "Exercise generated successfully.",
                        exercise
                );

        return ResponseEntity.ok(response);
    }
}
