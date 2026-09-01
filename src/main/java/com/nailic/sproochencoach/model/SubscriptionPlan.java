package com.nailic.sproochencoach.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
public class SubscriptionPlan extends BaseModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column
    private String stripeCustomerId;
    @Column
    private String stripeSubscriptionId;
    @Column
    private String paymentStatus;
    @Column
    private String subscriptionStatus;
    @Column
    private LocalDate startedAt;
    @Column
    private LocalDate currentPeriodEnd;
}
