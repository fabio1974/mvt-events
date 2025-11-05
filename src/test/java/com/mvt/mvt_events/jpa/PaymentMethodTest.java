package com.mvt.mvt_events.jpa;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
class PaymentMethodTest {

    @Test
    void testAllPaymentMethods() {
        PaymentMethod[] methods = PaymentMethod.values();

        assertEquals(6, methods.length);
        assertTrue(contains(methods, PaymentMethod.CREDIT_CARD));
        assertTrue(contains(methods, PaymentMethod.DEBIT_CARD));
        assertTrue(contains(methods, PaymentMethod.PIX));
        assertTrue(contains(methods, PaymentMethod.BANK_SLIP));
        assertTrue(contains(methods, PaymentMethod.CASH));
        assertTrue(contains(methods, PaymentMethod.WALLET));
    }

    @Test
    void testPaymentMethodValueOf() {
        assertEquals(PaymentMethod.CREDIT_CARD, PaymentMethod.valueOf("CREDIT_CARD"));
        assertEquals(PaymentMethod.DEBIT_CARD, PaymentMethod.valueOf("DEBIT_CARD"));
        assertEquals(PaymentMethod.PIX, PaymentMethod.valueOf("PIX"));
        assertEquals(PaymentMethod.BANK_SLIP, PaymentMethod.valueOf("BANK_SLIP"));
        assertEquals(PaymentMethod.CASH, PaymentMethod.valueOf("CASH"));
        assertEquals(PaymentMethod.WALLET, PaymentMethod.valueOf("WALLET"));
    }

    @Test
    void testInvalidPaymentMethod() {
        assertThrows(IllegalArgumentException.class, () -> {
            PaymentMethod.valueOf("INVALID_METHOD");
        });
    }

    @Test
    void testPaymentMethodNames() {
        assertEquals("CREDIT_CARD", PaymentMethod.CREDIT_CARD.name());
        assertEquals("PIX", PaymentMethod.PIX.name());
        assertEquals("CASH", PaymentMethod.CASH.name());
    }

    private boolean contains(PaymentMethod[] methods, PaymentMethod method) {
        for (PaymentMethod m : methods) {
            if (m == method) {
                return true;
            }
        }
        return false;
    }
}
