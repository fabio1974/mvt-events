package com.mvt.mvt_events.jpa;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
class PaymentStatusTest {

    @Test
    void testAllPaymentStatuses() {
        PaymentStatus[] statuses = PaymentStatus.values();

        assertEquals(6, statuses.length);
        assertTrue(contains(statuses, PaymentStatus.PENDING));
        assertTrue(contains(statuses, PaymentStatus.PROCESSING));
        assertTrue(contains(statuses, PaymentStatus.COMPLETED));
        assertTrue(contains(statuses, PaymentStatus.FAILED));
        assertTrue(contains(statuses, PaymentStatus.REFUNDED));
        assertTrue(contains(statuses, PaymentStatus.CANCELLED));
    }

    @Test
    void testPaymentStatusValueOf() {
        assertEquals(PaymentStatus.PENDING, PaymentStatus.valueOf("PENDING"));
        assertEquals(PaymentStatus.PROCESSING, PaymentStatus.valueOf("PROCESSING"));
        assertEquals(PaymentStatus.COMPLETED, PaymentStatus.valueOf("COMPLETED"));
        assertEquals(PaymentStatus.FAILED, PaymentStatus.valueOf("FAILED"));
        assertEquals(PaymentStatus.REFUNDED, PaymentStatus.valueOf("REFUNDED"));
        assertEquals(PaymentStatus.CANCELLED, PaymentStatus.valueOf("CANCELLED"));
    }

    @Test
    void testInvalidPaymentStatus() {
        assertThrows(IllegalArgumentException.class, () -> {
            PaymentStatus.valueOf("INVALID_STATUS");
        });
    }

    private boolean contains(PaymentStatus[] statuses, PaymentStatus status) {
        for (PaymentStatus s : statuses) {
            if (s == status) {
                return true;
            }
        }
        return false;
    }
}
