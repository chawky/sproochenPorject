package com.nailic.sproochencoach.exceptions;

import com.nailic.sproochencoach.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
//This class contains methods that handle exceptions for all controllers.
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserAlreadyExists(UserAlreadyExistsException exception) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(
                        new ApiResponse<>(
                                false,
                                exception.getMessage(),
                                null
                        )
                );
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotFound(UserNotFoundException exception) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(
                        new ApiResponse<>(
                                false,
                                exception.getMessage(),
                                null
                        )
                );
    }

    @ExceptionHandler(LocationProviderException.class)
    public ResponseEntity<ApiResponse<Void>> handleLocationProviderException(LocationProviderException exception) {
        log.error(
                "Handling LocationProviderException. statusCode={}, message={}",
                exception.getStatusCode(),
                exception.getMessage()
        );

        return ResponseEntity
                .status(exception.getStatusCode())
                .body(
                        new ApiResponse<>(
                                false,
                                exception.getMessage(),
                                null
                        )
                );
    }

    @ExceptionHandler(AiProviderException.class)
    public ResponseEntity<ApiResponse<Void>> handleAiProviderException(
            AiProviderException exception
    ) {
        log.error(
                "Handling AiProviderException. statusCode={}, message={}",
                exception.getStatusCode(),
                exception.getMessage()
        );

        return ResponseEntity
                .status(exception.getStatusCode())
                .body(
                        new ApiResponse<>(
                                false,
                                exception.getMessage(),
                                null
                        )
                );
    }

    @ExceptionHandler(OpenRouterError.class)
    public ResponseEntity<ApiResponse<Void>> handleOpenRouterError(
            OpenRouterError exception
    ) {
        log.error(
                "Handling OpenRouterError. statusCode={}, message={}",
                exception.getStatusCode(),
                exception.getMessage()
        );

        return ResponseEntity
                .status(exception.getStatusCode())
                .body(
                        new ApiResponse<>(
                                false,
                                exception.getMessage(),
                                null
                        )
                );
    }

    @ExceptionHandler(StripeException.class)
    public ResponseEntity<ApiResponse<Void>> handleStripeExceptionError(
            StripeException exception
    ) {
        log.error(
                "Handling StripeException. statusCode={}, message={}",
                exception.getStatusCode(),
                exception.getMessage()
        );

        return ResponseEntity
                .status(exception.getStatusCode())
                .body(
                        new ApiResponse<>(
                                false,
                                exception.getMessage(),
                                null
                        )
                );
    }
}
