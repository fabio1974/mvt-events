package com.mvt.mvt_events.jpa;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Item de um pedido — módulo Zapi-Food.
 * Snapshot do preço no momento da compra.
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private FoodOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Product product;

    @Builder.Default
    @NotNull
    @Column(nullable = false)
    private Integer quantity = 1;

    @NotNull
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /**
     * Observação por item (ex: "sem cebola", "ponto da carne: bem passado").
     * Canônico em pedidos de mesa a partir da fase 2; `notes` fica para uso legado/Zapi-Food.
     */
    @Column(columnDefinition = "TEXT")
    private String observation;

    /**
     * Adicionais pendurados neste item (ex: cheddar, ovo). Cascade total — removido junto.
     * Set (não List) pra evitar MultipleBagFetchException quando FoodOrder.items também é fetched.
     */
    @Builder.Default
    @OneToMany(mappedBy = "orderItem", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<OrderItemAddon> addons = new LinkedHashSet<>();

    /** Rodada de envio à cozinha (1 = pedido original, 2+ = itens adicionados depois) */
    @Builder.Default
    @Column(nullable = false)
    private Integer round = 1;

    /** Comanda à qual o item pertence; null = compartilhado entre todos */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "command_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private OrderCommand command;

    @com.fasterxml.jackson.annotation.JsonGetter("commandId")
    public Long getCommandId() {
        try { return command != null ? command.getId() : null; } catch (Exception e) { return null; }
    }

    /** Quando o item foi enviado à cozinha */
    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    /** Se o item deve ser empacotado pra viagem (embalo) */
    @Builder.Default
    @Column(nullable = false)
    private Boolean packaged = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @com.fasterxml.jackson.annotation.JsonGetter("productName")
    public String getProductName() {
        try { return product != null ? product.getName() : null; } catch (Exception e) { return null; }
    }

    @com.fasterxml.jackson.annotation.JsonGetter("productId")
    public Long getProductId() {
        try { return product != null ? product.getId() : null; } catch (Exception e) { return null; }
    }

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        if (sentAt == null) sentAt = createdAt;
    }
}
