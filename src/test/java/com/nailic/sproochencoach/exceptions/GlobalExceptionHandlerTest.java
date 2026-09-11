package com.nailic.sproochencoach.exceptions;

import com.nailic.sproochencoach.dto.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void aiProviderErrorsUseFriendlyPublicMessage() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleAiProviderException(
                new AiProviderException(
                        HttpStatus.UNAUTHORIZED.value(),
                        "AI provider returned invalid exercise JSON"
                )
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message())
                .isEqualTo("We could not create your practice activity right now. Please try again.");
    }

    @Test
    void paymentConflictUsesFriendlyPublicMessage() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleStripePaymentException(
                new StripePaymentException(
                        HttpStatus.CONFLICT.value(),
                        "user already subscribed"
                )
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message())
                .isEqualTo("You already have an active subscription.");
    }
}
