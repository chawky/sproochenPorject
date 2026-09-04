package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.model.AppUser;
import com.nailic.sproochencoach.model.SubscriptionPlan;
import com.nailic.sproochencoach.repository.AppUserRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class UserPlanTierResolverTest {
    @Mock
    private LoggedInUser loggedInUser;

    @Mock
    private AppUserRepo appUserRepo;

    @Test
    void activeSubscriptionResolvesPremium() {
        UserPlanTierResolver resolver = resolver();
        AppUser user = userWithSubscriptionStatus("active");

        assertThat(resolver.resolve(user)).isEqualTo(UserPlanTier.PREMIUM);
    }

    @Test
    void trialingSubscriptionResolvesPremium() {
        UserPlanTierResolver resolver = resolver();
        AppUser user = userWithSubscriptionStatus("trialing");

        assertThat(resolver.resolve(user)).isEqualTo(UserPlanTier.PREMIUM);
    }

    @Test
    void missingSubscriptionResolvesBasic() {
        UserPlanTierResolver resolver = resolver();

        assertThat(resolver.resolve(new AppUser())).isEqualTo(UserPlanTier.BASIC);
    }

    @Test
    void inactiveSubscriptionResolvesBasic() {
        UserPlanTierResolver resolver = resolver();
        AppUser user = userWithSubscriptionStatus("canceled");

        assertThat(resolver.resolve(user)).isEqualTo(UserPlanTier.BASIC);
    }

    private UserPlanTierResolver resolver() {
        return new UserPlanTierResolver(
                loggedInUser,
                appUserRepo,
                new SubscriptionAccessService()
        );
    }

    private AppUser userWithSubscriptionStatus(String subscriptionStatus) {
        SubscriptionPlan subscriptionPlan = new SubscriptionPlan();
        subscriptionPlan.setSubscriptionStatus(subscriptionStatus);

        AppUser user = new AppUser();
        user.setSubscriptionPlan(subscriptionPlan);
        return user;
    }
}
