package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.constants.AppConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AiModelRouter {
    @Value(AppConstants.PropertyPlaceholders.AI_CHAT_PROVIDER)
    private String provider;

    @Value(AppConstants.PropertyPlaceholders.AI_CHAT_MODEL)
    private String model;

    public AiModelRoute currentUserRoute() {
        return new AiModelRoute(provider, model);
    }
}
