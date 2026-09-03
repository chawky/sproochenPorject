package com.nailic.sproochencoach.service;

import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;

@Service
public class SubscriptionAccessService {
    private static final Set<String> ACCESS_GRANTED_SUBSCRIPTION_STATUSES = Set.of("active", "trialing");

    public boolean hasSubscriptionAccess(String subscriptionStatus) {
        return subscriptionStatus != null
                && ACCESS_GRANTED_SUBSCRIPTION_STATUSES.contains(subscriptionStatus.toLowerCase(Locale.ROOT));
    }
}
