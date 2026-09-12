package com.nailic.sproochencoach.exceptions;

import com.nailic.sproochencoach.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
//This class contains methods that handle exceptions for all controllers.
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String AI_UNAVAILABLE_MESSAGE =
            "We could not create your practice activity right now. Please try again.";
    private static final String LOCATION_UNAVAILABLE_MESSAGE =
            "We could not load location suggestions right now. Please try again.";
    private static final String PAYMENT_UNAVAILABLE_MESSAGE =
            "We could not complete the payment action right now. Please try again.";
    private static final String EMAIL_UNAVAILABLE_MESSAGE =
            "We could not send the verification email right now. Please try again.";
    private static final String OTP_RATE_LIMIT_MESSAGE =
            "Please wait before requesting another verification code.";
    private static final String UNAUTHORIZED_MESSAGE = "Please log in and try again.";
    private static final String UNEXPECTED_ERROR_MESSAGE =
            "Something went wrong. Please try again.";

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

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(BadRequestException exception) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(
                        new ApiResponse<>(
                                false,
                                exception.getMessage(),
                                null
                        )
                );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException exception) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(
                        new ApiResponse<>(
                                false,
                                "You do not have permission to do that.",
                                null
                        )
                );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException exception) {
        log.warn("Handling AuthenticationException. type={}, message={}", exception.getClass().getSimpleName(), exception.getMessage());

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(error(UNAUTHORIZED_MESSAGE));
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<ApiResponse<Void>> handleEmailNotVerifiedException(EmailNotVerifiedException exception) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(error(exception.getMessage()));
    }

    @ExceptionHandler(AiQuotaExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleAiQuotaExceeded(AiQuotaExceededException exception) {
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(
                        new ApiResponse<>(
                                false,
                                exception.getMessage(),
                                null
                        )
                );
    }

    @ExceptionHandler(OtpRateLimitExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleOtpRateLimitExceeded(OtpRateLimitExceededException exception) {
        log.warn("Handling OtpRateLimitExceededException. message={}", exception.getMessage());

        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(error(OTP_RATE_LIMIT_MESSAGE));
    }

    @ExceptionHandler(AiUsageRecordingException.class)
    public ResponseEntity<ApiResponse<Void>> handleAiUsageRecordingException(AiUsageRecordingException exception) {
        log.error("Handling AiUsageRecordingException. message={}", exception.getMessage(), exception);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error(UNEXPECTED_ERROR_MESSAGE));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(this::fieldErrorMessage)
                .orElse("Invalid request");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(
                        new ApiResponse<>(
                                false,
                                message,
                                null
                        )
                );
    }

    @ExceptionHandler(LocationProviderException.class)
    public ResponseEntity<ApiResponse<Void>> handleLocationProviderException(LocationProviderException exception) {
        log.error("Handling LocationProviderException. statusCode={}, message={}", exception.getStatusCode(), exception.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(error(LOCATION_UNAVAILABLE_MESSAGE));
    }

    @ExceptionHandler(AiProviderException.class)
    public ResponseEntity<ApiResponse<Void>> handleAiProviderException(
            AiProviderException exception
    ) {
        log.error("Handling AiProviderException. statusCode={}, message={}", exception.getStatusCode(), exception.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(error(AI_UNAVAILABLE_MESSAGE));
    }

    @ExceptionHandler(OpenRouterError.class)
    public ResponseEntity<ApiResponse<Void>> handleOpenRouterError(
            OpenRouterError exception
    ) {
        log.error("Handling OpenRouterError. statusCode={}, message={}", exception.getStatusCode(), exception.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(error(AI_UNAVAILABLE_MESSAGE));
    }

    @ExceptionHandler(StripePaymentException.class)
    public ResponseEntity<ApiResponse<Void>> handleStripePaymentException(
            StripePaymentException exception
    ) {
        log.error("Handling StripePaymentException. statusCode={}, message={}", exception.getStatusCode(), exception.getMessage());

        return ResponseEntity
                .status(exception.getStatusCode())
                .body(error(paymentMessage(exception)));
    }

    @ExceptionHandler(EmailDeliveryException.class)
    public ResponseEntity<ApiResponse<Void>> handleEmailDeliveryException(
            EmailDeliveryException exception
    ) {
        log.error("Handling EmailDeliveryException. statusCode={}, message={}", exception.getStatusCode(), exception.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(error(EMAIL_UNAVAILABLE_MESSAGE));
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MissingRequestHeaderException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class,
            ConstraintViolationException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleMissingRequestValue(Exception exception) {
        log.warn("Handling invalid request. type={}, message={}", exception.getClass().getSimpleName(), exception.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error("Please check the form and try again."));
    }

    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<ApiResponse<Void>> handleRestClientException(RestClientException exception) {
        log.error("Handling RestClientException. message={}", exception.getMessage(), exception);

        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(error(UNEXPECTED_ERROR_MESSAGE));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException exception) {
        log.error("Handling unexpected RuntimeException. type={}, message={}", exception.getClass().getSimpleName(), exception.getMessage(), exception);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error(UNEXPECTED_ERROR_MESSAGE));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception exception) {
        log.error("Handling unexpected Exception. type={}, message={}", exception.getClass().getSimpleName(), exception.getMessage(), exception);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error(UNEXPECTED_ERROR_MESSAGE));
    }

    private ApiResponse<Void> error(String message) {
        return new ApiResponse<>(
                false,
                message,
                null
        );
    }

    private String paymentMessage(StripePaymentException exception) {
        if (exception.getStatusCode() == HttpStatus.CONFLICT.value()) {
            return "You already have an active subscription.";
        }

        if (exception.getStatusCode() == HttpStatus.BAD_REQUEST.value()) {
            return "We could not process this payment request.";
        }

        return PAYMENT_UNAVAILABLE_MESSAGE;
    }

    private String validationMessage(String message) {
        if (message == null || message.isBlank()) {
            return "Please check the form and try again.";
        }

        return message;
    }

    private String fieldName(String field) {
        if (field == null || field.isBlank()) {
            return "Field";
        }

        String spaced = field.replaceAll("([a-z])([A-Z])", "$1 $2");
        return spaced.substring(0, 1).toUpperCase() + spaced.substring(1);
    }

    private String fieldErrorMessage(FieldError fieldError) {
        return fieldName(fieldError.getField()) + ": " + validationMessage(fieldError.getDefaultMessage());
    }
}
