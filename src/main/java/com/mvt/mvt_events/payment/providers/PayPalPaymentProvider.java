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
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

/**
 * PayPal payment provider implementation
 * Supports PayPal accounts, credit cards, and bank transfers
 */
@Component
@Slf4j
public class PayPalPaymentProvider implements PaymentProvider {

    @Value("${payment.paypal.client-id:}")
    private String clientId;

    @Value("${payment.paypal.client-secret:}")
    private String clientSecret;

    @Value("${payment.paypal.environment:sandbox}")
    private String environment;

    private static final BigDecimal PAYPAL_ACCOUNT_FEE_PERCENTAGE = new BigDecimal("3.49"); // 3.49%
    private static final BigDecimal PAYPAL_ACCOUNT_FIXED_FEE = new BigDecimal("0.60"); // R$ 0,60

    private static final BigDecimal CREDIT_CARD_FEE_PERCENTAGE = new BigDecimal("4.99"); // 4.99%
    private static final BigDecimal CREDIT_CARD_FIXED_FEE = new BigDecimal("0.60"); // R$ 0,60

    private static final BigDecimal BANK_TRANSFER_FEE_PERCENTAGE = new BigDecimal("1.99"); // 1.99%
    private static final BigDecimal BANK_TRANSFER_FIXED_FEE = new BigDecimal("0.00"); // Sem taxa fixa

