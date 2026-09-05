package com.nailic.sproochencoach.config;

import com.nailic.sproochencoach.constants.AppConstants;
import com.stripe.StripeClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StripeConfig {

    @Value(AppConstants.PropertyPlaceholders.STRIPE_API_KEY)
    private String apiKey;

    @Bean
    public StripeClient stripeClient() {
        return new StripeClient(apiKey);
    }
}
