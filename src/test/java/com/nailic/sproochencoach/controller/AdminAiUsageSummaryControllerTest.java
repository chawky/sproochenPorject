package com.nailic.sproochencoach.controller;

import com.nailic.sproochencoach.dto.AdminAiUsageDashboardSummaryDto;
import com.nailic.sproochencoach.model.AiUsage;
import com.nailic.sproochencoach.repository.AiUsageRepo;
import com.nailic.sproochencoach.service.AiUsageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminAiUsageSummaryControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AiUsageRepo aiUsageRepo;

    @Autowired
    private AiUsageService aiUsageService;

    @BeforeEach
    void setUp() {
        aiUsageRepo.deleteAll();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void summarizesAiUsageByProviderAndModel() throws Exception {
        saveUsage("kimi", "shared-model", 10, 5, 15, new BigDecimal("0.10"));
        saveUsage("kimi", "shared-model", 7, 3, 10, new BigDecimal("0.20"));
        saveUsage("openrouter", "shared-model", 1, 2, 3, new BigDecimal("0.30"));
        saveUsage("elevenlabs", "eleven_multilingual_v2", null, null, null, new BigDecimal("0.40"));
        saveUsage("groq", "whisper-large-v3", null, null, null, null);

        mockMvc.perform(get("/api/admin/ai-usage/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.models.length()").value(4))
                .andExpect(jsonPath("$.data.models[?(@.provider=='kimi' && @.model=='shared-model')].requests").value(2))
                .andExpect(jsonPath("$.data.models[?(@.provider=='kimi' && @.model=='shared-model')].inputTokens").value(17))
                .andExpect(jsonPath("$.data.models[?(@.provider=='kimi' && @.model=='shared-model')].outputTokens").value(8))
                .andExpect(jsonPath("$.data.models[?(@.provider=='kimi' && @.model=='shared-model')].totalTokens").value(25))
                .andExpect(jsonPath("$.data.models[?(@.provider=='openrouter' && @.model=='shared-model')].requests").value(1))
                .andExpect(jsonPath("$.data.models[?(@.provider=='elevenlabs' && @.model=='eleven_multilingual_v2')].inputTokens").value(0))
                .andExpect(jsonPath("$.data.models[?(@.provider=='groq' && @.model=='whisper-large-v3')].totalTokens").value(0))
                .andExpect(jsonPath("$.data.totals.requests").value(5))
                .andExpect(jsonPath("$.data.totals.inputTokens").value(18))
                .andExpect(jsonPath("$.data.totals.outputTokens").value(10))
                .andExpect(jsonPath("$.data.totals.totalTokens").value(28));

        AdminAiUsageDashboardSummaryDto summary = aiUsageService.getAdminAiUsageSummary();
        BigDecimal groupedTotal = summary.models()
                .stream()
                .map(model -> model.estimatedCostUsd())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(summary.totals().estimatedCostUsd()).isEqualByComparingTo(new BigDecimal("1.00"));
        assertThat(summary.totals().estimatedCostUsd()).isEqualByComparingTo(groupedTotal);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void emptyAiUsageReturnsZeroTotals() throws Exception {
        mockMvc.perform(get("/api/admin/ai-usage/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.models.length()").value(0))
                .andExpect(jsonPath("$.data.totals.requests").value(0))
                .andExpect(jsonPath("$.data.totals.inputTokens").value(0))
                .andExpect(jsonPath("$.data.totals.outputTokens").value(0))
                .andExpect(jsonPath("$.data.totals.totalTokens").value(0))
                .andExpect(jsonPath("$.data.totals.estimatedCostUsd").value(0));
    }

    @Test
    @WithMockUser(roles = "USER")
    void nonAdminCannotAccessAiUsageSummary() throws Exception {
        mockMvc.perform(get("/api/admin/ai-usage/summary"))
                .andExpect(status().isForbidden());
    }

    private void saveUsage(
            String provider,
            String model,
            Integer inputTokens,
            Integer outputTokens,
            Integer totalTokens,
            BigDecimal estimatedCostUsd
    ) {
        AiUsage usage = new AiUsage();
        usage.setUserId(42);
        usage.setProvider(provider);
        usage.setModel(model);
        usage.setRequestName("test request");
        usage.setInputTokens(inputTokens);
        usage.setOutputTokens(outputTokens);
        usage.setTotalTokens(totalTokens);
        usage.setEstimatedCostUsd(estimatedCostUsd);
        aiUsageRepo.save(usage);
    }
}