    @Override
    public PaymentResult createPayment(PaymentRequest request) {
        log.info("Creating PayPal payment for amount: {} using method: {}",
                request.getAmount(), request.getPaymentMethod());

        try {
            // Validate configuration
            if (clientId.isEmpty() || clientSecret.isEmpty()) {
                return PaymentResult.error("PayPal not configured", "CONFIGURATION_ERROR");
            }

            // Calculate fees
            BigDecimal fee = calculateFee(request.getAmount(), request.getPaymentMethod());

            // Mock PayPal payment creation
            String paymentId = "PAY-" + UUID.randomUUID().toString().replace("-", "").substring(0, 17).toUpperCase();
            String approvalUrl = generateMockApprovalUrl(paymentId, request);

            // Create response based on payment method
            PaymentResult.PaymentResultBuilder resultBuilder = PaymentResult.builder()
                    .success(true)
                    .paymentId(paymentId)
                    .status(Payment.PaymentStatus.PENDING)
                    .amount(request.getAmount())
                    .fee(fee)
                    .currency(request.getCurrency())
                    .paymentUrl(approvalUrl)
                    .expiresAt(LocalDateTime.now().plusHours(3));

            // Add payment method specific metadata
            Map<String, Object> metadata = Map.of(
                    "provider", "PAYPAL",
                    "payment_method", request.getPaymentMethod().name(),
                    "approval_url", approvalUrl,
                    "environment", environment
            );

            switch (request.getPaymentMethod()) {
                case PAYPAL_ACCOUNT:
                    metadata = Map.of(
                            "provider", "PAYPAL",
                            "payment_method", "PAYPAL_ACCOUNT",
                            "approval_url", approvalUrl,
                            "redirect_url", approvalUrl,
                            "expires_at", LocalDateTime.now().plusHours(3).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    );
                    break;

                case CREDIT_CARD:
                    metadata = Map.of(
                            "provider", "PAYPAL",
                            "payment_method", "CREDIT_CARD",
                            "approval_url", approvalUrl,
                            "card_funding_type", "CREDIT",
                            "expires_at", LocalDateTime.now().plusHours(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    );
                    break;

                case BANK_TRANSFER:
                    metadata = Map.of(
                            "provider", "PAYPAL",
                            "payment_method", "BANK_TRANSFER",
                            "approval_url", approvalUrl,
                            "bank_transfer_type", "ACH",
                            "processing_time", "1-3 business days",
                            "expires_at", LocalDateTime.now().plusDays(7).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    );
                    resultBuilder.expiresAt(LocalDateTime.now().plusDays(7));
                    break;

                default:
                    return PaymentResult.error("Unsupported payment method: " + request.getPaymentMethod(), "UNSUPPORTED_METHOD");
            }

            resultBuilder.metadata(metadata);

            PaymentResult result = resultBuilder.build();
            log.info("PayPal payment created successfully: {}", paymentId);
            return result;

        } catch (Exception e) {
            log.error("Error creating PayPal payment", e);
            return PaymentResult.error("Failed to create PayPal payment: " + e.getMessage(), "PAYMENT_CREATION_ERROR");
        }
    }

    @Override
    public PaymentResult confirmPayment(String paymentId, Map<String, Object> params) {
        log.info("Confirming PayPal payment: {}", paymentId);

        try {
            // Mock PayPal payment confirmation
            // In real implementation, this would call PayPal's capture/execute API

            String payerId = (String) params.get("PayerID");
            String token = (String) params.get("token");

            if (payerId == null || token == null) {
                return PaymentResult.error("Missing PayerID or token", "INVALID_PARAMETERS");
            }

            return PaymentResult.builder()
                    .success(true)
                    .paymentId(paymentId)
                    .status(Payment.PaymentStatus.COMPLETED)
                    .metadata(Map.of(
                            "provider", "PAYPAL",
                            "payer_id", payerId,
                            "token", token,
                            "transaction_id", "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                            "fee_paid_by", "merchant"
                    ))
                    .build();

        } catch (Exception e) {
            log.error("Error confirming PayPal payment: {}", paymentId, e);
            return PaymentResult.error("Failed to confirm PayPal payment: " + e.getMessage(), "PAYMENT_CONFIRMATION_ERROR");
        }
    }

    @Override
    public PaymentResult refundPayment(String paymentId, BigDecimal amount, String reason) {
        log.info("Processing PayPal refund for payment: {} amount: {}", paymentId, amount);

        try {
            // Mock PayPal refund
            String refundId = "RF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            return PaymentResult.builder()
                    .success(true)
                    .paymentId(refundId)
                    .status(Payment.PaymentStatus.REFUNDED)
                    .amount(amount)
                    .metadata(Map.of(
                            "provider", "PAYPAL",
                            "original_payment_id", paymentId,
                            "refund_reason", reason,
                            "refund_type", "FULL",
                            "processing_time", "5-7 business days"
                    ))
                    .build();

        } catch (Exception e) {
            log.error("Error processing PayPal refund: {}", paymentId, e);
            return PaymentResult.error("Failed to refund PayPal payment: " + e.getMessage(), "REFUND_ERROR");
        }
    }

    @Override
    public PaymentResult getPaymentStatus(String paymentId) {
        log.info("Getting PayPal payment status: {}", paymentId);

        try {
            // Mock status check
            // In real implementation, this would call PayPal's payment details API

            String status = mockPaymentStatus(paymentId);

            // Convert string status to enum
            Payment.PaymentStatus paymentStatus = convertStatusToEnum(status);

            return PaymentResult.builder()
                    .success(true)
                    .paymentId(paymentId)
                    .status(paymentStatus)
                    .metadata(Map.of(
                            "provider", "PAYPAL",
                            "last_updated", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                            "payment_method", "paypal"
                    ))
                    .build();

        } catch (Exception e) {
            log.error("Error getting PayPal payment status: {}", paymentId, e);
            return PaymentResult.error("Failed to get PayPal payment status: " + e.getMessage(), "STATUS_CHECK_ERROR");
        }
    }

    @Override
    public BigDecimal calculateFee(BigDecimal amount, Payment.PaymentMethod paymentMethod) {
        BigDecimal percentage;
        BigDecimal fixedFee;

        switch (paymentMethod) {
            case PAYPAL_ACCOUNT:
                percentage = PAYPAL_ACCOUNT_FEE_PERCENTAGE;
                fixedFee = PAYPAL_ACCOUNT_FIXED_FEE;
                break;
            case CREDIT_CARD:
                percentage = CREDIT_CARD_FEE_PERCENTAGE;
                fixedFee = CREDIT_CARD_FIXED_FEE;
                break;
            case BANK_TRANSFER:
                percentage = BANK_TRANSFER_FEE_PERCENTAGE;
                fixedFee = BANK_TRANSFER_FIXED_FEE;
                break;
            default:
                return BigDecimal.ZERO;
        }

        BigDecimal percentageFee = amount.multiply(percentage).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        return percentageFee.add(fixedFee);
    }

    @Override
    public String getProviderName() {
        return "PAYPAL";
    }

    @Override
    public boolean supportsPaymentMethod(Payment.PaymentMethod paymentMethod) {
        return switch (paymentMethod) {
            case PAYPAL_ACCOUNT, CREDIT_CARD, BANK_TRANSFER -> true;
            default -> false;
        };
    }

    /**
     * Generate mock PayPal approval URL
     */
    private String generateMockApprovalUrl(String paymentId, PaymentRequest request) {
        String baseUrl = environment.equals("sandbox") 
            ? "https://www.sandbox.paypal.com" 
            : "https://www.paypal.com";
        
        return String.format("%s/webapps/hermes?cmd=_express-checkout&useraction=commit&token=%s",
                baseUrl, paymentId);
    }

    /**
     * Mock payment status for demonstration
     */
    private String mockPaymentStatus(String paymentId) {
        // Simple mock logic based on payment ID
        int hash = paymentId.hashCode();
        return switch (Math.abs(hash) % 4) {
            case 0 -> "CREATED";
            case 1 -> "APPROVED";
            case 2 -> "COMPLETED";
            default -> "PENDING";
        };
    }

    /**
     * Convert PayPal string status to Payment.PaymentStatus enum
     */
    private Payment.PaymentStatus convertStatusToEnum(String status) {
        return switch (status.toUpperCase()) {
            case "CREATED", "PENDING", "APPROVED" -> Payment.PaymentStatus.PENDING;
            case "COMPLETED" -> Payment.PaymentStatus.COMPLETED;
            case "FAILED", "DENIED", "EXPIRED" -> Payment.PaymentStatus.FAILED;
            case "CANCELLED" -> Payment.PaymentStatus.CANCELLED;
            default -> Payment.PaymentStatus.PENDING;
        };
    }
}