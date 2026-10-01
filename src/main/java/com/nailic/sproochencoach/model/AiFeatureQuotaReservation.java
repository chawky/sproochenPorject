package com.nailic.sproochencoach.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_feature_quota_reservations")
@Getter
@Setter
@NoArgsConstructor
public class AiFeatureQuotaReservation {
    @Id
    private String id;
    @Column(nullable = false)
    private Integer userId;
    @Column(nullable = false)
    private String feature;
    @Column(nullable = false)
    private LocalDateTime windowStart;
    @Column(nullable = false)
    private LocalDateTime createdAt;
}
