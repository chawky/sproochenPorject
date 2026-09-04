package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.model.AiUsage;
import com.nailic.sproochencoach.model.AppUser;
import com.nailic.sproochencoach.repository.AiUsageRepo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiUsageServiceTest {
    @Mock
    private AiUsageRepo aiUsageRepo;

    @Mock
    private AiUsageCostService aiUsageCostService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recordsSuccessfulChatRequestEvenWhenTokenUsageIsMissing() {
        AppUser user = new AppUser();
        user.setId(42);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );
        when(aiUsageRepo.save(any(AiUsage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AiUsageService service = new AiUsageService(aiUsageRepo, aiUsageCostService);
        service.recordChatUsage("openrouter", "openrouter/free", "text exercise", null, null, null);

        ArgumentCaptor<AiUsage> usageCaptor = ArgumentCaptor.forClass(AiUsage.class);
        verify(aiUsageRepo).save(usageCaptor.capture());
        AiUsage usage = usageCaptor.getValue();

        assertThat(usage.getUserId()).isEqualTo(42);
        assertThat(usage.getProvider()).isEqualTo("openrouter");
        assertThat(usage.getUsageUnit()).isEqualTo("TOKEN");
        assertThat(usage.getUsageAmount()).isNull();
    }
}
