package com.mvt.mvt_events.jpa;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * Auditoria de transferências pagar.me entre recipients.
 * Usado hoje para transferir 87% do frete da plataforma → courier
 * quando o courier aceita a delivery de um pedido Zapi-Food já pago.
 */
@Entity
@Table(name = "pagarme_transfers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagarmeTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_order_id")
    @JsonIgnore
    private FoodOrder foodOrder;

    @Column(name = "delivery_id")
    private Long deliveryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    @JsonIgnore
    private User recipient;

    @Column(name = "recipient_pagarme_id", length = 100, nullable = false)
    private String recipientPagarmeId;

    @Column(name = "amount_cents", nullable = false)
    private Long amountCents;

    @Column(name = "pagarme_transfer_id", length = 100)
    private String pagarmeTransferId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "executed_at")
    private OffsetDateTime executedAt;

    public enum Status { PENDING, SUCCEEDED, FAILED }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
