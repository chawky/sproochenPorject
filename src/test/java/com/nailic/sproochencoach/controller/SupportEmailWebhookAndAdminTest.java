package com.nailic.sproochencoach.controller;

import com.nailic.sproochencoach.dto.ResendReceivedEmailDto;
import com.nailic.sproochencoach.model.SupportEmail;
import com.nailic.sproochencoach.repository.SupportEmailRepo;
import com.nailic.sproochencoach.service.ResendReceivedEmailClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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

    @MockitoBean
    private ResendReceivedEmailClient resendReceivedEmailClient;

    @BeforeEach
    void setUp() {
        supportEmailRepo.deleteAll();
    }

    @Test
    void validResendWebhookStoresSupportEmail() throws Exception {
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
                .thenReturn(receivedEmail(emailId, List.of("support@letz-speak.com")));

        postSignedWebhook(payload).andExpect(status().isOk());
        postSignedWebhook(payload).andExpect(status().isOk());

        assertThat(supportEmailRepo.count()).isEqualTo(1);
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
    @WithMockUser(roles = "ADMIN")
    void adminCanListReadAndMarkSupportEmailAsRead() throws Exception {
        SupportEmail older = savedEmail("old-email-id", "old@example.com", LocalDateTime.parse("2026-09-20T10:00:00"));
        SupportEmail newer = savedEmail("new-email-id", "new@example.com", LocalDateTime.parse("2026-09-21T10:00:00"));

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
                .andExpect(jsonPath("$.data.htmlBody").value("<p>Message body</p>"));

        mockMvc.perform(patch("/api/admin/support-emails/{id}/read", newer.getId()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.read").value(true));

        assertThat(supportEmailRepo.findById(newer.getId()).orElseThrow().isRead()).isTrue();
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
        ResendReceivedEmailDto email = new ResendReceivedEmailDto();
        email.setId(emailId);
        email.setFrom("learner@example.com");
        email.setTo(recipients);
        email.setSubject("Need help");
        email.setText("Plain support request");
        email.setHtml("<p>Plain support request</p>");
        email.setCreated_at("2026-09-21T10:00:00.000Z");
        return email;
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
