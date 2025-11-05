package com.mvt.mvt_events.jpa;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mvt.mvt_events.metadata.DisplayLabel;
import com.mvt.mvt_events.metadata.Visible;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidade que representa um pagamento de uma entrega.
 * 
 * Relacionamentos:
 * - N:1 com Delivery (um pagamento pertence a uma entrega)
 * - N:1 com User (payer - quem está pagando)
 * - 1:N com PayoutItem (um pagamento pode estar em múltiplos payouts)
 */
@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Payment extends BaseEntity {

    // ============================================================================
    // RELATIONSHIPS
    // ============================================================================

    @NotNull(message = "Entrega é obrigatória")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_id", nullable = false)
    @JsonIgnore
    @Visible(table = true, form = true, filter = true)
    private Delivery delivery;

    @NotNull(message = "Pagador é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_id", nullable = false)
    @JsonIgnore
    @Visible(table = true, form = true, filter = true)
    private User payer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    @JsonIgnore
    @Visible(table = true, form = true, filter = true)
    private Organization organization;

    // ============================================================================
    // PAYMENT INFO
    // ============================================================================

    @DisplayLabel
    @Column(name = "transaction_id", length = 100, unique = true)
    @Visible(table = true, form = false, filter = true)
    private String transactionId;

    @NotNull(message = "Valor é obrigatório")
    @Column(nullable = false, precision = 10, scale = 2)
    @Visible(table = true, form = true, filter = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    @Visible(table = true, form = true, filter = true)
    private PaymentMethod paymentMethod;

    @NotNull(message = "Status é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Visible(table = true, form = true, filter = true)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "payment_date")
    @Visible(table = true, form = false, filter = true)
    private LocalDateTime paymentDate;

    @Column(name = "provider", length = 50)
    @Visible(table = true, form = true, filter = true)
    private String provider; // stripe, mercadopago, paypal, etc

    @Column(name = "provider_payment_id", length = 100)
    @Visible(table = false, form = false, filter = false)
    private String providerPaymentId;

    // ============================================================================
    // METADATA
    // ============================================================================

    @Column(name = "notes", columnDefinition = "TEXT")
    @Visible(table = false, form = true, filter = false)
    private String notes;

    @Column(name = "metadata", columnDefinition = "JSONB")
    @Visible(table = false, form = false, filter = false)
    private String metadata;

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    public boolean isPending() {
        return status == PaymentStatus.PENDING;
    }

    public boolean isCompleted() {
        return status == PaymentStatus.COMPLETED;
    }

    public boolean isFailed() {
        return status == PaymentStatus.FAILED;
    }

    public boolean isRefunded() {
        return status == PaymentStatus.REFUNDED;
    }

    public boolean canBeRefunded() {
        return status == PaymentStatus.COMPLETED;
    }

    public void markAsCompleted() {
        this.status = PaymentStatus.COMPLETED;
        this.paymentDate = LocalDateTime.now();
    }

    public void markAsFailed() {
        this.status = PaymentStatus.FAILED;
    }

    public void markAsRefunded() {
        if (!canBeRefunded()) {
            throw new IllegalStateException("Pagamento não pode ser reembolsado no status: " + status);
        }
        this.status = PaymentStatus.REFUNDED;
    }

    public void markAsCancelled() {
        if (status == PaymentStatus.COMPLETED) {
            throw new IllegalStateException("Pagamento concluído não pode ser cancelado. Use refund.");
        }
        this.status = PaymentStatus.CANCELLED;
    }
}
