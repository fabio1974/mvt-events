package com.mvt.mvt_events.payment;

import com.mvt.mvt_events.jpa.Payment;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Interface for payment providers (Stripe, Mercado Pago, etc.)
 */
public interface PaymentProvider {

    /**
     * Create a payment intent/preference
     */
    PaymentResult createPayment(PaymentRequest request);

    /**
     * Confirm/capture a payment
     */
    PaymentResult confirmPayment(String paymentId, Map<String, Object> params);

    /**
     * Refund a payment
     */
    PaymentResult refundPayment(String paymentId, BigDecimal amount, String reason);

    /**
     * Get payment status
     */
    PaymentResult getPaymentStatus(String paymentId);

    /**
     * Calculate fees for the payment method
     */
    BigDecimal calculateFee(BigDecimal amount, Payment.PaymentMethod paymentMethod);

    /**
     * Get provider name
     */
    String getProviderName();

    /**
     * Check if payment method is supported
     */
    boolean supportsPaymentMethod(Payment.PaymentMethod paymentMethod);
}