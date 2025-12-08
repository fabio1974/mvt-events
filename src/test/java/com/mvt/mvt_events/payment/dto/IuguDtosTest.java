package com.mvt.mvt_events.payment.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes unitários para DTOs do Iugu
 * 
 * Foca em:
 * - Factory methods
 * - Helpers de validação
 * - SplitRule.validate()
 */
@DisplayName("Iugu DTOs - Testes")
class IuguDtosTest {

    // ========================================
    // SplitRule Tests
    // ========================================

    @Test
    @DisplayName("SplitRule - Deve criar split por percentual válido")
    void shouldCreatePercentageSplit() {
        // When
        SplitRule split = SplitRule.percentage(
                "acc_motoboy_123",
                BigDecimal.valueOf(87.0),
                "Motoboy - 87%"
        );

        // Then
        assertThat(split.receiverId()).isEqualTo("acc_motoboy_123");
        assertThat(split.percent()).isEqualByComparingTo(BigDecimal.valueOf(87.0));
        assertThat(split.cents()).isNull();
        assertThat(split.splitType()).isEqualTo("percentage");
        assertThat(split.description()).isEqualTo("Motoboy - 87%");
    }

    @Test
    @DisplayName("SplitRule - Deve criar split fixo em centavos válido")
    void shouldCreateFixedCentsSplit() {
        // When
        SplitRule split = SplitRule.fixedCents(
                "acc_platform",
                59, // R$ 0,59 = taxa Iugu
                "Taxa Iugu - R$ 0,59"
        );

        // Then
        assertThat(split.receiverId()).isEqualTo("acc_platform");
        assertThat(split.cents()).isEqualTo(59);
        assertThat(split.percent()).isNull();
        assertThat(split.splitType()).isEqualTo("cents_fixed");
        assertThat(split.description()).isEqualTo("Taxa Iugu - R$ 0,59");
    }

    @Test
    @DisplayName("SplitRule - Deve criar split para motoboy (87%)")
    void shouldCreateCourierSplit() {
        // When
        SplitRule split = SplitRule.forCourier("acc_courier_456");

        // Then
        assertThat(split.receiverId()).isEqualTo("acc_courier_456");
        assertThat(split.percent()).isEqualByComparingTo(BigDecimal.valueOf(87.0));
        assertThat(split.splitType()).isEqualTo("percentage");
        assertThat(split.description()).contains("Motoboy");
    }

    @Test
    @DisplayName("SplitRule - Deve criar split para gerente (5%)")
    void shouldCreateManagerSplit() {
        // When
        SplitRule split = SplitRule.forManager("acc_manager_789");

        // Then
        assertThat(split.receiverId()).isEqualTo("acc_manager_789");
        assertThat(split.percent()).isEqualByComparingTo(BigDecimal.valueOf(5.0));
        assertThat(split.splitType()).isEqualTo("percentage");
        assertThat(split.description()).contains("Gerente");
    }

    @Test
    @DisplayName("SplitRule - Deve criar split para plataforma (8%)")
    void shouldCreatePlatformSplit() {
        // When
        SplitRule split = SplitRule.forPlatform();

        // Then
        assertThat(split.receiverId()).isNull(); // Master account
        assertThat(split.percent()).isEqualByComparingTo(BigDecimal.valueOf(8.0));
        assertThat(split.splitType()).isEqualTo("percentage");
        assertThat(split.description()).contains("Plataforma");
    }

    @Test
    @DisplayName("SplitRule - Deve criar split para taxa Iugu (R$ 0,59)")
    void shouldCreateIuguFeeSplit() {
        // When
        SplitRule split = SplitRule.forIuguFee();

        // Then
        assertThat(split.receiverId()).isNull(); // Master account
        assertThat(split.cents()).isEqualTo(59);
        assertThat(split.splitType()).isEqualTo("cents_fixed");
        assertThat(split.description()).contains("Taxa Iugu");
    }

