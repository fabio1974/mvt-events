package com.mvt.mvt_events.payment.providers;

import com.mvt.mvt_events.jpa.Payment;
import com.mvt.mvt_events.payment.PaymentProvider;
import com.mvt.mvt_events.payment.PaymentRequest;
import com.mvt.mvt_events.payment.PaymentResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class MercadoPagoPaymentProvider implements PaymentProvider {

    @Value("${payment.mercadopago.access-token:}")
    private String accessToken;

    @Value("${payment.mercadopago.public-key:}")
    private String publicKey;

    // MercadoPago fees (Brazilian market)
    private static final BigDecimal CREDIT_CARD_FEE_RATE = new BigDecimal("0.0499"); // 4.99%
    private static final BigDecimal CREDIT_CARD_FIXED_FEE = new BigDecimal("0.39"); // R$ 0.39
    private static final BigDecimal PIX_FEE_RATE = new BigDecimal("0.0099"); // 0.99%
    private static final BigDecimal DEBIT_CARD_FEE_RATE = new BigDecimal("0.0349"); // 3.49%

    @Override
    public PaymentResult createPayment(PaymentRequest request) {
        log.info("Creating MercadoPago payment for amount: {} {}", request.getAmount(), request.getCurrency());

        try {
            // TODO: Integrate with actual MercadoPago SDK
            // For now, return a mock response

            String preferenceId = UUID.randomUUID().toString();

            PaymentResult.PaymentResultBuilder result = PaymentResult.builder()
                    .success(true)
                    .paymentId(preferenceId)
                    .providerPaymentId(preferenceId)
                    .status(Payment.PaymentStatus.PENDING)
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .fee(calculateFee(request.getAmount(), request.getPaymentMethod()));

            if (request.getPaymentMethod() == Payment.PaymentMethod.PIX) {
                // PIX payment
                String pixCode = generateMockPixCode();
                result.qrCode(pixCode)
                        .pixCopyPaste(pixCode)
                        .qrCodeBase64(generateMockQrCodeBase64())
                        .expiresAt(LocalDateTime.now().plusMinutes(
                                request.getPixExpirationMinutes() != null ? request.getPixExpirationMinutes() : 30));
            } else {
                // Credit/Debit card or other methods
                result.paymentUrl("https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=" + preferenceId);
            }

            Map<String, Object> providerResponse = new HashMap<>();
            providerResponse.put("preference_id", preferenceId);
            providerResponse.put("init_point",
                    "https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=" + preferenceId);
            providerResponse.put("sandbox_init_point",
                    "https://sandbox.mercadopago.com.br/checkout/v1/redirect?pref_id=" + preferenceId);
            result.providerResponse(providerResponse);

            return result.build();

        } catch (Exception e) {
            log.error("Error creating MercadoPago payment", e);
            return PaymentResult.error("Failed to create payment: " + e.getMessage(), "MERCADOPAGO_ERROR");
        }
    }

    @Override
    public PaymentResult confirmPayment(String paymentId, Map<String, Object> params) {
        log.info("Confirming MercadoPago payment: {}", paymentId);

        try {
            // TODO: Integrate with actual MercadoPago SDK to get payment details

            return PaymentResult.success(paymentId, paymentId, Payment.PaymentStatus.COMPLETED);

        } catch (Exception e) {
            log.error("Error confirming MercadoPago payment", e);
            return PaymentResult.error("Failed to confirm payment: " + e.getMessage(), "MERCADOPAGO_CONFIRM_ERROR");
        }
    }

    @Override
    public PaymentResult refundPayment(String paymentId, BigDecimal amount, String reason) {
        log.info("Refunding MercadoPago payment: {} amount: {}", paymentId, amount);

        try {
            // TODO: Integrate with actual MercadoPago SDK to create refund

            return PaymentResult.builder()
                    .success(true)
                    .paymentId(paymentId)
                    .providerPaymentId(String.valueOf(System.currentTimeMillis()))
                    .status(Payment.PaymentStatus.REFUNDED)
                    .amount(amount)
                    .build();

        } catch (Exception e) {
            log.error("Error refunding MercadoPago payment", e);
            return PaymentResult.error("Failed to refund payment: " + e.getMessage(), "MERCADOPAGO_REFUND_ERROR");
        }
    }

    @Override
    public PaymentResult getPaymentStatus(String paymentId) {
        log.info("Getting MercadoPago payment status: {}", paymentId);

        try {
            // TODO: Integrate with actual MercadoPago SDK to get payment status

            return PaymentResult.success(paymentId, paymentId, Payment.PaymentStatus.COMPLETED);

        } catch (Exception e) {
            log.error("Error getting MercadoPago payment status", e);
            return PaymentResult.error("Failed to get payment status: " + e.getMessage(), "MERCADOPAGO_STATUS_ERROR");
        }
    }

    @Override
    public BigDecimal calculateFee(BigDecimal amount, Payment.PaymentMethod paymentMethod) {
        return switch (paymentMethod) {
            case CREDIT_CARD ->
                amount.multiply(CREDIT_CARD_FEE_RATE).add(CREDIT_CARD_FIXED_FEE).setScale(2, RoundingMode.HALF_UP);
            case DEBIT_CARD ->
                amount.multiply(DEBIT_CARD_FEE_RATE).add(CREDIT_CARD_FIXED_FEE).setScale(2, RoundingMode.HALF_UP);
            case PIX ->
                amount.multiply(PIX_FEE_RATE).setScale(2, RoundingMode.HALF_UP);
            default -> BigDecimal.ZERO;
        };
    }

    @Override
    public String getProviderName() {
        return "MERCADOPAGO";
    }

    @Override
    public boolean supportsPaymentMethod(Payment.PaymentMethod paymentMethod) {
        return switch (paymentMethod) {
            case CREDIT_CARD, DEBIT_CARD, PIX -> true;
            case BANK_TRANSFER -> true; // MercadoPago supports bank transfers
            default -> false;
        };
    }

    private String generateMockPixCode() {
        // Mock PIX code - in real implementation, this would come from MercadoPago
        return "00020126580014br.gov.bcb.pix0136" + UUID.randomUUID().toString().replace("-", "")
                + "5204000053039865802BR5925CORRIDAS DA SERRA EVENTOS6009Sao Paulo62070503***6304";
    }

    private String generateMockQrCodeBase64() {
        // Mock base64 QR code - in real implementation, this would be generated from
        // PIX code
        return "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==";
    }
}