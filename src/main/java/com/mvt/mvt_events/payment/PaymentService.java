package com.mvt.mvt_events.payment;

import com.mvt.mvt_events.jpa.Payment;
import com.mvt.mvt_events.jpa.Registration;
import com.mvt.mvt_events.repository.PaymentRepository;
import com.mvt.mvt_events.service.RegistrationService;
import com.mvt.mvt_events.payment.providers.MercadoPagoPaymentProvider;
import com.mvt.mvt_events.payment.providers.PayPalPaymentProvider;
import com.mvt.mvt_events.payment.providers.StripePaymentProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
@Slf4j
public class PaymentService {

    @Value("${payment.provider:MERCADOPAGO}")
    private String defaultProvider;

    @Autowired
    private StripePaymentProvider stripeProvider;

    @Autowired
    private MercadoPagoPaymentProvider mercadoPagoProvider;

    @Autowired
    private PayPalPaymentProvider paypalProvider;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RegistrationService registrationService;

    /**
     * Create a payment using the configured provider
     */
    public PaymentResult createPayment(PaymentRequest request) {
        PaymentProvider provider = getProvider(request.getPaymentMethod());

        if (!provider.supportsPaymentMethod(request.getPaymentMethod())) {
            log.warn("Payment method {} not supported by provider {}",
                    request.getPaymentMethod(), provider.getProviderName());
            return PaymentResult.error(
                    "Payment method not supported by " + provider.getProviderName(),
                    "UNSUPPORTED_PAYMENT_METHOD");
        }

        log.info("Creating payment using provider: {} for method: {}",
                provider.getProviderName(), request.getPaymentMethod());

        return provider.createPayment(request);
    }

    /**
     * Confirm a payment
     */
    public PaymentResult confirmPayment(String paymentId, Map<String, Object> params) {
        PaymentProvider provider = getProviderForPayment(paymentId);
        return provider.confirmPayment(paymentId, params);
    }

    /**
     * Refund a payment
     */
    public PaymentResult refundPayment(String paymentId, BigDecimal amount, String reason) {
        PaymentProvider provider = getProviderForPayment(paymentId);
        return provider.refundPayment(paymentId, amount, reason);
    }

    /**
     * Get payment status
     */
    public PaymentResult getPaymentStatus(String paymentId) {
        PaymentProvider provider = getProviderForPayment(paymentId);
        return provider.getPaymentStatus(paymentId);
    }

    /**
     * Calculate fee for a payment
     */
    public BigDecimal calculateFee(BigDecimal amount, Payment.PaymentMethod paymentMethod) {
        PaymentProvider provider = getProvider(paymentMethod);
        return provider.calculateFee(amount, paymentMethod);
    }

    /**
     * Get the appropriate provider based on payment method and configuration
     */
    private PaymentProvider getProvider(Payment.PaymentMethod paymentMethod) {
        // For PIX, always use MercadoPago as Stripe doesn't support it in Brazil
        if (paymentMethod == Payment.PaymentMethod.PIX) {
            log.debug("Using MercadoPago for PIX payment");
            return mercadoPagoProvider;
        }

        // For PayPal account, always use PayPal provider
        if (paymentMethod == Payment.PaymentMethod.PAYPAL_ACCOUNT) {
            log.debug("Using PayPal for PayPal account payment");
            return paypalProvider;
        }

        // For other methods, use configured provider
        return switch (defaultProvider.toUpperCase()) {
            case "STRIPE" -> {
                if (stripeProvider.supportsPaymentMethod(paymentMethod)) {
                    yield stripeProvider;
                } else {
                    log.warn("Stripe doesn't support {}, falling back to MercadoPago", paymentMethod);
                    yield mercadoPagoProvider;
                }
            }
            case "MERCADOPAGO" -> mercadoPagoProvider;
            case "PAYPAL" -> {
                if (paypalProvider.supportsPaymentMethod(paymentMethod)) {
                    yield paypalProvider;
                } else {
                    log.warn("PayPal doesn't support {}, falling back to MercadoPago", paymentMethod);
                    yield mercadoPagoProvider;
                }
            }
            default -> {
                log.warn("Unknown provider {}, using MercadoPago", defaultProvider);
                yield mercadoPagoProvider;
            }
        };
    }

