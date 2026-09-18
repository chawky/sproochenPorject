package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.exceptions.BadRequestException;
import com.nailic.sproochencoach.exceptions.EmailDeliveryException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class SupportServiceTest {
    @Test
    void sendsValidSupportRequestWithoutAttachment() {
        EmailSender emailSender = mock(EmailSender.class);
        SupportService service = service(emailSender, mock(SupportRateLimitService.class));

        service.sendSupportRequest(
                "user@example.com",
                "Subscription problem",
                "My subscription page is showing the wrong status.",
                null,
                "203.0.113.10"
        );

        verify(emailSender).send(
                eq("support@letz-speak.com"),
                eq("Support request: Subscription problem"),
                contains("From:\nuser@example.com"),
                eq("user@example.com"),
                eq(List.of())
        );
    }

    @Test
    void sendsValidSupportRequestWithAttachment() {
        EmailSender emailSender = mock(EmailSender.class);
        SupportService service = service(emailSender, mock(SupportRateLimitService.class));
        MockMultipartFile attachment = new MockMultipartFile(
                "attachments",
                "../screenshot.png",
                "image/png",
                new byte[] {
                        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
                }
        );

        service.sendSupportRequest(
                "user@example.com",
                "Bug",
                "Screenshot attached.",
                List.of(attachment),
                "203.0.113.10"
        );

        ArgumentCaptor<List> attachmentsCaptor = ArgumentCaptor.forClass(List.class);
        verify(emailSender).send(
                eq("support@letz-speak.com"),
                eq("Support request: Bug"),
                anyString(),
                eq("user@example.com"),
                attachmentsCaptor.capture()
        );

        @SuppressWarnings("unchecked")
        List<EmailAttachment> attachments = (List<EmailAttachment>) attachmentsCaptor.getValue();
        assertThat(attachments).containsExactly(new EmailAttachment(
                "screenshot.png",
                "image/png",
                "iVBORw0KGgo="
        ));
    }

    @Test
    void rejectsInvalidEmail() {
        EmailSender emailSender = mock(EmailSender.class);
        SupportService service = service(emailSender, mock(SupportRateLimitService.class));

        assertThatThrownBy(() -> service.sendSupportRequest(
                "not-an-email",
                "Bug",
                "Message",
                null,
                "203.0.113.10"
        )).isInstanceOf(BadRequestException.class)
                .hasMessage("Email must be a valid email address.");

        verify(emailSender, never()).send(anyString(), anyString(), anyString(), anyString(), anyList());
    }

    @Test
    void rejectsBlankSubject() {
        EmailSender emailSender = mock(EmailSender.class);
        SupportService service = service(emailSender, mock(SupportRateLimitService.class));

        assertThatThrownBy(() -> service.sendSupportRequest(
                "user@example.com",
                " ",
                "Message",
                null,
                "203.0.113.10"
        )).isInstanceOf(BadRequestException.class)
                .hasMessage("Subject is required.");
    }

    @Test
    void rejectsBlankMessage() {
        EmailSender emailSender = mock(EmailSender.class);
        SupportService service = service(emailSender, mock(SupportRateLimitService.class));

        assertThatThrownBy(() -> service.sendSupportRequest(
                "user@example.com",
                "Bug",
                " ",
                null,
                "203.0.113.10"
        )).isInstanceOf(BadRequestException.class)
                .hasMessage("Message is required.");
    }

    @Test
    void rejectsUnsupportedAttachmentType() {
        EmailSender emailSender = mock(EmailSender.class);
        SupportService service = service(emailSender, mock(SupportRateLimitService.class));
        MockMultipartFile attachment = new MockMultipartFile(
                "attachments",
                "notes.txt",
                "text/plain",
                "hello".getBytes()
        );

        assertThatThrownBy(() -> service.sendSupportRequest(
                "user@example.com",
                "Bug",
                "Message",
                List.of(attachment),
                "203.0.113.10"
        )).isInstanceOf(BadRequestException.class)
                .hasMessage("Unsupported attachment type.");
    }

    @Test
    void rejectsTooManyAttachments() {
        EmailSender emailSender = mock(EmailSender.class);
        SupportService service = service(emailSender, mock(SupportRateLimitService.class));

        assertThatThrownBy(() -> service.sendSupportRequest(
                "user@example.com",
                "Bug",
                "Message",
                List.of(pdf("a.pdf", 1), pdf("b.pdf", 1), pdf("c.pdf", 1), pdf("d.pdf", 1)),
                "203.0.113.10"
        )).isInstanceOf(BadRequestException.class)
                .hasMessage("A maximum of 3 attachments is allowed.");
    }

    @Test
    void rejectsOversizedAttachment() {
        EmailSender emailSender = mock(EmailSender.class);
        SupportService service = service(emailSender, mock(SupportRateLimitService.class));
        byte[] bytes = new byte[(5 * 1024 * 1024) + 1];
        bytes[0] = 0x25;
        bytes[1] = 0x50;
        bytes[2] = 0x44;
        bytes[3] = 0x46;
        bytes[4] = 0x2D;
        MockMultipartFile attachment = new MockMultipartFile(
                "attachments",
                "large.pdf",
                "application/pdf",
                bytes
        );

        assertThatThrownBy(() -> service.sendSupportRequest(
                "user@example.com",
                "Bug",
                "Message",
                List.of(attachment),
                "203.0.113.10"
        )).isInstanceOf(BadRequestException.class)
                .hasMessage("Each attachment must be at most 5 MB.");
    }

    @Test
    void propagatesProviderFailureWithoutChangingIt() {
        EmailSender emailSender = mock(EmailSender.class);
        SupportService service = service(emailSender, mock(SupportRateLimitService.class));
        EmailDeliveryException providerFailure = new EmailDeliveryException(
                "Email provider rejected the request",
                HttpStatus.BAD_GATEWAY.value()
        );
        doThrow(providerFailure).when(emailSender)
                .send(anyString(), anyString(), anyString(), anyString(), anyList());

        assertThatThrownBy(() -> service.sendSupportRequest(
                "user@example.com",
                "Bug",
                "Message",
                null,
                "203.0.113.10"
        )).isSameAs(providerFailure);
    }

    private SupportService service(EmailSender emailSender, SupportRateLimitService supportRateLimitService) {
        SupportService service = new SupportService(emailSender, supportRateLimitService);
        ReflectionTestUtils.setField(service, "recipient", "support@letz-speak.com");
        return service;
    }

    private MockMultipartFile pdf(String filename, int suffix) {
        return new MockMultipartFile(
                "attachments",
                filename,
                "application/pdf",
                new byte[] {0x25, 0x50, 0x44, 0x46, 0x2D, (byte) suffix}
        );
    }
}
