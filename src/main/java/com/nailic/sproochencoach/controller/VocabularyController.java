package com.nailic.sproochencoach.controller;

import com.nailic.sproochencoach.dto.ApiResponse;
import com.nailic.sproochencoach.dto.VocabularyDto;
import com.nailic.sproochencoach.dto.VocabularyRequestDto;
import com.nailic.sproochencoach.service.VocabularyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exercises")
@RequiredArgsConstructor
public class VocabularyController {
    private final VocabularyService vocabularyService;
    @PostMapping("/vocabulary")
    public ResponseEntity<ApiResponse<VocabularyDto>> generateVocabExercise(
            @Valid @RequestBody VocabularyRequestDto request
    ) {
        VocabularyDto exercise = vocabularyService.generateVocabExercise(request);

        ApiResponse<VocabularyDto> response =
                new ApiResponse<>(
                        true,
                        "Vocab Exercise generated successfully.",
                        exercise
                );

        return ResponseEntity.ok(response);
    }

}
