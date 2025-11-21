package com.mvt.mvt_events.jpa;

import com.mvt.mvt_events.metadata.Visible;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Representa um item de repasse individual para um beneficiário.
 * Cada PayoutItem rastreia um repasse específico (valor, beneficiário, status, pagamento).
 * Criado automaticamente quando um Payment é processado e dividido entre beneficiários.
 */
@Entity
@Table(name = "payout_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PayoutItem extends BaseEntity {

    // ============================================================================
    // RELATIONSHIPS
    // ============================================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    @Visible(table = true, form = true, filter = true)
    private Payment payment;

    // ============================================================================
    // ITEM VALUE
    // ============================================================================

    @NotNull(message = "Valor do item é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor mínimo é R$ 0,01")
    @Column(name = "item_value", precision = 10, scale = 2, nullable = false)
    @Visible(table = true, form = false, filter = false)
    private BigDecimal itemValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", nullable = false, length = 20)
    @Visible(table = true, form = false, filter = true)
    private ValueType valueType;

    // ============================================================================
    // BENEFICIARY
    // ============================================================================

    @NotNull(message = "Beneficiário é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beneficiary_id", nullable = false)
    @Visible(table = true, form = true, filter = true)
    private User beneficiary; // Quem vai receber este repasse

    // ============================================================================
    // PAYOUT STATUS & TRACKING
    // ============================================================================

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Visible(table = true, form = true, filter = true)
    private PayoutStatus status = PayoutStatus.PENDING;

    @Column(name = "paid_at")
    @Visible(table = true, form = false, filter = false)
    private java.time.LocalDateTime paidAt;

    @Column(name = "payment_reference", length = 100)
    @Visible(table = true, form = false, filter = false)
    private String paymentReference; // ID da transação split (PIX, transferência, etc)

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    @Visible(table = true, form = false, filter = true)
    private PaymentMethod paymentMethod;

    @Column(name = "notes", columnDefinition = "TEXT")
    @Visible(table = false, form = true, filter = false)
    private String notes; // Observações sobre o repasse

    // ============================================================================
    // ENUMS
    // ============================================================================

    public enum ValueType {
        COURIER_AMOUNT, // Valor destinado ao motoboy
        ADM_COMMISSION, // Comissão destinada ao ADM/Gerente
        SYSTEM_FEE, // Taxa do sistema Zap10
        PLATFORM_FEE, // Taxa da plataforma de pagamento
        OTHER // Outros tipos de repasse
    }

    public enum PayoutStatus {
        PENDING, // Aguardando processamento
        PROCESSING, // Em processamento
        PAID, // Pago com sucesso
        FAILED, // Falhou
        CANCELLED // Cancelado
    }

    public enum PaymentMethod {
        PIX, // PIX
        BANK_TRANSFER, // Transferência bancária
        CASH, // Dinheiro
        WALLET, // Carteira digital
        OTHER // Outro método
    }

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    /**
     * Verifica se o repasse foi pago
     */
    public boolean isPaid() {
        return status == PayoutStatus.PAID;
    }

    /**
     * Marca o repasse como pago
     */
    public void markAsPaid(String reference, PaymentMethod method) {
        this.status = PayoutStatus.PAID;
        this.paidAt = java.time.LocalDateTime.now();
        this.paymentReference = reference;
        this.paymentMethod = method;
    }

    /**
     * Marca o repasse como falho
     */
    public void markAsFailed(String reason) {
        this.status = PayoutStatus.FAILED;
        this.notes = reason;
    }

    /**
     * Cancela o repasse
     */
    public void cancel(String reason) {
        this.status = PayoutStatus.CANCELLED;
        this.notes = reason;
    }
}
