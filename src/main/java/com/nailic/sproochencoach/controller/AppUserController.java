package com.nailic.sproochencoach.controller;

import com.nailic.sproochencoach.dto.*;
import com.nailic.sproochencoach.model.AppUser;
import com.nailic.sproochencoach.service.AiQuotaService;
import com.nailic.sproochencoach.service.AppUserService;
import com.nailic.sproochencoach.service.EmailAndOtpService;
import com.nailic.sproochencoach.service.LuxembourgLocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class AppUserController {

    private final AppUserService appUserService;
    private final EmailAndOtpService emailAndOtpService;
    private final LuxembourgLocationService luxembourgLocationService;
    private final AiQuotaService aiQuotaService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ResponseUserDto>>> findAll() {
        List<ResponseUserDto> users = appUserService.findAll();

        ApiResponse<List<ResponseUserDto>> response = new ApiResponse<>(
                true,
                "Users retrieved successfully",
                users
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ResponseUserDto>> findById(
            @PathVariable Integer id
    ) {
        ResponseUserDto user = appUserService.findById(id);

        ApiResponse<ResponseUserDto> response = new ApiResponse<>(
                true,
                "User retrieved successfully",
                user
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/locations")
    public ResponseEntity<ApiResponse<List<LocationSuggestionDto>>> searchLocations(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<LocationSuggestionDto> locations = luxembourgLocationService.searchLocations(query, limit);

        ApiResponse<List<LocationSuggestionDto>> response = new ApiResponse<>(
                true,
                "Locations retrieved successfully",
                locations
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/addUser")
    public ResponseEntity<ApiResponse<ResponseUserDto>> createUser(
            @Valid @RequestBody RequestUserDto request
    ) {
        ResponseUserDto createdUser = appUserService.addUser(request);

        ApiResponse<ResponseUserDto> response =
                new ApiResponse<>(
                        true,
                        "User created successfully. Please verify your email.",
                        createdUser
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ResponseUserDto>> updateUser(
            @PathVariable Integer id,
            @Valid @RequestBody RequestUserDto request
    ) {
        ResponseUserDto updatedUser = appUserService.updateUser(id, request);

        ApiResponse<ResponseUserDto> response =
                new ApiResponse<>(
                        true,
                        "User updated successfully",
                        updatedUser
                );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<ResponseUserDto>> login(
            @Valid @RequestBody RequestUserDto request
    ) {
        ResponseUserDto authenticatedUser = appUserService.login(request);

        ApiResponse<ResponseUserDto> response = new ApiResponse<>(
                true,
                "Login successful",
                authenticatedUser
        );

        return ResponseEntity.ok(response);
    }
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<ResponseUserDto>> me(
            Authentication authentication
    ) {
        AppUser user = (AppUser) authentication.getPrincipal();

        ResponseUserDto responseUser = appUserService.findById(user.getId());

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Current user retrieved successfully",
                        responseUser
                )
        );
    }

    @GetMapping("/me/ai-quota")
    public ResponseEntity<ApiResponse<AiQuotaStatusDto>> myAiQuota() {
        AiQuotaStatusDto quotaStatus = aiQuotaService.getCurrentUserQuotaStatus();

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "AI quota retrieved successfully",
                        quotaStatus
                )
        );
    }

    @PostMapping("/sendOtp")
    public ResponseEntity<ApiResponse<Void>> sendOtp(
            @Valid @RequestBody SendOtpRequest request
    ) {
        emailAndOtpService.sendEmailAndSaveOtp(request.getEmail());

        ApiResponse<Void> response = new ApiResponse<>(
                true,
                "If the email exists, a verification code has been sent",
                null
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/resendOtp")
    public ResponseEntity<ApiResponse<Void>> resendOtp(
            @Valid @RequestBody SendOtpRequest request
    ) {
        emailAndOtpService.resendEmailAndSaveOtp(request.getEmail());

        ApiResponse<Void> response = new ApiResponse<>(
                true,
                "If the email exists, a verification code has been sent",
                null
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/verifyOtp")
    public ResponseEntity<ApiResponse<Void>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request
    ) {
        boolean verified = emailAndOtpService.verifyOtp(request);

        if (!verified) {
            ApiResponse<Void> response = new ApiResponse<>(
                    false,
                    "The verification code is invalid, expired, or the maximum number of attempts was reached",
                    null
            );

            return ResponseEntity
                    .badRequest()
                    .body(response);
        }

        ApiResponse<Void> response = new ApiResponse<>(
                true,
                "Email verified successfully",
                null
        );

        return ResponseEntity.ok(response);
    }
}
