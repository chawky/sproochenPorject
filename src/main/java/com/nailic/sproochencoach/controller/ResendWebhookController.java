package com.nailic.sproochencoach.controller;

import com.nailic.sproochencoach.dto.ApiResponse;
import com.nailic.sproochencoach.service.SupportEmailWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhooks/resend")
@RequiredArgsConstructor
public class ResendWebhookController {
    private final SupportEmailWebhookService supportEmailWebhookService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> handle(
            @RequestBody String payload,
            @RequestHeader HttpHeaders headers
    ) {
        supportEmailWebhookService.handle(payload, headers);

        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Webhook processed successfully",
                null
        ));
    }
}
