package com.nailic.sproochencoach.controller;

import com.nailic.sproochencoach.dto.ApiResponse;
import com.nailic.sproochencoach.dto.SupportRequestDto;
import com.nailic.sproochencoach.service.ClientIpResolver;
import com.nailic.sproochencoach.service.SupportService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
public class SupportController {
    private final SupportService supportService;
    private final ClientIpResolver clientIpResolver;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> sendSupportRequest(
            @ModelAttribute SupportRequestDto request,
            HttpServletRequest httpServletRequest
    ) {
        String clientIp = clientIpResolver.resolve(httpServletRequest);
        supportService.sendSupportRequest(
                request.getEmail(),
                request.getSubject(),
                request.getMessage(),
                request.getAttachments(),
                clientIp
        );

        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Support request sent successfully.",
                null
        ));
    }
}
