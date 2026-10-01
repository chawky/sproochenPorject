package com.nailic.sproochencoach.config;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "ai.rate-limit")
@Validated
@Getter
@Setter
public class AiRateLimitProperties {
    @Min(1)
    private int maxRequestsPerMinute = 20;
}
