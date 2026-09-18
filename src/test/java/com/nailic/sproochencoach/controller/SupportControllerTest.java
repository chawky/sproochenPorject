package com.nailic.sproochencoach.controller;

import com.nailic.sproochencoach.exceptions.EmailDeliveryException;
import com.nailic.sproochencoach.exceptions.GlobalExceptionHandler;
import com.nailic.sproochencoach.exceptions.SupportRateLimitExceededException;
import com.nailic.sproochencoach.service.ClientIpResolver;
import com.nailic.sproochencoach.service.SupportService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SupportControllerTest {
    @Test
    void sendSupportRequestUsesForwardedClientIp() throws Exception {
        SupportService supportService = mock(SupportService.class);
        ClientIpResolver clientIpResolver = mock(ClientIpResolver.class);
        when(clientIpResolver.resolve(any())).thenReturn("203.0.113.10");
        MockMultipartFile attachment = new MockMultipartFile(
                "attachments",
                "screenshot.png",
                "image/png",
                new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}
        );

        mockMvc(supportService, clientIpResolver).perform(multipart("/api/support")
                        .file(attachment)
                        .part(textPart("email", "user@example.com"))
                        .part(textPart("subject", "Subscription problem"))
                        .part(textPart("message", "My subscription page is showing the wrong status."))
                        .header("X-Forwarded-For", "203.0.113.10, 10.0.0.5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Support request sent successfully."));

        verify(supportService).sendSupportRequest(
                eq("user@example.com"),
                eq("Subscription problem"),
                eq("My subscription page is showing the wrong status."),
                anyList(),
                eq("203.0.113.10")
        );
    }

    @Test
    void providerFailureReturnsBadGateway() throws Exception {
        SupportService supportService = mock(SupportService.class);
        doThrow(new EmailDeliveryException("Email provider rejected the request", HttpStatus.BAD_GATEWAY.value()))
                .when(supportService)
                .sendSupportRequest(any(), any(), any(), any(), any());

        mockMvc(supportService, mock(ClientIpResolver.class)).perform(multipart("/api/support")
                        .part(textPart("email", "user@example.com"))
                        .part(textPart("subject", "Bug"))
                        .part(textPart("message", "Message")))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("We could not send the email right now. Please try again."));
    }

    @Test
    void rateLimitFailureReturnsTooManyRequests() throws Exception {
        SupportService supportService = mock(SupportService.class);
        doThrow(new SupportRateLimitExceededException("Support request rate limit exceeded"))
                .when(supportService)
                .sendSupportRequest(any(), any(), any(), any(), any());

        mockMvc(supportService, mock(ClientIpResolver.class)).perform(multipart("/api/support")
                        .part(textPart("email", "user@example.com"))
                        .part(textPart("subject", "Bug"))
                        .part(textPart("message", "Message")))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Too many support requests. Please wait and try again."));
    }

    private MockMvc mockMvc(SupportService supportService, ClientIpResolver clientIpResolver) {
        return MockMvcBuilders
                .standaloneSetup(new SupportController(supportService, clientIpResolver))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private MockPart textPart(String name, String value) {
        return new MockPart(name, value.getBytes(StandardCharsets.UTF_8));
    }
}