    @Test
    @DisplayName("SplitRule - Deve validar split por percentual válido")
    void shouldValidatePercentageSplit() {
        // Given
        SplitRule split = SplitRule.percentage(
                "acc_123",
                BigDecimal.valueOf(50.0),
                "Test"
        );

        // When/Then
        assertThatNoException().isThrownBy(() -> split.validate());
    }

    @Test
    @DisplayName("SplitRule - Deve rejeitar percentual negativo")
    void shouldRejectNegativePercentage() {
        // Given
        SplitRule split = SplitRule.percentage(
                "acc_123",
                BigDecimal.valueOf(-10.0), // Inválido
                "Test"
        );

        // When/Then
        assertThatThrownBy(() -> split.validate())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Percentual deve estar entre 0 e 100");
    }

    @Test
    @DisplayName("SplitRule - Deve rejeitar percentual > 100")
    void shouldRejectPercentageOver100() {
        // Given
        SplitRule split = SplitRule.percentage(
                "acc_123",
                BigDecimal.valueOf(150.0), // Inválido
                "Test"
        );

        // When/Then
        assertThatThrownBy(() -> split.validate())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Percentual deve estar entre 0 e 100");
    }

    @Test
    @DisplayName("SplitRule - Deve rejeitar cents negativo")
    void shouldRejectNegativeCents() {
        // Given
        SplitRule split = SplitRule.fixedCents(
                "acc_123",
                -100, // Inválido
                "Test"
        );

        // When/Then
        assertThatThrownBy(() -> split.validate())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Valor em centavos deve ser positivo");
    }

    @Test
    @DisplayName("SplitRule - Deve rejeitar split sem percent e sem cents")
    void shouldRejectSplitWithoutValues() {
        // Given
        SplitRule split = new SplitRule(
                "acc_123",
                null, // Sem percent
                null, // Sem cents - INVÁLIDO
                "percentage",
                "Test"
        );

        // When/Then
        assertThatThrownBy(() -> split.validate())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Split deve ter percent ou cents definido");
    }

    // ========================================
    // SubAccountResponse Tests
    // ========================================

    @Test
    @DisplayName("SubAccountResponse - Pode receber pagamentos quando ativa e verificada")
    void shouldAllowPaymentsWhenActiveAndVerified() {
        // Given
        SubAccountResponse response = new SubAccountResponse(
                "acc_123",
                "João Motoboy",
                "joao@test.com",
                true, // isActive
                true,
                "verified" // verificationStatus
        );

        // Then
        assertThat(response.canReceivePayments()).isTrue();
    }

    @Test
    @DisplayName("SubAccountResponse - Não pode receber se inativa")
    void shouldNotAllowPaymentsWhenInactive() {
        // Given
        SubAccountResponse response = new SubAccountResponse(
                "acc_123",
                "João Motoboy",
                "joao@test.com",
                false, // isActive = false
                true,
                "verified"
        );

        // Then
        assertThat(response.canReceivePayments()).isFalse();
    }

    @Test
    @DisplayName("SubAccountResponse - Não pode receber se não verificada")
    void shouldNotAllowPaymentsWhenNotVerified() {
        // Given
        SubAccountResponse response = new SubAccountResponse(
                "acc_123",
                "João Motoboy",
                "joao@test.com",
                true,
                true,
                "pending" // Não verificada
        );

        // Then
        assertThat(response.canReceivePayments()).isFalse();
    }

    @Test
    @DisplayName("SubAccountResponse - Deve detectar status pendente")
    void shouldDetectPendingStatus() {
        // Given
        SubAccountResponse response = new SubAccountResponse(
                "acc_123",
                "João Motoboy",
                "joao@test.com",
                true,
                true,
                "pending"
        );

        // Then
        assertThat(response.isPendingVerification()).isTrue();
    }

