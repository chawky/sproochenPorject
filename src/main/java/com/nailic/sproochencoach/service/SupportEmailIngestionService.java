package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.constants.AppConstants;
import com.nailic.sproochencoach.dto.ResendReceivedEmailAttachmentDto;
import com.nailic.sproochencoach.dto.ResendReceivedEmailDto;
import com.nailic.sproochencoach.model.SupportEmail;
import com.nailic.sproochencoach.model.SupportEmailAttachment;
import com.nailic.sproochencoach.repository.SupportEmailAttachmentRepo;
import com.nailic.sproochencoach.repository.SupportEmailRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SupportEmailIngestionService {
    private final SupportEmailRepo supportEmailRepo;
    private final SupportEmailAttachmentRepo supportEmailAttachmentRepo;
    private final ResendReceivedEmailClient resendReceivedEmailClient;

    @Value(AppConstants.PropertyPlaceholders.SUPPORT_EMAIL_RECIPIENT)
    private String supportRecipient;

    @Transactional
    public SupportEmailIngestionResult ingestFromResend(String emailId) {
        return ingest(resendReceivedEmailClient.getReceivedEmail(emailId));
    }

    @Transactional
    public SupportEmailIngestionResult ingest(ResendReceivedEmailDto receivedEmail) {
        if (receivedEmail == null || !StringUtils.hasText(receivedEmail.getId())) {
            return SupportEmailIngestionResult.ignored();
        }

        SupportEmail existingEmail = supportEmailRepo.findByResendEmailId(receivedEmail.getId()).orElse(null);
        if (existingEmail != null) {
            int attachmentsAdded = backfillAttachments(existingEmail, receivedEmail.getAttachments());
            return SupportEmailIngestionResult.existing(attachmentsAdded);
        }

        if (!isSupportEmail(receivedEmail.getTo())) {
            return SupportEmailIngestionResult.ignored();
        }

        SupportEmail supportEmail = new SupportEmail();
        supportEmail.setResendEmailId(receivedEmail.getId());
        supportEmail.setFromEmail(nullToBlank(receivedEmail.getFrom()));
        supportEmail.setToEmail(String.join(", ", nullToEmpty(receivedEmail.getTo())));
        supportEmail.setSubject(receivedEmail.getSubject());
        supportEmail.setTextBody(receivedEmail.getText());
        supportEmail.setHtmlBody(receivedEmail.getHtml());
        supportEmail.setReceivedAt(parseDateTime(receivedEmail.getCreated_at()));

        try {
            SupportEmail savedEmail = supportEmailRepo.save(supportEmail);
            int attachmentsAdded = backfillAttachments(savedEmail, receivedEmail.getAttachments());
            return SupportEmailIngestionResult.imported(attachmentsAdded);
        } catch (DataIntegrityViolationException exception) {
            SupportEmail concurrentlyCreatedEmail = supportEmailRepo.findByResendEmailId(receivedEmail.getId())
                    .orElseThrow(() -> exception);
            int attachmentsAdded = backfillAttachments(concurrentlyCreatedEmail, receivedEmail.getAttachments());
            return SupportEmailIngestionResult.existing(attachmentsAdded);
        }
    }

    private int backfillAttachments(SupportEmail supportEmail, List<ResendReceivedEmailAttachmentDto> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return 0;
        }

        int added = 0;
        for (ResendReceivedEmailAttachmentDto attachment : attachments) {
            if (!StringUtils.hasText(attachment.getId())
                    || supportEmailAttachmentRepo.existsBySupportEmailIdAndResendAttachmentId(
                    supportEmail.getId(),
                    attachment.getId()
            )) {
                continue;
            }

            try {
                supportEmailAttachmentRepo.save(toAttachment(supportEmail, attachment));
                added++;
            } catch (DataIntegrityViolationException ignoredDuplicate) {
                // Unique constraint is the final idempotency guard for concurrent sync/webhook processing.
            }
        }

        return added;
    }

    private SupportEmailAttachment toAttachment(
            SupportEmail supportEmail,
            ResendReceivedEmailAttachmentDto resendAttachment
    ) {
        SupportEmailAttachment attachment = new SupportEmailAttachment();
        attachment.setSupportEmail(supportEmail);
        attachment.setResendAttachmentId(resendAttachment.getId());
        attachment.setFilename(resendAttachment.getFilename());
        attachment.setContentType(resendAttachment.getContent_type());
        attachment.setContentDisposition(resendAttachment.getContent_disposition());
        attachment.setContentId(resendAttachment.getContent_id());
        attachment.setSizeBytes(resendAttachment.getSize());
        return attachment;
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

    private List<String> nullToEmpty(List<String> values) {
        return values == null ? List.of() : values;
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }
}
