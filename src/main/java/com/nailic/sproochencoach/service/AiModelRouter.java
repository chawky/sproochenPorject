package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.exceptions.UserNotFoundException;
import com.nailic.sproochencoach.model.AppUser;
import com.nailic.sproochencoach.model.SubscriptionPlan;
import com.nailic.sproochencoach.repository.AppUserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiModelRouter {
    private final LoggedInUser loggedInUser;
    private final AppUserRepo appUserRepo;
    private final SubscriptionAccessService subscriptionAccessService;

    @Value("${ai.chat.basic.provider}")
    private String basicProvider;

    @Value("${ai.chat.basic.model}")
    private String basicModel;

    @Value("${ai.chat.premium.provider}")
    private String premiumProvider;

    @Value("${ai.chat.premium.model}")
    private String premiumModel;

    @Transactional(readOnly = true)
    public AiModelRoute currentUserRoute() {
        AppUser user = appUserRepo.findById(loggedInUser.getId())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (hasPremiumAccess(user)) {
            return new AiModelRoute(premiumProvider, premiumModel);
        }

        return new AiModelRoute(basicProvider, basicModel);
    }

    private boolean hasPremiumAccess(AppUser user) {
        SubscriptionPlan subscriptionPlan = user.getSubscriptionPlan();
        return subscriptionPlan != null
                && subscriptionAccessService.hasSubscriptionAccess(subscriptionPlan.getSubscriptionStatus());
    }
}
