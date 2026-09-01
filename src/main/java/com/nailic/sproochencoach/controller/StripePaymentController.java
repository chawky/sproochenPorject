package com.nailic.sproochencoach.controller;

import com.nailic.sproochencoach.dto.ApiResponse;
import com.nailic.sproochencoach.dto.StripeSessionURLDto;
import com.nailic.sproochencoach.service.StripePaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class StripePaymentController {
    private final StripePaymentService stripePaymentService;

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<StripeSessionURLDto>> checkout() {
        ApiResponse<StripeSessionURLDto> response = new ApiResponse<>(
                true,
                "Checkout session created successfully.",
                stripePaymentService.createStripeCheckoutSession()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {
        stripePaymentService.handleWebhook(payload, signature);

        return ResponseEntity.ok().build();
    }
}
