package com.mvt.mvt_events.payment;

import com.mvt.mvt_events.jpa.Payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResult {

    private boolean success;
    private String paymentId;
    private String providerPaymentId;
    private Payment.PaymentStatus status;
    private BigDecimal amount;
    private BigDecimal fee;
    private String currency;
    private String paymentUrl; // For PIX QR code or redirect URLs
    private String qrCode; // PIX QR code data
    private String qrCodeBase64; // PIX QR code as base64 image
    private String pixCopyPaste; // PIX copy-paste code
    private LocalDateTime expiresAt;
    private String errorMessage;
    private String errorCode;
    private Map<String, Object> providerResponse;
    private Map<String, Object> metadata;

    // Factory methods for common scenarios
    public static PaymentResult success(String paymentId, String providerPaymentId, Payment.PaymentStatus status) {
        return PaymentResult.builder()
                .success(true)
                .paymentId(paymentId)
                .providerPaymentId(providerPaymentId)
                .status(status)
                .build();
    }

    public static PaymentResult error(String errorMessage, String errorCode) {
        return PaymentResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .errorCode(errorCode)
                .status(Payment.PaymentStatus.FAILED)
                .build();
    }
}