package com.nailic.sproochencoach.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class AiModelRouterTest {
    @Test
    void basicAndPremiumShareThePaidKimiRoute() {
        AiModelRouter router = new AiModelRouter();
        ReflectionTestUtils.setField(router, "provider", "kimi");
        ReflectionTestUtils.setField(router, "model", "moonshotai/kimi-k3");

        AiModelRoute basicRoute = router.currentUserRoute();
        AiModelRoute premiumRoute = router.currentUserRoute();

        assertThat(basicRoute).isEqualTo(new AiModelRoute("kimi", "moonshotai/kimi-k3"));
        assertThat(premiumRoute).isEqualTo(basicRoute);
        assertThat(basicRoute.model()).isNotEqualTo("openrouter/free");
    }
}
