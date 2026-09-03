package com.nailic.sproochencoach.repository;

import com.nailic.sproochencoach.model.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubscriptionPlanRepo
        extends JpaRepository<SubscriptionPlan, Long> {
    Optional<SubscriptionPlan> findByStripeSubscriptionId(String stripeSubscriptionId);
}