package com.mvt.mvt_events.payment;

import com.mvt.mvt_events.jpa.Payment;
import com.mvt.mvt_events.jpa.Registration;
import com.mvt.mvt_events.repository.PaymentRepository;
import com.mvt.mvt_events.service.RegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PaymentServiceTransactionTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RegistrationService registrationService;

    @InjectMocks
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void processPaymentSuccess_shouldCreatePaymentAndUpdateRegistrationStatus() {
        // Given
        Long registrationId = 1L;
        String paymentIntentId = "pi_test_123";
        long amountInCents = 5000L;
        String currency = "BRL";

        Registration mockRegistration = new Registration();
        mockRegistration.setId(registrationId);
        mockRegistration.setTenantId(1L);
        mockRegistration.setStatus(Registration.RegistrationStatus.PENDING);

        when(registrationService.get(registrationId)).thenReturn(mockRegistration);
        when(paymentRepository.save(any(Payment.class))).thenReturn(new Payment());
        when(registrationService.save(any(Registration.class))).thenReturn(mockRegistration);

        // When
        paymentService.processPaymentSuccess(registrationId, paymentIntentId, amountInCents, currency);

        // Then
        verify(registrationService).get(registrationId);
        verify(paymentRepository).save(argThat(payment -> payment.getStatus() == Payment.PaymentStatus.COMPLETED &&
                payment.getGatewayPaymentId().equals(paymentIntentId) &&
                payment.getGatewayProvider().equals("stripe")));
        verify(registrationService)
                .save(argThat(registration -> registration.getStatus() == Registration.RegistrationStatus.ACTIVE));
    }

    @Test
    void processPaymentFailure_shouldCreateFailedPaymentAndKeepRegistrationPending() {
        // Given
        Long registrationId = 1L;
        String paymentIntentId = "pi_test_456";
        long amountInCents = 5000L;
        String currency = "BRL";

        Registration mockRegistration = new Registration();
        mockRegistration.setId(registrationId);
        mockRegistration.setTenantId(1L);
        mockRegistration.setStatus(Registration.RegistrationStatus.PENDING);

        when(registrationService.get(registrationId)).thenReturn(mockRegistration);
        when(paymentRepository.save(any(Payment.class))).thenReturn(new Payment());

        // When
        paymentService.processPaymentFailure(registrationId, paymentIntentId, amountInCents, currency);

        // Then
        verify(registrationService).get(registrationId);
        verify(paymentRepository).save(argThat(payment -> payment.getStatus() == Payment.PaymentStatus.FAILED &&
                payment.getGatewayPaymentId().equals(paymentIntentId) &&
                payment.getGatewayProvider().equals("stripe")));
        // Registration não deve ser alterada em caso de falha (permanece PENDING)
        verify(registrationService, never()).save(any(Registration.class));
    }
}