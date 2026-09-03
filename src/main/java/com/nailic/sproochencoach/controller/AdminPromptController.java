package com.nailic.sproochencoach.controller;

import com.nailic.sproochencoach.dto.AdminPromptCreateRequest;
import com.nailic.sproochencoach.dto.AdminPromptDto;
import com.nailic.sproochencoach.dto.AdminPromptUpdateRequest;
import com.nailic.sproochencoach.dto.ApiResponse;
import com.nailic.sproochencoach.service.PromptTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/prompts")
@RequiredArgsConstructor
public class AdminPromptController {
    private final PromptTemplateService promptTemplateService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminPromptDto>>> getPrompts() {
        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Admin prompts retrieved successfully",
                        promptTemplateService.getPrompts()
                )
        );
    }

    @GetMapping("/{key}")
    public ResponseEntity<ApiResponse<AdminPromptDto>> getPrompt(@PathVariable String key) {
        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Admin prompt retrieved successfully",
                        promptTemplateService.getPrompt(key)
                )
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AdminPromptDto>> createPrompt(
            @Valid @RequestBody AdminPromptCreateRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponse<>(
                        true,
                        "Admin prompt created successfully",
                        promptTemplateService.createPrompt(request)
                ));
    }

    @PutMapping("/{key}")
    public ResponseEntity<ApiResponse<AdminPromptDto>> updatePrompt(
            @PathVariable String key,
            @Valid @RequestBody AdminPromptUpdateRequest request
    ) {
        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Admin prompt updated successfully",
                        promptTemplateService.updatePrompt(key, request)
                )
        );
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<ApiResponse<Void>> deletePrompt(@PathVariable String key) {
        promptTemplateService.deletePrompt(key);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Admin prompt deleted successfully",
                        null
                )
        );
    }
}
