package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.exceptions.UserNotFoundException;
import com.nailic.sproochencoach.model.AppUser;
import com.nailic.sproochencoach.model.SubscriptionPlan;
import com.nailic.sproochencoach.repository.AppUserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserPlanTierResolver {
    private final LoggedInUser loggedInUser;
    private final AppUserRepo appUserRepo;
    private final SubscriptionAccessService subscriptionAccessService;

    @Transactional(readOnly = true)
    public UserPlanTier currentUserTier() {
        AppUser user = appUserRepo.findById(loggedInUser.getId())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        return resolve(user);
    }

    public UserPlanTier resolve(AppUser user) {
        SubscriptionPlan subscriptionPlan = user.getSubscriptionPlan();
        if (subscriptionPlan != null
                && subscriptionAccessService.hasSubscriptionAccess(subscriptionPlan.getSubscriptionStatus())) {
            return UserPlanTier.PREMIUM;
        }

        return UserPlanTier.BASIC;
    }
}
