package com.nailic.sproochencoach.controller;

import com.nailic.sproochencoach.dto.ResendReceivedEmailDto;
import com.nailic.sproochencoach.dto.ResendReceivedEmailAttachmentDto;
import com.nailic.sproochencoach.dto.ResendReceivedEmailListDto;
import com.nailic.sproochencoach.dto.SupportEmailAttachmentDownload;
import com.nailic.sproochencoach.exceptions.EmailDeliveryException;
import com.nailic.sproochencoach.model.SupportEmail;
import com.nailic.sproochencoach.model.SupportEmailAttachment;
import com.nailic.sproochencoach.repository.SupportEmailAttachmentRepo;
import com.nailic.sproochencoach.repository.SupportEmailRepo;
import com.nailic.sproochencoach.service.ResendReceivedEmailClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "resend.webhook-secret=whsec_MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "support.email.recipient=support@letz-speak.com"
})
class SupportEmailWebhookAndAdminTest {
    private static final String WEBHOOK_SECRET = "whsec_MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SupportEmailRepo supportEmailRepo;

    @Autowired
    private SupportEmailAttachmentRepo supportEmailAttachmentRepo;

    @MockitoBean
    private ResendReceivedEmailClient resendReceivedEmailClient;

    @BeforeEach
    void setUp() {
        supportEmailAttachmentRepo.deleteAll();
        supportEmailRepo.deleteAll();
    }

    @Test
    void supportEmailWithoutAttachmentsStillWorks() throws Exception {
        String emailId = "550e8400-e29b-41d4-a716-446655440000";
        String payload = payload(emailId);
        when(resendReceivedEmailClient.getReceivedEmail(emailId))
                .thenReturn(receivedEmail(emailId, List.of("support@letz-speak.com")));

        postSignedWebhook(payload)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        List<SupportEmail> emails = supportEmailRepo.findAll();
        assertThat(emails).hasSize(1);
        assertThat(emails.get(0).getResendEmailId()).isEqualTo(emailId);
        assertThat(emails.get(0).getFromEmail()).isEqualTo("learner@example.com");
        assertThat(emails.get(0).getToEmail()).isEqualTo("support@letz-speak.com");
        assertThat(emails.get(0).getSubject()).isEqualTo("Need help");
        assertThat(emails.get(0).getTextBody()).isEqualTo("Plain support request");
        assertThat(emails.get(0).getHtmlBody()).isEqualTo("<p>Plain support request</p>");
        assertThat(supportEmailAttachmentRepo.count()).isZero();
    }

    @Test
    void emailWithAttachmentPersistsMetadata() throws Exception {
        String emailId = "550e8400-e29b-41d4-a716-446655440010";
        String payload = payload(emailId);
        when(resendReceivedEmailClient.getReceivedEmail(emailId))
                .thenReturn(receivedEmail(emailId, List.of("support@letz-speak.com"), List.of(attachment("att_1"))));

        postSignedWebhook(payload).andExpect(status().isOk());

        List<SupportEmailAttachment> attachments = supportEmailAttachmentRepo.findAll();
        assertThat(attachments).hasSize(1);
        assertThat(attachments.get(0).getResendAttachmentId()).isEqualTo("att_1");
        assertThat(attachments.get(0).getFilename()).isEqualTo("invoice.pdf");
        assertThat(attachments.get(0).getContentType()).isEqualTo("application/pdf");
        assertThat(attachments.get(0).getContentDisposition()).isEqualTo("attachment");
        assertThat(attachments.get(0).getContentId()).isEqualTo("cid-1");
        assertThat(attachments.get(0).getSizeBytes()).isEqualTo(2048L);
    }

