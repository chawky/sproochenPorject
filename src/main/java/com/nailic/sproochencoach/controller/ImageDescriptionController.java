package com.nailic.sproochencoach.controller;

import com.nailic.sproochencoach.dto.ApiResponse;
import com.nailic.sproochencoach.dto.ExerciseRequestDto;
import com.nailic.sproochencoach.dto.GeneratedImageDto;
import com.nailic.sproochencoach.dto.SpeakingEvaluation;
import com.nailic.sproochencoach.service.ImageDescriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/exercises")
@RequiredArgsConstructor
public class ImageDescriptionController {
    private final ImageDescriptionService imageDescriptionService;
    @PostMapping("/generate-image")
    public ResponseEntity<ApiResponse<GeneratedImageDto>> generateImage(
            @RequestBody ExerciseRequestDto request
    ) {
        GeneratedImageDto image = imageDescriptionService.generateImage(request);

        ApiResponse<GeneratedImageDto> response =
                new ApiResponse<>(
                        true,
                        "Exercise generated successfully.",
                        image
                );

        return ResponseEntity.ok(response);
    }

    @PostMapping(
            value = "/image-description/recording",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<SpeakingEvaluation>> receiveImageDescriptionRecording(
            @RequestParam("audio") MultipartFile audio,
            @RequestParam("imageDescription") String imageDescription
    ) {

        SpeakingEvaluation evaluation = imageDescriptionService.generateEvaluation(audio, imageDescription);

        ApiResponse<SpeakingEvaluation> response =
                new ApiResponse<>(
                        true,
                        "Image description evaluated successfully.",
                        evaluation
                );

        return ResponseEntity.ok(response);
    }
}
