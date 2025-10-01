package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.jpa.Payment;
import com.mvt.mvt_events.jpa.Registration;
import com.mvt.mvt_events.payment.PaymentRequest;
import com.mvt.mvt_events.payment.PaymentResult;
import com.mvt.mvt_events.payment.PaymentService;
import com.mvt.mvt_events.repository.RegistrationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@Slf4j
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private com.mvt.mvt_events.service.RegistrationService registrationService;

    /**
     * Create a new payment
     */
    @PostMapping("/create")
    public ResponseEntity<?> createPayment(@RequestBody CreatePaymentRequest request) {
        try {
            log.info("Creating payment for registration: {} method: {}",
                    request.getRegistrationId(), request.getPaymentMethod());

            // Validate registration exists and get with user and event loaded
            Registration registration = registrationService.get(request.getRegistrationId());

            // Build payment request
            PaymentRequest paymentRequest = PaymentRequest.builder()
                    .registrationId(request.getRegistrationId().toString())
                    .amount(request.getAmount())
                    .currency("BRL")
                    .paymentMethod(request.getPaymentMethod())
                    .customerEmail(registration.getUser().getUsername())
                    .customerName(registration.getUser().getName())
                    .customerDocumentNumber(registration.getUser().getDocumentNumber())
                    .customerPhone(registration.getUser().getPhone())
                    .description("Inscrição para evento: " + registration.getEvent().getName())
                    .returnUrl(request.getReturnUrl())
                    .cancelUrl(request.getCancelUrl())
                    .webhookUrl(request.getWebhookUrl())
                    .pixExpirationMinutes(request.getPixExpirationMinutes())
                    .cardToken(request.getCardToken())
                    .installments(request.getInstallments())
                    .build();

            PaymentResult result = paymentService.createPayment(paymentRequest);

            if (result.isSuccess()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            log.error("Error creating payment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create payment", "message", e.getMessage()));
        }
    }

    /**
     * Confirm a payment
     */
    @PostMapping("/{paymentId}/confirm")
    public ResponseEntity<?> confirmPayment(@PathVariable String paymentId,
            @RequestBody(required = false) Map<String, Object> params) {
        try {
            if (params == null) {
                params = new HashMap<>();
            }

            PaymentResult result = paymentService.confirmPayment(paymentId, params);

            if (result.isSuccess()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            log.error("Error confirming payment: {}", paymentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to confirm payment", "message", e.getMessage()));
        }
    }

    /**
     * Get payment status
     */
    @GetMapping("/{paymentId}/status")
    public ResponseEntity<?> getPaymentStatus(@PathVariable String paymentId) {
        try {
            PaymentResult result = paymentService.getPaymentStatus(paymentId);

            if (result.isSuccess()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            log.error("Error getting payment status: {}", paymentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get payment status", "message", e.getMessage()));
        }
    }

    /**
     * Refund a payment
     */
    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<?> refundPayment(@PathVariable String paymentId,
            @RequestBody RefundRequest request) {
        try {
            PaymentResult result = paymentService.refundPayment(
                    paymentId,
                    request.getAmount(),
                    request.getReason());

            if (result.isSuccess()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            log.error("Error refunding payment: {}", paymentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to refund payment", "message", e.getMessage()));
        }
    }

    /**
     * Calculate payment fee
     */
    @GetMapping("/calculate-fee")
    public ResponseEntity<?> calculateFee(@RequestParam BigDecimal amount,
            @RequestParam Payment.PaymentMethod paymentMethod) {
        try {
            BigDecimal fee = paymentService.calculateFee(amount, paymentMethod);

            Map<String, Object> response = new HashMap<>();
            response.put("amount", amount);
            response.put("paymentMethod", paymentMethod);
            response.put("fee", fee);
            response.put("totalAmount", amount.add(fee));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error calculating fee", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to calculate fee", "message", e.getMessage()));
        }
    }

    /**
     * Get supported payment methods
     */
    @GetMapping("/methods")
    public ResponseEntity<?> getSupportedPaymentMethods() {
        try {
            Map<String, Object> response = new HashMap<>();

            for (Payment.PaymentMethod method : Payment.PaymentMethod.values()) {
                if (paymentService.isPaymentMethodSupported(method)) {
                    response.put(method.name(), Map.of(
                            "name", method.getDisplayName(),
                            "supported", true));
                }
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting supported payment methods", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get payment methods", "message", e.getMessage()));
        }
    }

    // Request DTOs
    public static class CreatePaymentRequest {
        private Long registrationId;
        private BigDecimal amount;
        private Payment.PaymentMethod paymentMethod;
        private String returnUrl;
        private String cancelUrl;
        private String webhookUrl;
        private Integer pixExpirationMinutes;
        private String cardToken;
        private Integer installments;

        // Getters and setters
        public Long getRegistrationId() {
            return registrationId;
        }

        public void setRegistrationId(Long registrationId) {
            this.registrationId = registrationId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public Payment.PaymentMethod getPaymentMethod() {
            return paymentMethod;
        }

        public void setPaymentMethod(Payment.PaymentMethod paymentMethod) {
            this.paymentMethod = paymentMethod;
        }

        public String getReturnUrl() {
            return returnUrl;
        }

        public void setReturnUrl(String returnUrl) {
            this.returnUrl = returnUrl;
        }

        public String getCancelUrl() {
            return cancelUrl;
        }

        public void setCancelUrl(String cancelUrl) {
            this.cancelUrl = cancelUrl;
        }

        public String getWebhookUrl() {
            return webhookUrl;
        }

        public void setWebhookUrl(String webhookUrl) {
            this.webhookUrl = webhookUrl;
        }

        public Integer getPixExpirationMinutes() {
            return pixExpirationMinutes;
        }

        public void setPixExpirationMinutes(Integer pixExpirationMinutes) {
            this.pixExpirationMinutes = pixExpirationMinutes;
        }

        public String getCardToken() {
            return cardToken;
        }

        public void setCardToken(String cardToken) {
            this.cardToken = cardToken;
        }

        public Integer getInstallments() {
            return installments;
        }

        public void setInstallments(Integer installments) {
            this.installments = installments;
        }
    }

    public static class RefundRequest {
        private BigDecimal amount;
        private String reason;

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    /**
     * Stripe Webhook endpoint
     */
    @PostMapping("/webhooks/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) {

        log.info("Received Stripe webhook");

        try {
            // TODO: Verificar assinatura do webhook para segurança em produção
            // Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);

            // Parse do payload JSON
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode eventJson = objectMapper.readTree(payload);

            String eventType = eventJson.get("type").asText();
            log.info("Processing webhook event type: {}", eventType);

            // Processar eventos de sucesso do pagamento
            if ("payment_intent.succeeded".equals(eventType)) {
                processPaymentSuccess(eventJson);
            } else if ("payment_intent.payment_failed".equals(eventType)) {
                processPaymentFailure(eventJson);
            } else {
                log.info("Ignoring webhook event type: {}", eventType);
            }

            return ResponseEntity.ok("Success");

        } catch (Exception e) {
            log.error("Error processing Stripe webhook", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Webhook error");
        }
    }

    private void processPaymentSuccess(com.fasterxml.jackson.databind.JsonNode eventJson) {
        try {
            com.fasterxml.jackson.databind.JsonNode paymentIntent = eventJson.get("data").get("object");
            com.fasterxml.jackson.databind.JsonNode metadata = paymentIntent.get("metadata");

            if (metadata != null && metadata.has("registration_id")) {
                String registrationId = metadata.get("registration_id").asText();
                String paymentIntentId = paymentIntent.get("id").asText();
                long amountInCents = paymentIntent.get("amount").asLong();
                String currency = paymentIntent.get("currency").asText().toUpperCase();

                log.info("Processing payment success for registration: {} payment: {}", registrationId,
                        paymentIntentId);

                // Processar pagamento em transação
                paymentService.processPaymentSuccess(Long.parseLong(registrationId), paymentIntentId,
                        amountInCents, currency);

                log.info("Payment completed for registration: {} with amount: {} {}",
                        registrationId, new java.math.BigDecimal(amountInCents).divide(new java.math.BigDecimal(100)),
                        currency);
            } else {
                log.warn("No registration_id found in payment_intent metadata");
            }
        } catch (Exception e) {
            log.error("Error processing payment success", e);
        }
    }

    private void processPaymentFailure(com.fasterxml.jackson.databind.JsonNode eventJson) {
        try {
            com.fasterxml.jackson.databind.JsonNode paymentIntent = eventJson.get("data").get("object");
            com.fasterxml.jackson.databind.JsonNode metadata = paymentIntent.get("metadata");

            if (metadata != null && metadata.has("registration_id")) {
                String registrationId = metadata.get("registration_id").asText();
                String paymentIntentId = paymentIntent.get("id").asText();
                long amountInCents = paymentIntent.get("amount").asLong();
                String currency = paymentIntent.get("currency").asText().toUpperCase();

                log.info("Processing payment failure for registration: {} payment: {}", registrationId,
                        paymentIntentId);

                // Processar falha do pagamento em transação
                paymentService.processPaymentFailure(Long.parseLong(registrationId), paymentIntentId,
                        amountInCents, currency);

                log.info("Payment failed for registration: {}", registrationId);
            } else {
                log.warn("No registration_id found in payment_intent metadata");
            }
        } catch (Exception e) {
            log.error("Error processing payment failure", e);
        }
    }
}