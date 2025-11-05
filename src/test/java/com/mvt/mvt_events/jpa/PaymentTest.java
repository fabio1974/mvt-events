package com.mvt.mvt_events.jpa;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
class PaymentTest {

    private Payment payment;
    private Delivery delivery;
    private User payer;
    private Organization organization;

    @BeforeEach
    void setUp() {
        // Create test entities
        delivery = new Delivery();
        delivery.setId(1L);

        payer = new User();
        payer.setId("user-uuid-123");
        payer.setName("Test User");

        organization = new Organization();
        organization.setId(1L);
        organization.setName("Test Org");

        // Create payment
        payment = new Payment();
        payment.setDelivery(delivery);
        payment.setPayer(payer);
        payment.setOrganization(organization);
        payment.setAmount(new BigDecimal("100.00"));
        payment.setPaymentMethod(PaymentMethod.PIX);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setTransactionId("TXN-123456");
        payment.setProvider("mercadopago");
    }

    @Test
    void testPaymentCreation() {
        assertNotNull(payment);
        assertEquals(new BigDecimal("100.00"), payment.getAmount());
        assertEquals(PaymentMethod.PIX, payment.getPaymentMethod());
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        assertEquals("TXN-123456", payment.getTransactionId());
        assertEquals("mercadopago", payment.getProvider());
    }

    @Test
    void testMarkAsCompleted() {
        payment.markAsCompleted();

        assertEquals(PaymentStatus.COMPLETED, payment.getStatus());
        assertNotNull(payment.getPaymentDate());
    }

    @Test
    void testMarkAsFailed() {
        String errorMessage = "Payment declined";
        payment.markAsFailed(errorMessage);

        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        assertTrue(payment.getNotes().contains(errorMessage));
    }

    @Test
    void testMarkAsRefunded() {
        payment.markAsCompleted(); // First complete it
        payment.markAsRefunded();

        assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
    }

    @Test
    void testMarkAsCancelled() {
        payment.markAsCancelled();

        assertEquals(PaymentStatus.CANCELLED, payment.getStatus());
    }

    @Test
    void testMarkAsProcessing() {
        payment.markAsProcessing();

        assertEquals(PaymentStatus.PROCESSING, payment.getStatus());
    }

    @Test
    void testIsCompleted() {
        assertFalse(payment.isCompleted());

        payment.markAsCompleted();
        assertTrue(payment.isCompleted());
    }

    @Test
    void testIsPending() {
        assertTrue(payment.isPending());

        payment.markAsCompleted();
        assertFalse(payment.isPending());
    }

    @Test
    void testIsFailed() {
        assertFalse(payment.isFailed());

        payment.markAsFailed("Error");
        assertTrue(payment.isFailed());
    }

    @Test
    void testIsRefunded() {
        assertFalse(payment.isRefunded());

        payment.markAsCompleted();
        payment.markAsRefunded();
        assertTrue(payment.isRefunded());
    }

    @Test
    void testAddNote() {
        String note1 = "First note";
        String note2 = "Second note";

        payment.addNote(note1);
        assertTrue(payment.getNotes().contains(note1));

        payment.addNote(note2);
        assertTrue(payment.getNotes().contains(note1));
        assertTrue(payment.getNotes().contains(note2));
    }

    @Test
    void testAmountValidation() {
        // Amount should be positive
        payment.setAmount(new BigDecimal("50.00"));
        assertTrue(payment.getAmount().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void testPaymentRelationships() {
        assertNotNull(payment.getDelivery());
        assertNotNull(payment.getPayer());
        assertNotNull(payment.getOrganization());

        assertEquals(1L, payment.getDelivery().getId());
        assertEquals("user-uuid-123", payment.getPayer().getId());
        assertEquals(1L, payment.getOrganization().getId());
    }

    @Test
    void testProviderPaymentId() {
        String providerPaymentId = "MP-123456789";
        payment.setProviderPaymentId(providerPaymentId);

        assertEquals(providerPaymentId, payment.getProviderPaymentId());
    }

    @Test
    void testPaymentMetadata() {
        String metadata = "{\"customer_id\": \"CUST-123\", \"ip_address\": \"192.168.1.1\"}";
        payment.setMetadata(metadata);

        assertEquals(metadata, payment.getMetadata());
        assertTrue(payment.getMetadata().contains("customer_id"));
    }
}
