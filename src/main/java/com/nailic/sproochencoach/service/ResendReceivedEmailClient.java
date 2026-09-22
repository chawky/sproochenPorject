package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.constants.AppConstants;
import com.nailic.sproochencoach.dto.ResendReceivedEmailDto;
import com.nailic.sproochencoach.dto.ResendReceivedEmailAttachmentDto;
import com.nailic.sproochencoach.dto.ResendReceivedEmailListDto;
import com.nailic.sproochencoach.dto.SupportEmailAttachmentDownload;
import com.nailic.sproochencoach.exceptions.EmailDeliveryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;

@Service
public class ResendReceivedEmailClient {
    private static final Logger log = LoggerFactory.getLogger(ResendReceivedEmailClient.class);

    private final RestClient resendRestClient;
    private final String apiKey;

    public ResendReceivedEmailClient(
            @Qualifier(AppConstants.RestClientBeans.RESEND) RestClient resendRestClient,
            @Value(AppConstants.PropertyPlaceholders.EMAIL_RESEND_API_KEY) String apiKey
    ) {
        this.resendRestClient = resendRestClient;
        this.apiKey = apiKey;
    }

    public ResendReceivedEmailDto getReceivedEmail(String emailId) {
        validateConfiguration();

        try {
            return resendRestClient.get()
                    .uri(AppConstants.ApiPaths.RESEND_RECEIVING_EMAIL, emailId)
                    .retrieve()
                    .body(ResendReceivedEmailDto.class);
        } catch (RestClientResponseException exception) {
            log.error(
                    "Resend received email retrieval failed. statusCode={}, emailId={}",
                    exception.getStatusCode().value(),
                    emailId,
                    exception
            );
            throw new EmailDeliveryException(
                    "Email provider rejected the received email request",
                    exception.getStatusCode().value(),
                    exception
            );
        } catch (RestClientException exception) {
            log.error("Resend received email retrieval failed. emailId={}", emailId, exception);
            throw new EmailDeliveryException(
                    "Email provider received email request failed",
                    HttpStatus.BAD_GATEWAY.value(),
                    exception
            );
        }
    }

    public ResendReceivedEmailListDto listReceivedEmails(int limit, String after) {
        validateConfiguration();

        try {
            return resendRestClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder
                                .path(AppConstants.ApiPaths.RESEND_RECEIVING_EMAILS)
                                .queryParam("limit", limit);
                        if (after != null && !after.isBlank()) {
                            builder.queryParam("after", after);
                        }
                        return builder.build();
                    })
                    .retrieve()
                    .body(ResendReceivedEmailListDto.class);
        } catch (RestClientResponseException exception) {
            log.error(
                    "Resend received email list failed. statusCode={}",
                    exception.getStatusCode().value(),
                    exception
            );
            throw new EmailDeliveryException(
                    "Email provider rejected the received email list request",
                    exception.getStatusCode().value(),
                    exception
            );
        } catch (RestClientException exception) {
            log.error("Resend received email list failed.", exception);
            throw new EmailDeliveryException(
                    "Email provider received email list request failed",
                    HttpStatus.BAD_GATEWAY.value(),
                    exception
            );
        }
    }

    public SupportEmailAttachmentDownload downloadReceivedAttachment(
            String emailId,
            String attachmentId,
            String fallbackFilename,
            String fallbackContentType
    ) {
        ResendReceivedEmailAttachmentDto attachment = getReceivedAttachment(emailId, attachmentId);
        if (attachment == null || attachment.getDownload_url() == null || attachment.getDownload_url().isBlank()) {
            throw new EmailDeliveryException("Received email attachment download URL is missing", HttpStatus.BAD_GATEWAY.value());
        }

        try {
            byte[] content = RestClient.create()
                    .get()
                    .uri(URI.create(attachment.getDownload_url()))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new EmailDeliveryException(
                                "Received email attachment download failed",
                                response.getStatusCode().value()
                        );
                    })
                    .body(byte[].class);

            return new SupportEmailAttachmentDownload(
                    firstText(attachment.getFilename(), fallbackFilename),
                    firstText(attachment.getContent_type(), fallbackContentType),
                    content == null ? new byte[0] : content
            );
        } catch (EmailDeliveryException exception) {
            throw exception;
        } catch (RestClientException exception) {
            log.error("Resend received attachment signed URL download failed. emailId={}, attachmentId={}", emailId, attachmentId, exception);
            throw new EmailDeliveryException(
                    "Received email attachment download request failed",
                    HttpStatus.BAD_GATEWAY.value(),
                    exception
            );
        }
    }

    private ResendReceivedEmailAttachmentDto getReceivedAttachment(String emailId, String attachmentId) {
        validateConfiguration();

        try {
            return resendRestClient.get()
                    .uri(AppConstants.ApiPaths.RESEND_RECEIVING_ATTACHMENT, emailId, attachmentId)
                    .retrieve()
                    .body(ResendReceivedEmailAttachmentDto.class);
        } catch (RestClientResponseException exception) {
            log.error(
                    "Resend received attachment retrieval failed. statusCode={}, emailId={}, attachmentId={}",
                    exception.getStatusCode().value(),
                    emailId,
                    attachmentId,
                    exception
            );
            throw new EmailDeliveryException(
                    "Email provider rejected the received attachment request",
                    exception.getStatusCode().value(),
                    exception
            );
        } catch (RestClientException exception) {
            log.error("Resend received attachment retrieval failed. emailId={}, attachmentId={}", emailId, attachmentId, exception);
            throw new EmailDeliveryException(
                    "Email provider received attachment request failed",
                    HttpStatus.BAD_GATEWAY.value(),
                    exception
            );
        }
    }

    private String firstText(String value, String fallback) {
        if (value != null && !value.isBlank()) {
            return value;
        }

        return fallback;
    }

    private void validateConfiguration() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new EmailDeliveryException(
                    "Resend API key is not configured",
                    HttpStatus.INTERNAL_SERVER_ERROR.value()
            );
        }
    }
}