    /**
     * Get provider for existing payment (based on payment ID format or stored
     * provider info)
     */
    private PaymentProvider getProviderForPayment(String paymentId) {
        // Simple heuristic based on payment ID format
        if (paymentId.startsWith("pi_") || paymentId.startsWith("re_")) {
            return stripeProvider;
        }

        if (paymentId.startsWith("PAY-")) {
            return paypalProvider;
        }

        // Default to configured provider or MercadoPago
        return getProvider(Payment.PaymentMethod.CREDIT_CARD); // Use default logic
    }

    /**
     * Get supported payment methods for current configuration
     */
    public Map<Payment.PaymentMethod, PaymentProvider> getSupportedPaymentMethods() {
        return Map.of(
                Payment.PaymentMethod.PIX, mercadoPagoProvider,
                Payment.PaymentMethod.CREDIT_CARD, getProvider(Payment.PaymentMethod.CREDIT_CARD),
                Payment.PaymentMethod.DEBIT_CARD, getProvider(Payment.PaymentMethod.DEBIT_CARD),
                Payment.PaymentMethod.PAYPAL_ACCOUNT, paypalProvider,
                Payment.PaymentMethod.BANK_TRANSFER, getProvider(Payment.PaymentMethod.BANK_TRANSFER));
    }

    /**
     * Check if a payment method is supported
     */
    public boolean isPaymentMethodSupported(Payment.PaymentMethod paymentMethod) {
        PaymentProvider provider = getProvider(paymentMethod);
        return provider.supportsPaymentMethod(paymentMethod);
    }

    /**
     * Save a payment entity to the database
     */
    public Payment savePayment(Payment payment) {
        log.info("Saving payment for registration: {} with status: {}",
                payment.getRegistration().getId(), payment.getStatus());
        return paymentRepository.save(payment);
    }

    /**
     * Process payment success in a transaction
     * Updates both Payment and Registration status
     */
    @org.springframework.transaction.annotation.Transactional
    public void processPaymentSuccess(Long registrationId, String paymentIntentId,
            long amountInCents, String currency) {
        log.info("Processing payment success in transaction for registration: {}", registrationId);

        // Buscar registration com user e event (eager loading)
        Registration registration = registrationService.get(registrationId);

        // Criar novo Payment
        Payment payment = new Payment();
        payment.setRegistration(registration);
        payment.setAmount(new java.math.BigDecimal(amountInCents).divide(new java.math.BigDecimal(100)));
        payment.setCurrency(currency);
        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        payment.setPaymentMethod(Payment.PaymentMethod.CREDIT_CARD);
        payment.setGatewayProvider("stripe");
        payment.setGatewayPaymentId(paymentIntentId);
        payment.setProcessedAt(java.time.LocalDateTime.now());

        // Salvar payment
        paymentRepository.save(payment);

        // Atualizar status da registration para ACTIVE
        registration.setStatus(Registration.RegistrationStatus.ACTIVE);
        registrationService.save(registration);

        log.info("Payment and registration updated successfully for registration: {}", registrationId);
    }

    /**
     * Process payment failure in a transaction
     * Updates both Payment and Registration status
     */
    @org.springframework.transaction.annotation.Transactional
    public void processPaymentFailure(Long registrationId, String paymentIntentId,
            long amountInCents, String currency) {
        log.info("Processing payment failure in transaction for registration: {}", registrationId);

        // Buscar registration com user e event (eager loading)
        Registration registration = registrationService.get(registrationId);

        // Criar novo Payment com status FAILED
        Payment payment = new Payment();
        payment.setRegistration(registration);
        payment.setAmount(new java.math.BigDecimal(amountInCents).divide(new java.math.BigDecimal(100)));
        payment.setCurrency(currency);
        payment.setStatus(Payment.PaymentStatus.FAILED);
        payment.setPaymentMethod(Payment.PaymentMethod.CREDIT_CARD);
        payment.setGatewayProvider("stripe");
        payment.setGatewayPaymentId(paymentIntentId);
        payment.setProcessedAt(java.time.LocalDateTime.now());

        // Salvar payment
        paymentRepository.save(payment);

        // Manter registration como PENDING (pode tentar pagar novamente)
        // Não cancelar automaticamente

        log.info("Payment failure recorded for registration: {}", registrationId);
    }

    /**
     * List payments with filters
     */
    public org.springframework.data.domain.Page<Payment> listWithFilters(
            Payment.PaymentStatus status,
            Long registrationId,
            String provider,
            org.springframework.data.domain.Pageable pageable) {

        org.springframework.data.jpa.domain.Specification<Payment> spec = com.mvt.mvt_events.specification.PaymentSpecification
                .buildSpecification(status, registrationId, provider);

        return paymentRepository.findAll(spec, pageable);
    }
}