    @Test
    @DisplayName("SubAccountResponse - Não deve ser pendente quando verificada")
    void shouldNotBePendingWhenVerified() {
        // Given
        SubAccountResponse response = new SubAccountResponse(
                "acc_123",
                "João Motoboy",
                "joao@test.com",
                true,
                true,
                "verified"
        );

        // Then
        assertThat(response.isPendingVerification()).isFalse();
    }

    // ========================================
    // InvoiceResponse Tests
    // ========================================

    @Test
    @DisplayName("InvoiceResponse - Deve detectar status pending")
    void shouldDetectPendingInvoice() {
        // Given
        InvoiceResponse invoice = new InvoiceResponse(
                "inv_123",
                "00020101021243650016COM.MERCADOPAGO...",
                "https://qr.iugu.com/...",
                "https://secure.iugu.com/...",
                "pending",
                10000,
                "03/12/2025",
                "cliente@test.com",
                java.util.Map.of()
        );

        // Then
        assertThat(invoice.isPending()).isTrue();
        assertThat(invoice.isPaid()).isFalse();
    }

    @Test
    @DisplayName("InvoiceResponse - Deve detectar status paid")
    void shouldDetectPaidInvoice() {
        // Given
        InvoiceResponse invoice = new InvoiceResponse(
                "inv_123",
                null,
                null,
                "https://secure.iugu.com/...",
                "paid",
                10000,
                "03/12/2025",
                "cliente@test.com",
                java.util.Map.of()
        );

        // Then
        assertThat(invoice.isPaid()).isTrue();
        assertThat(invoice.isPending()).isFalse();
    }

    @Test
    @DisplayName("InvoiceResponse - Deve extrair deliveryId das custom variables")
    void shouldExtractDeliveryId() {
        // Given
        InvoiceResponse invoice = new InvoiceResponse(
                "inv_123",
                "qr_code",
                "qr_url",
                "secure_url",
                "pending",
                10000,
                "03/12/2025",
                "cliente@test.com",
                java.util.Map.of("delivery_id", "uuid-delivery-123")
        );

        // Then
        assertThat(invoice.getDeliveryId()).isEqualTo("uuid-delivery-123");
    }

    // ========================================
    // WebhookEvent Tests
    // ========================================

    @Test
    @DisplayName("WebhookEvent - Deve detectar evento invoice.paid")
    void shouldDetectInvoicePaidEvent() {
        // Given
        WebhookEvent event = new WebhookEvent(
                "invoice.paid",
                java.util.Map.of("id", "inv_123", "status", "paid")
        );

        // Then
        assertThat(event.isPaymentConfirmed()).isTrue();
        assertThat(event.isWithdrawalCompleted()).isFalse();
    }

    @Test
    @DisplayName("WebhookEvent - Deve detectar evento withdrawal.completed")
    void shouldDetectWithdrawalCompletedEvent() {
        // Given
        WebhookEvent event = new WebhookEvent(
                "withdrawal.completed",
                java.util.Map.of("account_id", "acc_123")
        );

        // Then
        assertThat(event.isWithdrawalCompleted()).isTrue();
        assertThat(event.isPaymentConfirmed()).isFalse();
    }

    @Test
    @DisplayName("WebhookEvent - Deve extrair invoice ID")
    void shouldExtractInvoiceId() {
        // Given
        WebhookEvent event = new WebhookEvent(
                "invoice.paid",
                java.util.Map.of("id", "inv_abc123")
        );

        // Then
        assertThat(event.getInvoiceId()).isEqualTo("inv_abc123");
    }

    @Test
    @DisplayName("WebhookEvent - Deve extrair account ID")
    void shouldExtractAccountId() {
        // Given
        WebhookEvent event = new WebhookEvent(
                "withdrawal.completed",
                java.util.Map.of("account_id", "acc_xyz789")
        );

        // Then
        assertThat(event.getAccountId()).isEqualTo("acc_xyz789");
    }
}
