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

    @Builder.Default
    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
    @com.fasterxml.jackson.annotation.JsonIgnore
    @com.mvt.mvt_events.metadata.Visible(table = false, form = false, filter = false)
    private List<Delivery> deliveries = new ArrayList<>();

    /**
     * Retorna a delivery ativa (não-cancelada) mais recente, ou null.
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public Delivery getActiveDelivery() {
        if (deliveries == null || deliveries.isEmpty()) return null;
        return deliveries.stream()
                .filter(d -> d.getStatus() != Delivery.DeliveryStatus.CANCELLED)
                .max(java.util.Comparator.comparingLong(Delivery::getId))
                .orElse(null);
    }

    @Builder.Default
    @Column(name = "order_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private OrderType orderType = OrderType.DELIVERY;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "waiter_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    @com.mvt.mvt_events.metadata.Visible(table = false, form = false, filter = false)
    private User waiter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    @com.mvt.mvt_events.metadata.Visible(table = false, form = false, filter = false)
    private RestaurantTable table;

    /** Número da mesa (denormalizado — mantém histórico mesmo se mesa for deletada) */
    @Column(name = "table_number")
    @com.mvt.mvt_events.metadata.Visible(table = true, form = false, filter = true)
    private Integer tableNumberField;

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

    /** Forma de pagamento informada ao fechar a conta */
    @Column(name = "table_payment_method", length = 20)
    @Enumerated(EnumType.STRING)
    @com.mvt.mvt_events.metadata.Visible(table = false, form = false, filter = false)
    private PaymentMethod tablePaymentMethod;

    @Column(name = "paid_at")
    @com.mvt.mvt_events.metadata.Visible(table = false, form = false, filter = false)
    private OffsetDateTime paidAt;

    /** Status dos items compartilhados (commandId null). Cada comanda tem status próprio. */
    @Enumerated(EnumType.STRING)
    @Column(name = "mesa_status", length = 30, nullable = false)
    @Builder.Default
    @com.mvt.mvt_events.metadata.Visible(table = false, form = false, filter = false)
    private MesaStatus mesaStatus = MesaStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "mesa_payment_method", length = 20)
    @com.mvt.mvt_events.metadata.Visible(table = false, form = false, filter = false)
    private PaymentMethod mesaPaymentMethod;

    @Column(name = "mesa_paid_at")
    @com.mvt.mvt_events.metadata.Visible(table = false, form = false, filter = false)
    private OffsetDateTime mesaPaidAt;

    public enum MesaStatus { OPEN, PAID }

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
        try {
            Delivery active = getActiveDelivery();
            return active != null ? active.getId() : null;
        } catch (Exception e) { return null; }
    }

    @com.fasterxml.jackson.annotation.JsonGetter("waiterName")
    public String getWaiterName() {
        try { return waiter != null ? waiter.getName() : null; } catch (Exception e) { return null; }
    }

    @com.fasterxml.jackson.annotation.JsonGetter("tableNumber")
    public Integer getTableNumber() {
        if (tableNumberField != null) return tableNumberField;
        try { return table != null ? table.getNumber() : null; } catch (Exception e) { return null; }
    }

    // Dados do estabelecimento (CLIENT) — usados no cabeçalho de impressão
    @com.fasterxml.jackson.annotation.JsonGetter("storeName")
    public String getStoreName() {
        try { return client != null ? client.getName() : null; } catch (Exception e) { return null; }
    }

    @com.fasterxml.jackson.annotation.JsonGetter("storeDocument")
    public String getStoreDocument() {
        try { return client != null ? client.getDocumentFormatted() : null; } catch (Exception e) { return null; }
    }

    @com.fasterxml.jackson.annotation.JsonGetter("storePhone")
    public String getStorePhone() {
        try { return client != null ? client.getPhone() : null; } catch (Exception e) { return null; }
    }

    @com.fasterxml.jackson.annotation.JsonGetter("storeAddress")
    public String getStoreAddress() {
        try {
            if (client == null) return null;
            com.mvt.mvt_events.jpa.Address addr = client.getAddress();
            return addr != null ? addr.getFullAddress() : null;
        } catch (Exception e) { return null; }
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
        PLACED,             // Pedido criado, aguardando restaurante
        ACCEPTED,           // Restaurante aceitou
        PREPARING,          // Em preparo
        READY,              // Pronto para retirada / servir
        DELIVERING,         // Em entrega (delivery) / Servido na mesa (table)
        AWAITING_PAYMENT,   // Conta fechada, aguardando pagamento
        COMPLETED,          // Pago e concluído
        CANCELLED           // Cancelado
    }

    public enum OrderType {
        DELIVERY,   // Pedido de entrega (food delivery)
        TABLE       // Pedido de mesa (atendimento no local)
    }
}
