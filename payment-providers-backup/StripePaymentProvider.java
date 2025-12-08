package com.mvt.mvt_events.payment.providers;

import com.mvt.mvt_events.payment.PaymentProvider;
import com.mvt.mvt_events.payment.PaymentResult;
import com.mvt.mvt_events.payment.PaymentRequest;
import com.mvt.mvt_events.jpa.Payment;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentIntentConfirmParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class StripePaymentProvider implements PaymentProvider {

    @Value("${payment.stripe.secret-key:}")
    private String stripeSecretKey;

    @Value("${payment.stripe.public-key:}")
    private String stripePublicKey;

    // Stripe fees: 2.9% + $0.30 for credit cards, 2.4% + $0.30 for debit cards
    private static final BigDecimal CREDIT_CARD_PERCENTAGE = new BigDecimal("0.029");
    private static final BigDecimal DEBIT_CARD_PERCENTAGE = new BigDecimal("0.024");
    private static final BigDecimal CREDIT_CARD_FIXED_FEE = new BigDecimal("0.30");
    private static final BigDecimal DEBIT_CARD_FIXED_FEE = new BigDecimal("0.30");

    @PostConstruct
    public void init() {
        if (stripeSecretKey != null && !stripeSecretKey.isEmpty()) {
            Stripe.apiKey = stripeSecretKey;
            log.info("Stripe configured with secret key: {}...{}",
                    stripeSecretKey.substring(0, 7),
                    stripeSecretKey.substring(stripeSecretKey.length() - 4));
        } else {
            log.warn("Stripe secret key not configured");
        }
    }

    @Override
    public PaymentResult createPayment(PaymentRequest request) {
        log.info("Creating Stripe payment: amount={}, currency={}, method={}",
                request.getAmount(), request.getCurrency(), request.getPaymentMethod());

        try {
            // Convert amount to cents (Stripe uses smallest currency unit)
            long amountInCents = request.getAmount().multiply(new BigDecimal("100")).longValue();

            // Build payment intent parameters
            PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency(request.getCurrency().toLowerCase())
                    .setDescription(request.getDescription())
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build());

            // Add customer information if available
            if (request.getCustomerEmail() != null) {
                paramsBuilder.setReceiptEmail(request.getCustomerEmail());
            }

            // Add metadata
            paramsBuilder.putMetadata("customer_name", request.getCustomerName())
                    .putMetadata("payment_method_type", request.getPaymentMethod().name());

            // Create payment intent
            PaymentIntent intent = PaymentIntent.create(paramsBuilder.build());

            // Build response
            Map<String, Object> providerResponse = new HashMap<>();
            providerResponse.put("payment_intent_id", intent.getId());
            providerResponse.put("client_secret", intent.getClientSecret());
            providerResponse.put("status", intent.getStatus());

            return PaymentResult.builder()
                    .success(true)
                    .paymentId(intent.getId())
                    .providerPaymentId(intent.getId())
                    .status(convertStripeStatus(intent.getStatus()))
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .providerResponse(providerResponse)
                    .fee(calculateFee(request.getAmount(), request.getPaymentMethod()))
                    .build();

        } catch (StripeException e) {
            log.error("Stripe payment creation failed", e);
            return PaymentResult.builder()
                    .success(false)
                    .errorMessage("Stripe payment failed: " + e.getMessage())
                    .errorCode(e.getCode())
                    .status(Payment.PaymentStatus.FAILED)
                    .build();
        } catch (Exception e) {
            log.error("Unexpected error creating Stripe payment", e);
            return PaymentResult.builder()
                    .success(false)
                    .errorMessage("Unexpected error: " + e.getMessage())
                    .errorCode("UNEXPECTED_ERROR")
                    .status(Payment.PaymentStatus.FAILED)
                    .build();
        }
    }

    @Override
    public PaymentResult confirmPayment(String paymentId, Map<String, Object> params) {
        log.info("Confirming Stripe payment: {}", paymentId);

        try {
            PaymentIntent intent = PaymentIntent.retrieve(paymentId);

            if ("requires_confirmation".equals(intent.getStatus())) {
                PaymentIntentConfirmParams confirmParams = PaymentIntentConfirmParams.builder()
                        .setReturnUrl(params.get("return_url") != null ? params.get("return_url").toString() : null)
                        .build();

                intent = intent.confirm(confirmParams);
            }

            Map<String, Object> providerResponse = new HashMap<>();
            providerResponse.put("payment_intent_id", intent.getId());
            providerResponse.put("status", intent.getStatus());

            return PaymentResult.builder()
                    .success(true)
                    .paymentId(intent.getId())
                    .providerPaymentId(intent.getId())
                    .status(convertStripeStatus(intent.getStatus()))
                    .providerResponse(providerResponse)
                    .build();

        } catch (StripeException e) {
            log.error("Stripe payment confirmation failed", e);
            return PaymentResult.builder()
                    .success(false)
                    .errorMessage("Stripe confirmation failed: " + e.getMessage())
                    .errorCode(e.getCode())
                    .status(Payment.PaymentStatus.FAILED)
                    .build();
        }
    }

    @Override
    public PaymentResult refundPayment(String paymentId, BigDecimal amount, String reason) {
        log.info("Processing Stripe refund: paymentId={}, amount={}, reason={}", paymentId, amount, reason);

        try {
            // Create refund using Stripe API
            com.stripe.param.RefundCreateParams refundParams = com.stripe.param.RefundCreateParams.builder()
                    .setPaymentIntent(paymentId)
                    .setAmount(amount.multiply(new BigDecimal("100")).longValue())
                    .setReason(com.stripe.param.RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER)
                    .build();

            com.stripe.model.Refund refund = com.stripe.model.Refund.create(refundParams);

            return PaymentResult.builder()
                    .success(true)
                    .paymentId(refund.getId())
                    .providerPaymentId(refund.getPaymentIntent())
                    .status(Payment.PaymentStatus.REFUNDED)
                    .build();

        } catch (StripeException e) {
            log.error("Stripe refund failed", e);
            return PaymentResult.builder()
                    .success(false)
                    .errorMessage("Stripe refund failed: " + e.getMessage())
                    .errorCode(e.getCode())
                    .status(Payment.PaymentStatus.FAILED)
                    .build();
        }
    }

    @Override
    public PaymentResult getPaymentStatus(String paymentId) {
        log.info("Getting Stripe payment status: {}", paymentId);

        try {
            PaymentIntent intent = PaymentIntent.retrieve(paymentId);

            Map<String, Object> providerResponse = new HashMap<>();
            providerResponse.put("payment_intent_id", intent.getId());
            providerResponse.put("status", intent.getStatus());
            providerResponse.put("amount", intent.getAmount());
            providerResponse.put("currency", intent.getCurrency());

            return PaymentResult.builder()
                    .success(true)
                    .paymentId(intent.getId())
                    .providerPaymentId(intent.getId())
                    .status(convertStripeStatus(intent.getStatus()))
                    .amount(new BigDecimal(intent.getAmount()).divide(new BigDecimal("100")))
                    .currency(intent.getCurrency().toUpperCase())
                    .providerResponse(providerResponse)
                    .build();

        } catch (StripeException e) {
            log.error("Error getting Stripe payment status", e);
            return PaymentResult.builder()
                    .success(false)
                    .errorMessage("Error getting payment status: " + e.getMessage())
                    .errorCode(e.getCode())
                    .status(Payment.PaymentStatus.FAILED)
                    .build();
        }
    }

    @Override
    public BigDecimal calculateFee(BigDecimal amount, String paymentMethod) {
        BigDecimal percentageFee = amount.multiply(
                "CREDIT_CARD".equals(paymentMethod) ? CREDIT_CARD_PERCENTAGE : DEBIT_CARD_PERCENTAGE);
        return switch (paymentMethod) {
            case "CREDIT_CARD" -> percentageFee.add(CREDIT_CARD_FIXED_FEE);
            case "DEBIT_CARD" -> percentageFee.add(DEBIT_CARD_FIXED_FEE);
            default -> BigDecimal.ZERO;
        };
    }

    @Override
    public String getProviderName() {
        return "stripe";
    }

    @Override
    public boolean supportsPaymentMethod(String paymentMethod) {
        return switch (paymentMethod) {
            case "CREDIT_CARD", "DEBIT_CARD" -> true;
            case "PIX" -> false; // Stripe doesn't support PIX directly in Brazil
            default -> false;
        };
    }
}
