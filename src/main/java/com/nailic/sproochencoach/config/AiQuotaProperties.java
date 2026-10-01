package com.nailic.sproochencoach.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
    private BasicQuota basic = new BasicQuota();

    @Getter
    @Setter
    public static class BasicQuota {
        @Valid
        private FeatureQuota speaking = new FeatureQuota();

        @Valid
        private FeatureQuota listening = new FeatureQuota();

        @Valid
        private FeatureQuota imageDescription = new FeatureQuota();

        @Valid
        private FeatureQuota vocabulary = new FeatureQuota();

        @Valid
        private FeatureQuota topicExercise = new FeatureQuota();
    }

    @Getter
    @Setter
    public static class FeatureQuota {
        @Min(0)
        @NotNull
        private Integer weeklyLimit;
    }
}
