package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.constants.AppConstants;
import com.nailic.sproochencoach.dto.ResendEmailRequest;
import com.nailic.sproochencoach.exceptions.EmailDeliveryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ResendEmailSender implements EmailSender {
    private static final Logger log = LoggerFactory.getLogger(ResendEmailSender.class);

    private final RestClient resendRestClient;
    private final String from;
    private final String apiKey;

    public ResendEmailSender(
            @Qualifier(AppConstants.RestClientBeans.RESEND) RestClient resendRestClient,
            @Value(AppConstants.PropertyPlaceholders.EMAIL_FROM) String from,
            @Value(AppConstants.PropertyPlaceholders.EMAIL_RESEND_API_KEY) String apiKey
    ) {
        this.resendRestClient = resendRestClient;
        this.from = from;
        this.apiKey = apiKey;
    }

    @Override
    public void send(String to, String subject, String text) {
        send(to, subject, text, null, null);
    }

    @Override
    public void send(
            String to,
            String subject,
            String text,
            String replyTo,
            List<EmailAttachment> attachments
    ) {
        validateConfiguration();

        try {
            resendRestClient.post()
                    .uri(AppConstants.ApiPaths.RESEND_EMAILS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody(to, subject, text, replyTo, attachments))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            log.error(
                    "Resend email request failed. statusCode={}, recipient={}",
                    exception.getStatusCode().value(),
                    maskEmail(to),
                    exception
            );
            throw new EmailDeliveryException(
                    "Email provider rejected the request",
                    exception.getStatusCode().value(),
                    exception
            );
        } catch (RestClientException exception) {
            log.error("Resend email request failed. recipient={}", maskEmail(to), exception);
            throw new EmailDeliveryException(
                    "Email provider request failed",
                    HttpStatus.BAD_GATEWAY.value(),
                    exception
            );
        }
    }

    private Map<String, Object> requestBody(
            String to,
            String subject,
            String text,
            String replyTo,
            List<EmailAttachment> attachments
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("from", from);
        body.put("to", to);
        body.put("subject", subject);
        body.put("text", text);

        if (replyTo != null && !replyTo.isBlank()) {
            body.put("reply_to", replyTo);
        }

        List<ResendEmailRequest.Attachment> resendAttachments = resendAttachments(attachments);
        if (resendAttachments != null) {
            body.put("attachments", resendAttachments);
        }

        return body;
    }

    private List<ResendEmailRequest.Attachment> resendAttachments(List<EmailAttachment> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return null;
        }

        return attachments.stream()
                .map(attachment -> new ResendEmailRequest.Attachment(
                        attachment.base64Content(),
                        attachment.filename(),
                        attachment.contentType()
                ))
                .toList();
    }

    private void validateConfiguration() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new EmailDeliveryException(
                    "Resend API key is not configured",
                    HttpStatus.INTERNAL_SERVER_ERROR.value()
            );
        }

        if (from == null || from.isBlank()) {
            throw new EmailDeliveryException(
                    "Email sender address is not configured",
                    HttpStatus.INTERNAL_SERVER_ERROR.value()
            );
        }
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "<blank>";
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return "***";
        }

        return email.charAt(0) + "***" + email.substring(atIndex);
    }
}
