package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.constants.AppConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiModelRouter {
    private final UserPlanTierResolver userPlanTierResolver;

    @Value(AppConstants.PropertyPlaceholders.AI_CHAT_BASIC_PROVIDER)
    private String basicProvider;

    @Value(AppConstants.PropertyPlaceholders.AI_CHAT_BASIC_MODEL)
    private String basicModel;

    @Value(AppConstants.PropertyPlaceholders.AI_CHAT_PREMIUM_PROVIDER)
    private String premiumProvider;

    @Value(AppConstants.PropertyPlaceholders.AI_CHAT_PREMIUM_MODEL)
    private String premiumModel;

    public AiModelRoute currentUserRoute() {
        return switch (userPlanTierResolver.currentUserTier()) {
            case PREMIUM -> new AiModelRoute(premiumProvider, premiumModel);
            case BASIC -> new AiModelRoute(basicProvider, basicModel);
        };
    }
}
