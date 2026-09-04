package com.nailic.sproochencoach.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "ai.quota")
@Validated
@Getter
@Setter
public class AiQuotaProperties {
    @Valid
    private TierQuota basic = new TierQuota();

    @Valid
    private TierQuota premium = new TierQuota();

    @Getter
    @Setter
    public static class TierQuota {
        @Valid
        private FeatureQuota chat = new FeatureQuota();

        @Valid
        private FeatureQuota tts = new FeatureQuota();

        @Valid
        private FeatureQuota stt = new FeatureQuota();

        @Valid
        private FeatureQuota image = new FeatureQuota();
    }

    @Getter
    @Setter
    public static class FeatureQuota {
        @Min(0)
        private Integer dailyLimit;

        @Min(0)
        private Integer monthlyLimit;
    }
}
