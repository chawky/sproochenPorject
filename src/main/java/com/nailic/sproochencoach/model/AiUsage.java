package com.nailic.sproochencoach.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_usage")
@Getter
@Setter
@NoArgsConstructor
public class AiUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(nullable = false)
    private String provider;

    @Column(nullable = false)
    private String model;

    @Column(nullable = false)
    private String requestName;

    @Column
    private Integer inputTokens;

    @Column
    private Integer outputTokens;

    @Column
    private Integer totalTokens;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
