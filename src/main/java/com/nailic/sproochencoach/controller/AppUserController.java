package com.nailic.sproochencoach.controller;

import com.nailic.sproochencoach.dto.*;
import com.nailic.sproochencoach.model.AppUser;
import com.nailic.sproochencoach.service.AppUserService;
import com.nailic.sproochencoach.service.EmailAndOtpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class AppUserController {

    private final AppUserService appUserService;
    private final EmailAndOtpService emailAndOtpService;
    private final ModelMapper mapper;

    @GetMapping
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

    @PostMapping("/addUser")
    public ResponseEntity<ApiResponse<ResponseUserDto>> createUser(
            @RequestBody RequestUserDto request
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

        ResponseUserDto responseUser =
                mapper.map(user, ResponseUserDto.class);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Current user retrieved successfully",
                        responseUser
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