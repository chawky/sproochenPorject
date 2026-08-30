package com.nailic.sproochencoach.controller;

import com.nailic.sproochencoach.dto.ApiResponse;
import com.nailic.sproochencoach.dto.StripeSessionURLDto;
import com.nailic.sproochencoach.service.StripePaymentService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class StripePaymentController {
    private final StripePaymentService stripePaymentService;
    private static final Logger log = LoggerFactory.getLogger(StripePaymentController.class);
    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<StripeSessionURLDto>> checkout(Authentication authentication) throws StripeException {
        ApiResponse<StripeSessionURLDto>  response = new ApiResponse<StripeSessionURLDto>(
                true,
                "checkout session retrieved successfully.",
                stripePaymentService.createStripeCheckoutSession(authentication)
        );
        return ResponseEntity.ok(response);
    }
    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) throws SignatureVerificationException {
        log.info("Stripe webhook request received, payload length={}", payload.length());
        stripePaymentService.constructEvent(payload,signature);

        return ResponseEntity.ok().build();
    }
}
