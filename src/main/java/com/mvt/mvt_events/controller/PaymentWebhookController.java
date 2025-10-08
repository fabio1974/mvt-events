package com.mvt.mvt_events.controller;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments/webhook")
@Tag(name = "Webhooks de Pagamento", description = "Endpoints para webhooks de gateways de pagamento (acesso público)")
@Slf4j
public class PaymentWebhookController {

    @Value("${payment.stripe.webhook-secret:}")
    private String stripeWebhookSecret;

    @PostMapping("/stripe")
    @Operation(summary = "Webhook Stripe", description = "Endpoint público para notificações do Stripe")
    public ResponseEntity<?> stripeWebhook(@RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {
        try {
            log.info("Received Stripe webhook");

            if (stripeWebhookSecret.isEmpty()) {
                log.warn("Stripe webhook secret not configured");
                return ResponseEntity.ok().build();
            }

            // Verify webhook signature
            Event event = Webhook.constructEvent(payload, signature, stripeWebhookSecret);

            log.info("Processing Stripe webhook event: {}", event.getType());

            // Process different event types
            switch (event.getType()) {
                case "payment_intent.succeeded":
                    handlePaymentIntentSucceeded(event);
                    break;
                case "payment_intent.payment_failed":
                    handlePaymentIntentFailed(event);
                    break;
                case "payment_intent.canceled":
                    handlePaymentIntentCanceled(event);
                    break;
                case "payment_intent.requires_action":
                    handlePaymentIntentRequiresAction(event);
                    break;
                default:
                    log.info("Unhandled Stripe webhook event type: {}", event.getType());
            }

            return ResponseEntity.ok().build();

        } catch (SignatureVerificationException e) {
            log.error("Invalid Stripe webhook signature", e);
            return ResponseEntity.badRequest().body("Invalid signature");
        } catch (Exception e) {
            log.error("Error processing Stripe webhook", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Handle successful payment intent
     */
    private void handlePaymentIntentSucceeded(Event event) {
        try {
            PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
            if (paymentIntent != null) {
                log.info("Payment succeeded: {}", paymentIntent.getId());
                // TODO: Update payment status in database to COMPLETED
                // TODO: Send confirmation email
                // TODO: Update registration status
            }
        } catch (Exception e) {
            log.error("Error handling payment_intent.succeeded", e);
        }
    }

    /**
     * Handle failed payment intent
     */
    private void handlePaymentIntentFailed(Event event) {
        try {
            PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
            if (paymentIntent != null) {
                log.info("Payment failed: {}", paymentIntent.getId());
                // TODO: Update payment status in database to FAILED
                // TODO: Send failure notification
                // TODO: Log failure reason
            }
        } catch (Exception e) {
            log.error("Error handling payment_intent.payment_failed", e);
        }
    }

    /**
     * Handle canceled payment intent
     */
    private void handlePaymentIntentCanceled(Event event) {
        try {
            PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
            if (paymentIntent != null) {
                log.info("Payment canceled: {}", paymentIntent.getId());
                // TODO: Update payment status in database to CANCELLED
            }
        } catch (Exception e) {
            log.error("Error handling payment_intent.canceled", e);
        }
    }

    /**
     * Handle payment intent that requires action
     */
    private void handlePaymentIntentRequiresAction(Event event) {
        try {
            PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
            if (paymentIntent != null) {
                log.info("Payment requires action: {}", paymentIntent.getId());
                // TODO: Update payment status to PENDING
                // TODO: Notify customer of required action
            }
        } catch (Exception e) {
            log.error("Error handling payment_intent.requires_action", e);
        }
    }

    /**
     * MercadoPago webhook endpoint
     */
    @PostMapping("/mercadopago")
    public ResponseEntity<?> mercadoPagoWebhook(@RequestBody Map<String, Object> payload,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String id) {
        try {
            log.info("Received MercadoPago webhook - topic: {}, id: {}", topic, id);
            log.debug("MercadoPago webhook payload: {}", payload);

            // TODO: Verify webhook authenticity
            // TODO: Process webhook based on topic (payment, merchant_order, etc.)
            // TODO: Update payment status in database

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Error processing MercadoPago webhook", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * PayPal webhook endpoint
     */
    @PostMapping("/paypal")
    public ResponseEntity<?> paypalWebhook(@RequestBody Map<String, Object> payload,
            @RequestHeader("PAYPAL-TRANSMISSION-ID") String transmissionId,
            @RequestHeader("PAYPAL-CERT-ID") String certId,
            @RequestHeader("PAYPAL-TRANSMISSION-SIG") String signature,
            @RequestHeader("PAYPAL-TRANSMISSION-TIME") String transmissionTime) {
        try {
            log.info("Received PayPal webhook - transmission_id: {}, cert_id: {}", transmissionId, certId);
            log.debug("PayPal webhook payload: {}", payload);

            // TODO: Verify webhook signature using PayPal SDK
            // TODO: Process webhook event (PAYMENT.CAPTURE.COMPLETED,
            // PAYMENT.CAPTURE.DENIED, etc.)
            // TODO: Update payment status in database

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Error processing PayPal webhook", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Generic webhook endpoint (auto-detect provider)
     */
    @PostMapping
    public ResponseEntity<?> genericWebhook(@RequestBody String payload,
            @RequestHeader Map<String, String> headers) {
        try {
            log.info("Received generic webhook with headers: {}", headers);

            // Auto-detect provider based on headers
            if (headers.containsKey("stripe-signature")) {
                return stripeWebhook(payload, headers.get("stripe-signature"));
            } else if (headers.containsKey("x-mercadopago-signature")) {
                // MercadoPago sends different headers
                log.info("Detected MercadoPago webhook");
                // TODO: Handle MercadoPago webhook
            } else if (headers.containsKey("paypal-transmission-id")) {
                // PayPal webhook detected
                log.info("Detected PayPal webhook");
                // TODO: Handle PayPal webhook - requires additional headers for security
            }

            log.warn("Unknown webhook provider");
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Error processing generic webhook", e);
            return ResponseEntity.badRequest().build();
        }
    }
}