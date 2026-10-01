package com.nailic.sproochencoach.controller;

import com.nailic.sproochencoach.dto.AiQuotaFeatureStatusDto;
import com.nailic.sproochencoach.dto.AiQuotaStatusDto;
import com.nailic.sproochencoach.service.AiQuotaService;
import com.nailic.sproochencoach.service.AppUserService;
import com.nailic.sproochencoach.service.ClientIpResolver;
import com.nailic.sproochencoach.service.EmailAndOtpService;
import com.nailic.sproochencoach.service.GoogleLoginService;
import com.nailic.sproochencoach.service.JwtCookieService;
import com.nailic.sproochencoach.service.LuxembourgLocationService;
import com.nailic.sproochencoach.service.PasswordResetService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AiQuotaControllerTest {
    @Test
    void currentUserQuotaReturnsFeatureLevelWeeklyStatus() throws Exception {
        AiQuotaService quotaService = mock(AiQuotaService.class);
        when(quotaService.getCurrentUserQuotaStatus()).thenReturn(new AiQuotaStatusDto(
                "BASIC",
                List.of(new AiQuotaFeatureStatusDto(
                        "SPEAKING", "weekly", 15, 4, 11L,
                        LocalDateTime.of(2026, 9, 8, 0, 0),
                        LocalDateTime.of(2026, 9, 15, 0, 0)
                ))
        ));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AppUserController(
                mock(AppUserService.class),
                mock(EmailAndOtpService.class),
                mock(LuxembourgLocationService.class),
                quotaService,
                mock(PasswordResetService.class),
                mock(JwtCookieService.class),
                mock(ClientIpResolver.class),
                mock(GoogleLoginService.class)
        )).build();

        mockMvc.perform(get("/api/users/me/ai-quota"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tier").value("BASIC"))
                .andExpect(jsonPath("$.data.features[0].feature").value("SPEAKING"))
                .andExpect(jsonPath("$.data.features[0].weeklyLimit").value(15))
                .andExpect(jsonPath("$.data.features[0].used").value(4))
                .andExpect(jsonPath("$.data.features[0].remaining").value(11))
                .andExpect(jsonPath("$.data.features[0].window").value("weekly"))
                .andExpect(jsonPath("$.data.features[0].windowStart").value("2026-09-08T00:00:00"))
                .andExpect(jsonPath("$.data.features[0].windowEnd").value("2026-09-15T00:00:00"));
    }
}
