package com.mvt.mvt_events.payment;

import com.mvt.mvt_events.jpa.Payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    private String registrationId;
    private BigDecimal amount;
    private String currency;
    private Payment.PaymentMethod paymentMethod;
    private String customerEmail;
    private String customerName;
    private String customerDocumentNumber;
    private String customerPhone;
    private String description;
    private String returnUrl;
    private String cancelUrl;
    private String webhookUrl;
    private Map<String, Object> metadata;

    // PIX specific
    private Integer pixExpirationMinutes;

    // Card specific
    private String cardToken;
    private Integer installments;
}