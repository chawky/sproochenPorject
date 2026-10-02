package com.nailic.sproochencoach.config;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "ai.leases")
@Validated
@Getter
@Setter
public class AiLeaseProperties {
    @NotNull
    private Duration quotaReservationTimeout = Duration.ofMinutes(10);

    @NotNull
    private Duration evaluationClaimTimeout = Duration.ofMinutes(10);
}
