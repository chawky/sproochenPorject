package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.constants.AppConstants;
import com.nailic.sproochencoach.dto.ResendReceivedEmailDto;
import com.nailic.sproochencoach.dto.ResendWebhookEventDto;
import com.nailic.sproochencoach.model.SupportEmail;
import com.nailic.sproochencoach.repository.SupportEmailRepo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SupportEmailWebhookService {
    private static final Logger log = LoggerFactory.getLogger(SupportEmailWebhookService.class);
    private static final String EMAIL_RECEIVED_EVENT = "email.received";

    private final ResendWebhookVerifier resendWebhookVerifier;
    private final ResendReceivedEmailClient resendReceivedEmailClient;
    private final SupportEmailRepo supportEmailRepo;
    private final ObjectMapper objectMapper;

    @Value(AppConstants.PropertyPlaceholders.SUPPORT_EMAIL_RECIPIENT)
    private String supportRecipient;

    @Transactional
    public void handle(String payload, org.springframework.http.HttpHeaders headers) {
        resendWebhookVerifier.verify(payload, headers);

        ResendWebhookEventDto event = parseEvent(payload);
        if (!EMAIL_RECEIVED_EVENT.equals(event.getType())) {
            log.debug("Ignoring unsupported Resend webhook event. type={}", event.getType());
            return;
        }

        String emailId = event.getData() == null ? null : event.getData().getEmail_id();
        if (!StringUtils.hasText(emailId)) {
            log.warn("Ignoring Resend email.received event without email ID");
            return;
        }

        if (supportEmailRepo.existsByResendEmailId(emailId)) {
            log.debug("Ignoring duplicate Resend received email. emailId={}", emailId);
            return;
        }

        ResendReceivedEmailDto receivedEmail = resendReceivedEmailClient.getReceivedEmail(emailId);
        if (!isSupportEmail(receivedEmail.getTo())) {
            log.debug("Ignoring inbound email for non-support recipient. emailId={}", emailId);
            return;
        }

        persist(emailId, receivedEmail);
    }

    private ResendWebhookEventDto parseEvent(String payload) {
        try {
            return objectMapper.readValue(payload, ResendWebhookEventDto.class);
        } catch (Exception exception) {
            log.warn("Invalid Resend webhook JSON. message={}", exception.getMessage());
            throw new com.nailic.sproochencoach.exceptions.BadRequestException("Invalid webhook payload");
        }
    }

    private boolean isSupportEmail(List<String> recipients) {
        if (recipients == null || recipients.isEmpty() || !StringUtils.hasText(supportRecipient)) {
            return false;
        }

        String normalizedSupportRecipient = normalizeEmail(supportRecipient);
        return recipients.stream()
                .map(this::normalizeEmail)
                .anyMatch(normalizedSupportRecipient::equals);
    }

    private void persist(String emailId, ResendReceivedEmailDto receivedEmail) {
        SupportEmail supportEmail = new SupportEmail();
        supportEmail.setResendEmailId(emailId);
        supportEmail.setFromEmail(nullToBlank(receivedEmail.getFrom()));
        supportEmail.setToEmail(String.join(", ", receivedEmail.getTo()));
        supportEmail.setSubject(receivedEmail.getSubject());
        supportEmail.setTextBody(receivedEmail.getText());
        supportEmail.setHtmlBody(receivedEmail.getHtml());
        supportEmail.setReceivedAt(parseDateTime(receivedEmail.getCreated_at()));

        try {
            supportEmailRepo.save(supportEmail);
        } catch (DataIntegrityViolationException exception) {
            log.debug("Ignoring concurrent duplicate Resend received email. emailId={}", emailId);
        }
    }

    private LocalDateTime parseDateTime(String value) {
        if (!StringUtils.hasText(value)) {
            return LocalDateTime.now();
        }

        return OffsetDateTime.parse(value).toLocalDateTime();
    }

    private String normalizeEmail(String value) {
        if (value == null) {
            return "";
        }

        String trimmed = value.trim();
        int start = trimmed.indexOf('<');
        int end = trimmed.indexOf('>');
        if (start >= 0 && end > start) {
            trimmed = trimmed.substring(start + 1, end);
        }

        return trimmed.toLowerCase(Locale.ROOT);
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }
}
