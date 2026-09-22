package com.nailic.sproochencoach.controller;

import com.nailic.sproochencoach.dto.AdminSupportEmailDetailDto;
import com.nailic.sproochencoach.dto.AdminSupportEmailListDto;
import com.nailic.sproochencoach.dto.ApiResponse;
import com.nailic.sproochencoach.service.AdminSupportEmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/support-emails")
@RequiredArgsConstructor
public class AdminSupportEmailController {
    private final AdminSupportEmailService adminSupportEmailService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminSupportEmailListDto>>> list() {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Support emails retrieved successfully",
                adminSupportEmailService.list()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminSupportEmailDetailDto>> get(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Support email retrieved successfully",
                adminSupportEmailService.get(id)
        ));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<AdminSupportEmailDetailDto>> markRead(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Support email marked as read",
                adminSupportEmailService.markRead(id)
        ));
    }
}