    @Test
    void invalidWebhookSignatureIsRejected() throws Exception {
        mockMvc.perform(post("/api/webhooks/resend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("svix-id", "msg_invalid")
                        .header("svix-timestamp", String.valueOf(System.currentTimeMillis() / 1000))
                        .header("svix-signature", "v1,invalid")
                        .content(payload("550e8400-e29b-41d4-a716-446655440000")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        assertThat(supportEmailRepo.count()).isZero();
    }

    @Test
    void duplicateWebhookDoesNotDuplicateSupportEmail() throws Exception {
        String emailId = "550e8400-e29b-41d4-a716-446655440001";
        String payload = payload(emailId);
        when(resendReceivedEmailClient.getReceivedEmail(emailId))
                .thenReturn(receivedEmail(emailId, List.of("support@letz-speak.com"), List.of(attachment("att_duplicate"))));

        postSignedWebhook(payload).andExpect(status().isOk());
        postSignedWebhook(payload).andExpect(status().isOk());

        assertThat(supportEmailRepo.count()).isEqualTo(1);
        assertThat(supportEmailAttachmentRepo.count()).isEqualTo(1);
        verify(resendReceivedEmailClient, times(1)).getReceivedEmail(emailId);
    }

    @Test
    void emailSentToAnotherRecipientIsNotStored() throws Exception {
        String emailId = "550e8400-e29b-41d4-a716-446655440002";
        String payload = payload(emailId);
        when(resendReceivedEmailClient.getReceivedEmail(emailId))
                .thenReturn(receivedEmail(emailId, List.of("billing@letz-speak.com")));

        postSignedWebhook(payload).andExpect(status().isOk());

        assertThat(supportEmailRepo.count()).isZero();
    }

    @Test
    @WithMockUser(roles = "USER")
    void nonAdminCannotAccessAdminSupportEmailApis() throws Exception {
        mockMvc.perform(get("/api/admin/support-emails"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void nonAdminCannotRetrieveAttachment() throws Exception {
        SupportEmail email = savedEmail("download-email-id", "from@example.com", LocalDateTime.parse("2026-09-21T10:00:00"));
        SupportEmailAttachment attachment = savedAttachment(email, "att_download");

        mockMvc.perform(get("/api/admin/support-emails/{emailId}/attachments/{attachmentId}", email.getId(), attachment.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanListReadAndMarkSupportEmailAsRead() throws Exception {
        SupportEmail older = savedEmail("old-email-id", "old@example.com", LocalDateTime.parse("2026-09-20T10:00:00"));
        SupportEmail newer = savedEmail("new-email-id", "new@example.com", LocalDateTime.parse("2026-09-21T10:00:00"));
        SupportEmailAttachment attachment = savedAttachment(newer, "att_detail");

        mockMvc.perform(get("/api/admin/support-emails"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(newer.getId()))
                .andExpect(jsonPath("$.data[0].fromEmail").value("new@example.com"))
                .andExpect(jsonPath("$.data[0].read").value(false))
                .andExpect(jsonPath("$.data[1].id").value(older.getId()));

        mockMvc.perform(get("/api/admin/support-emails/{id}", newer.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.toEmail").value("support@letz-speak.com"))
                .andExpect(jsonPath("$.data.textBody").value("Message body"))
                .andExpect(jsonPath("$.data.htmlBody").value("<p>Message body</p>"))
                .andExpect(jsonPath("$.data.attachments[0].id").value(attachment.getId()))
                .andExpect(jsonPath("$.data.attachments[0].filename").value("invoice.pdf"))
                .andExpect(jsonPath("$.data.attachments[0].contentType").value("application/pdf"))
                .andExpect(jsonPath("$.data.attachments[0].sizeBytes").value(2048));

        mockMvc.perform(patch("/api/admin/support-emails/{id}/read", newer.getId()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.read").value(true));

        assertThat(supportEmailRepo.findById(newer.getId()).orElseThrow().isRead()).isTrue();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void attachmentCannotBeRetrievedThroughDifferentSupportEmailId() throws Exception {
        SupportEmail owner = savedEmail("owner-email-id", "owner@example.com", LocalDateTime.parse("2026-09-21T10:00:00"));
        SupportEmail other = savedEmail("other-email-id", "other@example.com", LocalDateTime.parse("2026-09-22T10:00:00"));
        SupportEmailAttachment attachment = savedAttachment(owner, "att_owner");

        mockMvc.perform(get("/api/admin/support-emails/{emailId}/attachments/{attachmentId}", other.getId(), attachment.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanRetrieveValidAttachment() throws Exception {
        SupportEmail email = savedEmail("resend-email-download-id", "from@example.com", LocalDateTime.parse("2026-09-21T10:00:00"));
        SupportEmailAttachment attachment = savedAttachment(email, "resend-attachment-download-id");
        when(resendReceivedEmailClient.downloadReceivedAttachment(
                "resend-email-download-id",
                "resend-attachment-download-id",
                "invoice.pdf",
                "application/pdf"
        )).thenReturn(new SupportEmailAttachmentDownload("../invoice.pdf", "application/pdf", "pdf-bytes".getBytes(StandardCharsets.UTF_8)));

        mockMvc.perform(get("/api/admin/support-emails/{emailId}/attachments/{attachmentId}", email.getId(), attachment.getId()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/pdf"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"invoice.pdf\""))
                .andExpect(content().bytes("pdf-bytes".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void resendAttachmentRetrievalFailureIsHandledCleanly() throws Exception {
        SupportEmail email = savedEmail("resend-email-failure-id", "from@example.com", LocalDateTime.parse("2026-09-21T10:00:00"));
        SupportEmailAttachment attachment = savedAttachment(email, "resend-attachment-failure-id");
        when(resendReceivedEmailClient.downloadReceivedAttachment(
                "resend-email-failure-id",
                "resend-attachment-failure-id",
                "invoice.pdf",
                "application/pdf"
        )).thenThrow(new EmailDeliveryException("Email provider rejected the received attachment request", HttpStatus.NOT_FOUND.value()));

        mockMvc.perform(get("/api/admin/support-emails/{emailId}/attachments/{attachmentId}", email.getId(), attachment.getId()))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void syncImportsMissingReceivedEmail() throws Exception {
        String emailId = "sync-import-email-id";
        when(resendReceivedEmailClient.listReceivedEmails(100, null))
                .thenReturn(receivedEmailPage(false, listedEmail(emailId)));
        when(resendReceivedEmailClient.getReceivedEmail(emailId))
                .thenReturn(receivedEmail(emailId, List.of("support@letz-speak.com")));

        mockMvc.perform(post("/api/admin/support-emails/sync").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.imported").value(1))
                .andExpect(jsonPath("$.data.existing").value(0))
                .andExpect(jsonPath("$.data.ignored").value(0))
                .andExpect(jsonPath("$.data.attachmentsAdded").value(0))
                .andExpect(jsonPath("$.data.failed").value(0));

        assertThat(supportEmailRepo.findByResendEmailId(emailId)).isPresent();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void syncDoesNotImportEmailForAnotherRecipient() throws Exception {
        String emailId = "sync-ignore-email-id";
        when(resendReceivedEmailClient.listReceivedEmails(100, null))
                .thenReturn(receivedEmailPage(false, listedEmail(emailId)));
        when(resendReceivedEmailClient.getReceivedEmail(emailId))
                .thenReturn(receivedEmail(emailId, List.of("billing@letz-speak.com")));

        mockMvc.perform(post("/api/admin/support-emails/sync").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.imported").value(0))
                .andExpect(jsonPath("$.data.ignored").value(1));

        assertThat(supportEmailRepo.count()).isZero();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void syncDoesNotDuplicateExistingEmail() throws Exception {
        String emailId = "sync-existing-email-id";
        savedEmail(emailId, "from@example.com", LocalDateTime.parse("2026-09-21T10:00:00"));
        when(resendReceivedEmailClient.listReceivedEmails(100, null))
                .thenReturn(receivedEmailPage(false, listedEmail(emailId)));
        when(resendReceivedEmailClient.getReceivedEmail(emailId))
                .thenReturn(receivedEmail(emailId, List.of("support@letz-speak.com")));

        mockMvc.perform(post("/api/admin/support-emails/sync").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.imported").value(0))
                .andExpect(jsonPath("$.data.existing").value(1));

        assertThat(supportEmailRepo.count()).isEqualTo(1);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void syncBackfillsMissingAttachmentMetadataForExistingEmail() throws Exception {
        String emailId = "sync-backfill-email-id";
        savedEmail(emailId, "from@example.com", LocalDateTime.parse("2026-09-21T10:00:00"));
        when(resendReceivedEmailClient.listReceivedEmails(100, null))
                .thenReturn(receivedEmailPage(false, listedEmail(emailId)));
        when(resendReceivedEmailClient.getReceivedEmail(emailId))
                .thenReturn(receivedEmail(emailId, List.of("support@letz-speak.com"), List.of(attachment("sync-att-1"))));

        mockMvc.perform(post("/api/admin/support-emails/sync").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.imported").value(0))
                .andExpect(jsonPath("$.data.existing").value(1))
                .andExpect(jsonPath("$.data.attachmentsAdded").value(1));

        assertThat(supportEmailAttachmentRepo.count()).isEqualTo(1);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void syncDoesNotDuplicateExistingAttachmentMetadata() throws Exception {
        String emailId = "sync-existing-attachment-email-id";
        SupportEmail email = savedEmail(emailId, "from@example.com", LocalDateTime.parse("2026-09-21T10:00:00"));
        savedAttachment(email, "sync-att-existing");
        when(resendReceivedEmailClient.listReceivedEmails(100, null))
                .thenReturn(receivedEmailPage(false, listedEmail(emailId)));
        when(resendReceivedEmailClient.getReceivedEmail(emailId))
                .thenReturn(receivedEmail(emailId, List.of("support@letz-speak.com"), List.of(attachment("sync-att-existing"))));

        mockMvc.perform(post("/api/admin/support-emails/sync").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.existing").value(1))
                .andExpect(jsonPath("$.data.attachmentsAdded").value(0));

        assertThat(supportEmailAttachmentRepo.count()).isEqualTo(1);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void repeatedSyncIsIdempotent() throws Exception {
        String emailId = "sync-idempotent-email-id";
        when(resendReceivedEmailClient.listReceivedEmails(100, null))
                .thenReturn(receivedEmailPage(false, listedEmail(emailId)));
        when(resendReceivedEmailClient.getReceivedEmail(emailId))
                .thenReturn(receivedEmail(emailId, List.of("support@letz-speak.com"), List.of(attachment("sync-att-idempotent"))));

        mockMvc.perform(post("/api/admin/support-emails/sync").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.imported").value(1))
                .andExpect(jsonPath("$.data.attachmentsAdded").value(1));

        mockMvc.perform(post("/api/admin/support-emails/sync").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.imported").value(0))
                .andExpect(jsonPath("$.data.existing").value(1))
                .andExpect(jsonPath("$.data.attachmentsAdded").value(0));

        assertThat(supportEmailRepo.count()).isEqualTo(1);
        assertThat(supportEmailAttachmentRepo.count()).isEqualTo(1);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void syncProcessesMultipleResendPages() throws Exception {
        String firstEmailId = "sync-page-1-email-id";
        String secondEmailId = "sync-page-2-email-id";
        when(resendReceivedEmailClient.listReceivedEmails(100, null))
                .thenReturn(receivedEmailPage(true, listedEmail(firstEmailId)));
        when(resendReceivedEmailClient.listReceivedEmails(100, firstEmailId))
                .thenReturn(receivedEmailPage(false, listedEmail(secondEmailId)));
        when(resendReceivedEmailClient.getReceivedEmail(firstEmailId))
                .thenReturn(receivedEmail(firstEmailId, List.of("support@letz-speak.com")));
        when(resendReceivedEmailClient.getReceivedEmail(secondEmailId))
                .thenReturn(receivedEmail(secondEmailId, List.of("support@letz-speak.com")));

        mockMvc.perform(post("/api/admin/support-emails/sync").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.imported").value(2));

        assertThat(supportEmailRepo.count()).isEqualTo(2);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void syncReportsSingleEmailFailureAndContinues() throws Exception {
        String failingEmailId = "sync-failing-email-id";
        String importedEmailId = "sync-after-failure-email-id";
        when(resendReceivedEmailClient.listReceivedEmails(100, null))
                .thenReturn(receivedEmailPage(false, listedEmail(failingEmailId), listedEmail(importedEmailId)));
        when(resendReceivedEmailClient.getReceivedEmail(failingEmailId))
                .thenThrow(new EmailDeliveryException("Email provider rejected the received email request", HttpStatus.NOT_FOUND.value()));
        when(resendReceivedEmailClient.getReceivedEmail(importedEmailId))
                .thenReturn(receivedEmail(importedEmailId, List.of("support@letz-speak.com")));

        mockMvc.perform(post("/api/admin/support-emails/sync").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.imported").value(1))
                .andExpect(jsonPath("$.data.failed").value(1));

        assertThat(supportEmailRepo.findByResendEmailId(importedEmailId)).isPresent();
    }

    @Test
    @WithMockUser(roles = "USER")
    void nonAdminCannotInvokeSync() throws Exception {
        mockMvc.perform(post("/api/admin/support-emails/sync").with(csrf()))
                .andExpect(status().isForbidden());

        verify(resendReceivedEmailClient, never()).listReceivedEmails(100, null);
    }

    private org.springframework.test.web.servlet.ResultActions postSignedWebhook(String payload) throws Exception {
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String messageId = "msg_" + Math.abs(payload.hashCode());
        return mockMvc.perform(post("/api/webhooks/resend")
                .contentType(MediaType.APPLICATION_JSON)
                .header("svix-id", messageId)
                .header("svix-timestamp", timestamp)
                .header("svix-signature", signature(messageId, timestamp, payload))
                .content(payload));
    }

    private String payload(String emailId) {
        return """
                {
                  "type": "email.received",
                  "created_at": "2026-09-21T10:00:01.000Z",
                  "data": {
                    "email_id": "%s"
                  }
                }
                """.formatted(emailId);
    }

    private ResendReceivedEmailDto receivedEmail(String emailId, List<String> recipients) {
        return receivedEmail(emailId, recipients, List.of());
    }

    private ResendReceivedEmailDto listedEmail(String emailId) {
        ResendReceivedEmailDto email = new ResendReceivedEmailDto();
        email.setId(emailId);
        return email;
    }

    private ResendReceivedEmailListDto receivedEmailPage(boolean hasMore, ResendReceivedEmailDto... emails) {
        ResendReceivedEmailListDto page = new ResendReceivedEmailListDto();
        page.setHas_more(hasMore);
        page.setData(List.of(emails));
        return page;
    }

    private ResendReceivedEmailDto receivedEmail(
            String emailId,
            List<String> recipients,
            List<ResendReceivedEmailAttachmentDto> attachments
    ) {
        ResendReceivedEmailDto email = new ResendReceivedEmailDto();
        email.setId(emailId);
        email.setFrom("learner@example.com");
        email.setTo(recipients);
        email.setSubject("Need help");
        email.setText("Plain support request");
        email.setHtml("<p>Plain support request</p>");
        email.setCreated_at("2026-09-21T10:00:00.000Z");
        email.setAttachments(attachments);
        return email;
    }

    private ResendReceivedEmailAttachmentDto attachment(String id) {
        ResendReceivedEmailAttachmentDto attachment = new ResendReceivedEmailAttachmentDto();
        attachment.setId(id);
        attachment.setFilename("invoice.pdf");
        attachment.setContent_type("application/pdf");
        attachment.setContent_disposition("attachment");
        attachment.setContent_id("cid-1");
        attachment.setSize(2048L);
        return attachment;
    }

    private SupportEmail savedEmail(String resendEmailId, String fromEmail, LocalDateTime receivedAt) {
        SupportEmail supportEmail = new SupportEmail();
        supportEmail.setResendEmailId(resendEmailId);
        supportEmail.setFromEmail(fromEmail);
        supportEmail.setToEmail("support@letz-speak.com");
        supportEmail.setSubject("Subject");
        supportEmail.setTextBody("Message body");
        supportEmail.setHtmlBody("<p>Message body</p>");
        supportEmail.setReceivedAt(receivedAt);
        return supportEmailRepo.save(supportEmail);
    }

    private SupportEmailAttachment savedAttachment(SupportEmail supportEmail, String resendAttachmentId) {
        SupportEmailAttachment attachment = new SupportEmailAttachment();
        attachment.setSupportEmail(supportEmail);
        attachment.setResendAttachmentId(resendAttachmentId);
        attachment.setFilename("invoice.pdf");
        attachment.setContentType("application/pdf");
        attachment.setContentDisposition("attachment");
        attachment.setContentId("cid-1");
        attachment.setSizeBytes(2048L);
        return supportEmailAttachmentRepo.save(attachment);
    }

    private String signature(String messageId, String timestamp, String payload) throws Exception {
        String signedContent = messageId + "." + timestamp + "." + payload;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(
                Base64.getDecoder().decode(WEBHOOK_SECRET.substring("whsec_".length())),
                "HmacSHA256"
        ));
        return "v1," + Base64.getEncoder().encodeToString(mac.doFinal(signedContent.getBytes(StandardCharsets.UTF_8)));
    }
}
