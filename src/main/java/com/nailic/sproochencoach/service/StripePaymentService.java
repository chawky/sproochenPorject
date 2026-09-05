package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.constants.AppConstants;
import com.nailic.sproochencoach.dto.StripeSessionURLDto;
import com.nailic.sproochencoach.exceptions.StripePaymentException;
import com.nailic.sproochencoach.model.AppUser;
import com.nailic.sproochencoach.model.ProcessedStripeEvent;
import com.nailic.sproochencoach.model.SubscriptionPlan;
import com.nailic.sproochencoach.repository.AppUserRepo;
import com.nailic.sproochencoach.repository.ProcessedStripeEventRepo;
import com.nailic.sproochencoach.repository.SubscriptionPlanRepo;
import com.stripe.StripeClient;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class StripePaymentService {
    private static final Logger log = LoggerFactory.getLogger(StripePaymentService.class);
    private static final String CHECKOUT_SESSION_COMPLETED = "checkout.session.completed";
    private static final String CUSTOMER_SUBSCRIPTION_UPDATED =
            "customer.subscription.updated";
    private static final String CUSTOMER_SUBSCRIPTION_DELETED =
            "customer.subscription.deleted";
    private final StripeClient stripeClient;
    private final LoggedInUser loggedInUser;
    private final AppUserRepo appUserRepo;
    private final ProcessedStripeEventRepo processedStripeEventRepo;
    private final TransactionTemplate transactionTemplate;
    private final SubscriptionPlanRepo subscriptionPlanRepo;
    private final SubscriptionAccessService subscriptionAccessService;
    @Value(AppConstants.PropertyPlaceholders.STRIPE_PRICE_ID)
    private String stripePriceId;
    @Value(AppConstants.PropertyPlaceholders.STRIPE_SUCCESS_URL)
    private String stripeSuccessUrl;
    @Value(AppConstants.PropertyPlaceholders.STRIPE_CANCEL_URL)
    private String stripeCancelUrl;
    @Value(AppConstants.PropertyPlaceholders.STRIPE_WEBHOOK_SECRET)
    private String stripeWebhookSecret;

    public StripeSessionURLDto createStripeCheckoutSession() {
        AppUser user = loggedInUser.get();
        boolean userAlreadyHasAccess = appUserRepo.findSubscriptionPlanByUserId(user.getId())
                .map(subscriptionPlan -> subscriptionAccessService.hasSubscriptionAccess(
                        subscriptionPlan.getSubscriptionStatus()
                ))
                .orElse(false);

        if (userAlreadyHasAccess) {
            throw new StripePaymentException(HttpStatus.CONFLICT.value(), "user already subscribed");
        }

        try {
            SessionCreateParams params =
                    SessionCreateParams.builder()
                            .setSuccessUrl(stripeSuccessUrl)
                            .setClientReferenceId(String.valueOf(user.getId()))
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
                log.error("Stripe checkout session created without redirect URL. userId={}, sessionId={}", user.getId(), session.getId());
                throw new StripePaymentException(
                        HttpStatus.BAD_GATEWAY.value(),
                        "Stripe checkout session did not include a redirect URL"
                );
            }

            log.debug("Stripe checkout session created. userId={}, sessionId={}", user.getId(), session.getId());

            return StripeSessionURLDto.builder()
                    .stripeSessionUrl(sessionUrl)
                    .build();
        } catch (StripeException exception) {
            log.error("Stripe checkout session creation failed. userId={}, stripeStatusCode={}, stripeRequestId={}", user.getId(), exception.getStatusCode(), exception.getRequestId(), exception);
            throw new StripePaymentException(
                    HttpStatus.BAD_GATEWAY.value(),
                    "Unable to create Stripe checkout session"
            );
        }
    }

    public void handleWebhook(String payload, String signature) {
        Event event = constructEvent(payload, signature);

        Boolean processed = transactionTemplate.execute(status -> {
            if (!recordProcessedEvent(event)) {
                status.setRollbackOnly();
                return false;
            }

            processStripeEvent(event);
            return true;
        });

        if (Boolean.FALSE.equals(processed)) {
            log.debug("Stripe event already processed. eventId={}", event.getId());
        }
    }
    public void cancelSubscription() {
        SubscriptionPlan subscriptionPlan = getLoggedInUserSubscriptionPlan();
        String stripeSubscriptionId = subscriptionPlan.getStripeSubscriptionId();

        try {
            stripeClient.v1()
                    .subscriptions()
                    .cancel(stripeSubscriptionId);
        } catch (StripeException exception) {
            throw new StripePaymentException(
                    HttpStatus.BAD_GATEWAY.value(),
                    "Unable to cancel subscription"
            );
        }
    }
    public void updateSubscription() {
        SubscriptionPlan subscriptionPlan = getLoggedInUserSubscriptionPlan();
        String stripeSubscriptionId = subscriptionPlan.getStripeSubscriptionId();

        try {
            stripeClient.v1()
                    .subscriptions()
                    .update(stripeSubscriptionId);
        } catch (StripeException exception) {
            throw new StripePaymentException(
                    HttpStatus.BAD_GATEWAY.value(),
                    "Unable to update subscription"
            );
        }
    }

    private SubscriptionPlan getLoggedInUserSubscriptionPlan() {
        return appUserRepo.findSubscriptionPlanByUserId(loggedInUser.getId())
                .orElseThrow(() -> new StripePaymentException(
                        HttpStatus.BAD_REQUEST.value(),
                        "User has no subscription"
                ));
    }

    public void processStripeEvent(Event event) {
        switch (event.getType()) {
            case CHECKOUT_SESSION_COMPLETED -> handleCompletedEventSubscription(event);

            case CUSTOMER_SUBSCRIPTION_UPDATED -> handleUpdatedEventSubscription(event);

            case CUSTOMER_SUBSCRIPTION_DELETED -> handleDeletedEventSubscription(event);

            default -> log.debug("Ignoring unsupported Stripe event. eventType={}", event.getType());
        }
    }

    private void handleDeletedEventSubscription(Event event) {
        StripeObject dataObject = event
                .getDataObjectDeserializer()
                .getObject()
                .orElse(null);

        if (!(dataObject instanceof Subscription stripeSubscription)) {
            throw new StripePaymentException(
                    HttpStatus.BAD_REQUEST.value(),
                    "Stripe webhook payload could not be processed"
            );
        }

        SubscriptionPlan localSub = subscriptionPlanRepo
                .findByStripeSubscriptionId(stripeSubscription.getId())
                .orElseThrow(() -> new StripePaymentException(
                        HttpStatus.BAD_REQUEST.value(),
                        "Local subscription not found"
                ));

        localSub.setSubscriptionStatus(stripeSubscription.getStatus());

        subscriptionPlanRepo.save(localSub);
    }

    private void handleUpdatedEventSubscription(Event event) {
        StripeObject dataObject = event
                .getDataObjectDeserializer()
                .getObject()
                .orElse(null);

        if (!(dataObject instanceof Subscription stripeSubscription)) {
            log.error(
                    "Stripe subscription event could not be deserialized. eventType={}, eventApiVersion={}",
                    event.getType(),
                    event.getApiVersion()
            );

            throw new StripePaymentException(
                    HttpStatus.BAD_REQUEST.value(),
                    "Stripe webhook payload could not be processed"
            );
        }

        SubscriptionPlan localSub = subscriptionPlanRepo
                .findByStripeSubscriptionId(stripeSubscription.getId())
                .orElseThrow(() -> new StripePaymentException(
                        HttpStatus.BAD_REQUEST.value(),
                        "Local subscription not found"
                ));

        localSub.setSubscriptionStatus(stripeSubscription.getStatus());

        Long currentPeriodEnd = stripeSubscription
                .getItems()
                .getData()
                .get(0)
                .getCurrentPeriodEnd();

        localSub.setCurrentPeriodEnd(
                Instant.ofEpochSecond(currentPeriodEnd)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
        );

        subscriptionPlanRepo.save(localSub);
    }

    private void handleCompletedEventSubscription(Event event) {
        String eventType = event.getType();
        StripeObject dataObject = event
                .getDataObjectDeserializer()
                .getObject()
                .orElse(null);

        if (!(dataObject instanceof Session session)) {
            log.error("Stripe checkout session event could not be deserialized. eventType={}, eventApiVersion={}", eventType, event.getApiVersion());
            throw new StripePaymentException(
                    HttpStatus.BAD_REQUEST.value(),
                    "Stripe webhook payload could not be processed"
            );
        }

        AppUser user = findCheckoutUser(session);
        if (user == null) {
            return;
        }

        if (!"paid".equals(session.getPaymentStatus())) {
            return;
        }

        Subscription stripeSubscription = retrieveSubscription(session);

        SubscriptionPlan subscriptionPlan = new SubscriptionPlan();

        subscriptionPlan.setStripeSubscriptionId(session.getSubscription());
        subscriptionPlan.setStripeCustomerId(session.getCustomer());
        subscriptionPlan.setPaymentStatus(session.getPaymentStatus());
        subscriptionPlan.setSubscriptionStatus(stripeSubscription.getStatus());

        subscriptionPlan.setStartedAt(
                Instant.ofEpochSecond(stripeSubscription.getCreated())
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
        );

        Long currentPeriodEnd = stripeSubscription
                .getItems()
                .getData()
                .get(0)
                .getCurrentPeriodEnd();

        subscriptionPlan.setCurrentPeriodEnd(
                Instant.ofEpochSecond(currentPeriodEnd)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
        );

        user.setSubscriptionPlan(subscriptionPlan);
        appUserRepo.save(user);
        log.info("Stripe checkout completed. sessionId={}, paymentStatus={}, customerId={}, clientReferenceId={}, customerEmail={}, subscriptionId={}", session.getId(), session.getPaymentStatus(), session.getCustomer(), session.getClientReferenceId(), customerEmail(session), session.getSubscription());
    }

    private boolean recordProcessedEvent(Event event) {
        ProcessedStripeEvent processedEvent = new ProcessedStripeEvent();
        processedEvent.setEventId(event.getId());
        processedEvent.setEventType(event.getType());
        processedEvent.setProcessedAt(LocalDateTime.now());

        try {
            processedStripeEventRepo.saveAndFlush(processedEvent);
            return true;
        } catch (DataIntegrityViolationException exception) {
            return false;
        }
    }

    private AppUser findCheckoutUser(Session session) {
        Integer userId = checkoutUserId(session);

        if (userId == null) {
            return null;
        }

        AppUser user = appUserRepo.findById(userId).orElse(null);

        if (user == null) {
            log.warn("Stripe checkout ignored because user was not found. userId={}, sessionId={}", userId, session.getId());
        }

        return user;
    }

    private Integer checkoutUserId(Session session) {
        String clientReferenceId = session.getClientReferenceId();

        if (!StringUtils.hasText(clientReferenceId)) {
            log.warn("Stripe checkout ignored because client reference is missing. sessionId={}", session.getId());
            return null;
        }

        try {
            return Integer.valueOf(clientReferenceId);
        } catch (NumberFormatException exception) {
            log.warn("Stripe checkout ignored because client reference is invalid. sessionId={}, clientReferenceId={}", session.getId(), clientReferenceId);
            return null;
        }
    }

    private Subscription retrieveSubscription(Session session) {
        String subscriptionId = session.getSubscription();

        if (!StringUtils.hasText(subscriptionId)) {
            throw new StripePaymentException(
                    HttpStatus.BAD_REQUEST.value(),
                    "Stripe checkout session did not include a subscription"
            );
        }

        try {
            return stripeClient.v1().subscriptions().retrieve(subscriptionId);
        } catch (StripeException exception) {
            log.error("Stripe subscription retrieval failed. subscriptionId={}, stripeStatusCode={}, stripeRequestId={}", subscriptionId, exception.getStatusCode(), exception.getRequestId(), exception);
            throw new StripePaymentException(
                    HttpStatus.BAD_GATEWAY.value(),
                    "Unable to retrieve Stripe subscription"
            );
        }
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
            log.warn("Rejected Stripe webhook because payload could not be parsed. reason={}", exception.getMessage());
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
