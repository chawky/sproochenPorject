package com.nailic.sproochencoach.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiModelRouter {
    private final UserPlanTierResolver userPlanTierResolver;

    @Value("${ai.chat.basic.provider}")
    private String basicProvider;

    @Value("${ai.chat.basic.model}")
    private String basicModel;

    @Value("${ai.chat.premium.provider}")
    private String premiumProvider;

    @Value("${ai.chat.premium.model}")
    private String premiumModel;

    public AiModelRoute currentUserRoute() {
        return switch (userPlanTierResolver.currentUserTier()) {
            case PREMIUM -> new AiModelRoute(premiumProvider, premiumModel);
            case BASIC -> new AiModelRoute(basicProvider, basicModel);
        };
    }
}
