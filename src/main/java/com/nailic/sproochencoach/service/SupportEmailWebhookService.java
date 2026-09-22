package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.dto.ResendWebhookEventDto;
import com.nailic.sproochencoach.repository.SupportEmailRepo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class SupportEmailWebhookService {
    private static final Logger log = LoggerFactory.getLogger(SupportEmailWebhookService.class);
    private static final String EMAIL_RECEIVED_EVENT = "email.received";

    private final ResendWebhookVerifier resendWebhookVerifier;
    private final SupportEmailIngestionService supportEmailIngestionService;
    private final SupportEmailRepo supportEmailRepo;
    private final ObjectMapper objectMapper;

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

        supportEmailIngestionService.ingestFromResend(emailId);
    }

    private ResendWebhookEventDto parseEvent(String payload) {
        try {
            return objectMapper.readValue(payload, ResendWebhookEventDto.class);
        } catch (Exception exception) {
            log.warn("Invalid Resend webhook JSON. message={}", exception.getMessage());
            throw new com.nailic.sproochencoach.exceptions.BadRequestException("Invalid webhook payload");
        }
    }
}
