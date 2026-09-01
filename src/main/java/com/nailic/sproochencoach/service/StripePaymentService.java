package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.dto.StripeSessionURLDto;
import com.nailic.sproochencoach.exceptions.StripePaymentException;
import com.nailic.sproochencoach.model.AppUser;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class StripePaymentService {
    private static final Logger log = LoggerFactory.getLogger(StripePaymentService.class);

    private final StripeClient stripeClient;
    private final LoggedInUser loggedInUser;

    @Value("${stripe.price-id}")
    private String stripePriceId;
    @Value("${stripe.success-url}")
    private String stripeSuccessUrl;
    @Value("${stripe.cancel-url}")
    private String stripeCancelUrl;
    @Value("${stripe.webhook-secret}")
    private String stripeWebhookSecret;

    public StripeSessionURLDto createStripeCheckoutSession() {
        AppUser user = loggedInUser.get();

        try {
            SessionCreateParams params =
                    SessionCreateParams.builder()
                            .setSuccessUrl(stripeSuccessUrl)
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
            String sessionUrl = session.getUrl();

            if (!StringUtils.hasText(sessionUrl)) {
                log.error(
                        "Stripe checkout session created without redirect URL. userId={}, sessionId={}",
                        user.getId(),
                        session.getId()
                );
                throw new StripePaymentException(
                        HttpStatus.BAD_GATEWAY.value(),
                        "Stripe checkout session did not include a redirect URL"
                );
            }

            log.debug(
                    "Stripe checkout session created. userId={}, sessionId={}",
                    user.getId(),
                    session.getId()
            );

            return StripeSessionURLDto.builder()
                    .stripeSessionUrl(sessionUrl)
                    .build();
        } catch (StripeException exception) {
            log.error(
                    "Stripe checkout session creation failed. userId={}, stripeStatusCode={}, stripeRequestId={}",
                    user.getId(),
                    exception.getStatusCode(),
                    exception.getRequestId(),
                    exception
            );
            throw new StripePaymentException(
                    HttpStatus.BAD_GATEWAY.value(),
                    "Unable to create Stripe checkout session"
            );
        }
    }

    public void handleWebhook(String payload, String signature) {
        log.info("Stripe webhook request received. payloadLength={}", payload.length());

        Event event = constructEvent(payload, signature);
        String eventType = event.getType();

        if (!"checkout.session.completed".equals(eventType)) {
            log.debug("Ignoring unsupported Stripe event. eventType={}", eventType);
            return;
        }

        StripeObject dataObject = event
                .getDataObjectDeserializer()
                .getObject()
                .orElse(null);

        if (!(dataObject instanceof Session session)) {
            log.error(
                    "Stripe checkout session event could not be deserialized. eventType={}, eventApiVersion={}",
                    eventType,
                    event.getApiVersion()
            );
            throw new StripePaymentException(
                    HttpStatus.BAD_REQUEST.value(),
                    "Stripe webhook payload could not be processed"
            );
        }

        log.info(
                "Stripe checkout completed. sessionId={}, paymentStatus={}, customerId={}, customerEmail={}, subscriptionId={}",
                session.getId(),
                session.getPaymentStatus(),
                session.getCustomer(),
                customerEmail(session),
                session.getSubscription()
        );
    }

    private Event constructEvent(String payload, String signature) {
        try {
            return Webhook.constructEvent(
                    payload,
                    signature,
                    stripeWebhookSecret
            );
        } catch (SignatureVerificationException exception) {
            log.warn("Rejected Stripe webhook because signature verification failed");
            throw new StripePaymentException(
                    HttpStatus.BAD_REQUEST.value(),
                    "Invalid Stripe webhook signature"
            );
        } catch (RuntimeException exception) {
            log.warn("Rejected Stripe webhook because payload could not be parsed", exception);
            throw new StripePaymentException(
                    HttpStatus.BAD_REQUEST.value(),
                    "Invalid Stripe webhook payload"
            );
        }
    }

    private String customerEmail(Session session) {
        if (session.getCustomerDetails() == null) {
            return null;
        }

        return session.getCustomerDetails().getEmail();
    }
}
