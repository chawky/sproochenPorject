package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.constants.AppConstants;
import com.nailic.sproochencoach.exceptions.BadRequestException;
import com.nailic.sproochencoach.exceptions.EmailDeliveryException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SupportService {
    private static final Logger log = LoggerFactory.getLogger(SupportService.class);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final int MAX_EMAIL_LENGTH = 254;
    private static final int MAX_SUBJECT_LENGTH = 150;
    private static final int MAX_MESSAGE_LENGTH = 5_000;
    private static final int MAX_ATTACHMENTS = 3;
    private static final long MAX_ATTACHMENT_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "application/pdf"
    );

    private final EmailSender emailSender;
    private final SupportRateLimitService supportRateLimitService;
    @Value(AppConstants.PropertyPlaceholders.SUPPORT_EMAIL_RECIPIENT)
    private String recipient;

    public void sendSupportRequest(
            String email,
            String subject,
            String message,
            List<MultipartFile> attachments,
            String clientIp
    ) {
        supportRateLimitService.checkAllowed(email, clientIp);

        String trimmedEmail = validateEmail(email);
        String trimmedSubject = validateText("Subject", subject, MAX_SUBJECT_LENGTH);
        String trimmedMessage = validateText("Message", message, MAX_MESSAGE_LENGTH);
        List<EmailAttachment> emailAttachments = validateAttachments(attachments);

        String text = """
                Support request
                
                From:
                %s
                
                Subject:
                %s
                
                Message:
                %s
                """.formatted(trimmedEmail, trimmedSubject, trimmedMessage);

        try {
            emailSender.send(
                    recipient,
                    "Support request: " + trimmedSubject,
                    text,
                    trimmedEmail,
                    emailAttachments
            );
        } catch (RuntimeException exception) {
            log.error(
                    "Failed to send support request. requester={}, attachmentCount={}",
                    maskEmail(trimmedEmail),
                    emailAttachments.size(),
                    exception
            );
            throw exception;
        }
    }

    private String validateEmail(String email) {
        String trimmedEmail = trim(email);
        if (trimmedEmail.isBlank()) {
            throw new BadRequestException("Email is required.");
        }

        if (trimmedEmail.length() > MAX_EMAIL_LENGTH || !EMAIL_PATTERN.matcher(trimmedEmail).matches()) {
            throw new BadRequestException("Email must be a valid email address.");
        }

        return trimmedEmail;
    }

    private String validateText(String field, String value, int maxLength) {
        String trimmedValue = trim(value);
        if (trimmedValue.isBlank()) {
            throw new BadRequestException(field + " is required.");
        }

        if (trimmedValue.length() > maxLength) {
            throw new BadRequestException(field + " must be at most " + maxLength + " characters.");
        }

        return trimmedValue;
    }

    private List<EmailAttachment> validateAttachments(List<MultipartFile> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return List.of();
        }

        List<MultipartFile> submittedAttachments = attachments.stream()
                .filter(attachment -> attachment != null && hasSubmittedFile(attachment))
                .toList();
        if (submittedAttachments.size() > MAX_ATTACHMENTS) {
            throw new BadRequestException("A maximum of 3 attachments is allowed.");
        }

        return submittedAttachments.stream()
                .map(this::validateAttachment)
                .toList();
    }

    private EmailAttachment validateAttachment(MultipartFile attachment) {
        String contentType = normalizeContentType(attachment.getContentType());
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException("Unsupported attachment type.");
        }

        if (attachment.getSize() > MAX_ATTACHMENT_BYTES) {
            throw new BadRequestException("Each attachment must be at most 5 MB.");
        }

        byte[] bytes = readAttachment(attachment);
        if (!matchesContentType(contentType, bytes)) {
            throw new BadRequestException("Unsupported attachment type.");
        }

        return new EmailAttachment(
                safeFilename(attachment.getOriginalFilename()),
                contentType,
                Base64.getEncoder().encodeToString(bytes)
        );
    }

    private byte[] readAttachment(MultipartFile attachment) {
        try {
            return attachment.getBytes();
        } catch (IOException exception) {
            log.error(
                    "Failed to read support attachment. filename={}, size={}",
                    safeFilename(attachment.getOriginalFilename()),
                    attachment.getSize(),
                    exception
            );
            throw new EmailDeliveryException(
                    "Support attachment could not be read",
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    exception
            );
        }
    }

    private boolean matchesContentType(String contentType, byte[] bytes) {
        return switch (contentType) {
            case "image/jpeg" -> bytes.length >= 3
                    && (bytes[0] & 0xFF) == 0xFF
                    && (bytes[1] & 0xFF) == 0xD8
                    && (bytes[2] & 0xFF) == 0xFF;
            case "image/png" -> bytes.length >= 8
                    && (bytes[0] & 0xFF) == 0x89
                    && bytes[1] == 0x50
                    && bytes[2] == 0x4E
                    && bytes[3] == 0x47
                    && bytes[4] == 0x0D
                    && bytes[5] == 0x0A
                    && bytes[6] == 0x1A
                    && bytes[7] == 0x0A;
            case "image/webp" -> bytes.length >= 12
                    && bytes[0] == 0x52
                    && bytes[1] == 0x49
                    && bytes[2] == 0x46
                    && bytes[3] == 0x46
                    && bytes[8] == 0x57
                    && bytes[9] == 0x45
                    && bytes[10] == 0x42
                    && bytes[11] == 0x50;
            case "application/pdf" -> bytes.length >= 5
                    && bytes[0] == 0x25
                    && bytes[1] == 0x50
                    && bytes[2] == 0x44
                    && bytes[3] == 0x46
                    && bytes[4] == 0x2D;
            default -> false;
        };
    }

    private boolean hasSubmittedFile(MultipartFile attachment) {
        return attachment.getOriginalFilename() != null
                && !attachment.getOriginalFilename().isBlank();
    }

    private String normalizeContentType(String contentType) {
        return contentType == null
                ? ""
                : contentType.trim().toLowerCase(Locale.ROOT);
    }

    private String safeFilename(String filename) {
        String cleanFilename = StringUtils.cleanPath(trim(filename)).replace("\\", "/");
        int slashIndex = cleanFilename.lastIndexOf('/');
        if (slashIndex >= 0) {
            cleanFilename = cleanFilename.substring(slashIndex + 1);
        }

        cleanFilename = cleanFilename.replaceAll("[\\r\\n\\t\\x00-\\x1F\\x7F]", "_");
        if (cleanFilename.isBlank() || cleanFilename.equals(".") || cleanFilename.equals("..")) {
            return "attachment";
        }

        return cleanFilename.length() > 150
                ? cleanFilename.substring(cleanFilename.length() - 150)
                : cleanFilename;
    }

    private String trim(String value) {
        return value == null
                ? ""
                : value.trim();
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
