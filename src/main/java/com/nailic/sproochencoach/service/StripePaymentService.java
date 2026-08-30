package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.dto.StripeSessionURLDto;
import com.nailic.sproochencoach.model.AppUser;
import com.stripe.Stripe;
import com.stripe.StripeClient;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StripePaymentService {
    private static final Logger log = LoggerFactory.getLogger(StripePaymentService.class);
    @Value("${stripe.price-id}")
    private String stripePriceId;
    @Value("${stripe.sucess-url}")
    private String stripeSucessUrl;
    @Value("${stripe.cancel-url}")
    private String stripeCancelUrl;
    private final StripeClient stripeClient;
    @Value("${stripe.webhook-secret}")
    private String stripeWebhookSecret;

    public StripeSessionURLDto createStripeCheckoutSession(Authentication authentication) throws StripeException {

        if(authentication==null || !authentication.isAuthenticated()) {
            throw new UsernameNotFoundException("Authentication object is null , or user not authenticated");
        }
        AppUser user =  (AppUser)authentication.getPrincipal();


        SessionCreateParams params =
                SessionCreateParams.builder()
                        .setSuccessUrl(stripeSucessUrl)
                        .setCancelUrl(stripeCancelUrl)
                        .setCustomerEmail(user.getEmail())
                        .addLineItem(
                                SessionCreateParams.LineItem.builder()
                                        .setPrice(stripePriceId)
                                        .setQuantity(1L)
                                        .build()
                        )
                        .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                        .build();

        Session session = stripeClient.v1().checkout().sessions().create(params);

        return StripeSessionURLDto.builder().stripeSessionUrl(session.getUrl()).build();

    }

    public void constructEvent(String payload, String signature)
            throws SignatureVerificationException {

        Event event = Webhook.constructEvent(
                payload,
                signature,
                stripeWebhookSecret
        );

        String eventType = event.getType();
        String version = event.getApiVersion();
        StripeObject dataObject = event
                .getDataObjectDeserializer()
                .getObject()
                .orElse(null);
        log.info("Event API version: {}", event.getApiVersion());
        log.info("Stripe Java API version: {}", Stripe.API_VERSION);
        if ("checkout.session.completed".equals(eventType)
                && dataObject instanceof Session session) {

            String sessionId = session.getId();
            String paymentStatus = session.getPaymentStatus();
            String customerId = session.getCustomer();
            String subscriptionId = session.getSubscription();

            String customerEmail = null;

            if (session.getCustomerDetails() != null) {
                customerEmail = session.getCustomerDetails().getEmail();
            }

            log.info("Stripe event received: {}", eventType);
            log.info("Stripe session id: {}", sessionId);
            log.info("Stripe payment status: {}", paymentStatus);
            log.info("Stripe customer id: {}", customerId);
            log.info("Stripe customer email: {}", customerEmail);
            log.info("Stripe subscription id: {}", subscriptionId);
        }
    }
}
