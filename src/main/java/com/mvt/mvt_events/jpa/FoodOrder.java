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
    @com.fasterxml.jackson.annotation.JsonIgnore
    @com.mvt.mvt_events.metadata.Visible(table = false, form = false, filter = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    @com.mvt.mvt_events.metadata.Visible(table = false, form = false, filter = false)
    private User client;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    @com.mvt.mvt_events.metadata.Visible(table = false, form = false, filter = false)
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

    @Column(name = "delivery_address", columnDefinition = "TEXT")
    private String deliveryAddress;

    @Column(name = "delivery_latitude")
    @com.mvt.mvt_events.metadata.Visible(table = false, form = false, filter = false)
    private Double deliveryLatitude;

    @Column(name = "delivery_longitude")
    @com.mvt.mvt_events.metadata.Visible(table = false, form = false, filter = false)
    private Double deliveryLongitude;

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
    @com.mvt.mvt_events.metadata.Visible(table = false, form = false, filter = false)
    private List<OrderItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // Campos computed para serialização (tabela/view)
    @com.fasterxml.jackson.annotation.JsonGetter("customerName")
    public String getCustomerName() {
        try { return customer != null ? customer.getName() : null; } catch (Exception e) { return null; }
    }

    @com.fasterxml.jackson.annotation.JsonGetter("customerEmail")
    public String getCustomerEmail() {
        try { return customer != null ? customer.getUsername() : null; } catch (Exception e) { return null; }
    }

    @com.fasterxml.jackson.annotation.JsonGetter("customerPhone")
    public String getCustomerPhone() {
        try {
            if (customer == null) return null;
            String ddd = customer.getPhoneDdd();
            String num = customer.getPhoneNumber();
            if (ddd == null || num == null) return null;
            if (num.length() == 9) {
                return String.format("(%s) %s-%s", ddd, num.substring(0, 5), num.substring(5));
            }
            return String.format("(%s) %s", ddd, num);
        } catch (Exception e) { return null; }
    }

    @com.fasterxml.jackson.annotation.JsonGetter("deliveryIdValue")
    public Long getDeliveryIdValue() {
        try { return delivery != null ? delivery.getId() : null; } catch (Exception e) { return null; }
    }

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
