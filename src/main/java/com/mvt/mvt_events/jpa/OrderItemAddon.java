package com.mvt.mvt_events.jpa;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

/**
 * Adicional pendurado em um OrderItem.
 *
 * Exemplo: num pedido "2x Classic Burger", cada unidade pode ter adicionais diferentes,
 * então cada Burger vira um OrderItem separado e cada um carrega sua lista de addons.
 *
 * unitPrice é snapshot no momento do pedido — mudanças posteriores no Product.price
 * não afetam pedidos já lançados (mesma regra de {@link OrderItem#unitPrice}).
 */
@Entity
@Table(name = "order_item_addons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemAddon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Product product;

    @NotNull
    @Column(nullable = false)
    private Integer quantity;

    @NotNull
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @com.fasterxml.jackson.annotation.JsonGetter("productId")
    public Long getProductId() {
        try { return product != null ? product.getId() : null; } catch (Exception e) { return null; }
    }

    @com.fasterxml.jackson.annotation.JsonGetter("productName")
    public String getProductName() {
        try { return product != null ? product.getName() : null; } catch (Exception e) { return null; }
    }
}
