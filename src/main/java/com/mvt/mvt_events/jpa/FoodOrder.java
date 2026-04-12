package com.mvt.mvt_events.jpa;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Pedido do módulo Zapi-Food.
 * Nomeado FoodOrder para evitar conflito com a palavra reservada "Order" do SQL.
 * Status: PLACED → ACCEPTED → PREPARING → READY → DELIVERING → COMPLETED / CANCELLED
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private User client;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_id")
    private Delivery delivery;

    @Builder.Default
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PLACED;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "delivery_fee", precision = 10, scale = 2)
    private BigDecimal deliveryFee;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "estimated_preparation_minutes")
    private Integer estimatedPreparationMinutes;

    @Column(name = "accepted_at")
    private OffsetDateTime acceptedAt;

    @Column(name = "preparing_at")
    private OffsetDateTime preparingAt;

    @Column(name = "ready_at")
    private OffsetDateTime readyAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    // ============================================================================
    // ENUMS
    // ============================================================================

    public enum OrderStatus {
        PLACED,     // Pedido criado, aguardando restaurante
        ACCEPTED,   // Restaurante aceitou
        PREPARING,  // Em preparo
        READY,      // Pronto para retirada → cria Delivery
        DELIVERING, // Courier coletou, em trânsito
        COMPLETED,  // Entregue
        CANCELLED   // Cancelado (por cliente ou restaurante)
    }
}
