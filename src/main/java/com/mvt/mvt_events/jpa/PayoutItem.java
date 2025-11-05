package com.mvt.mvt_events.jpa;

import com.mvt.mvt_events.metadata.Visible;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Tabela intermediária que conecta UnifiedPayout com Payment.
 * Cada item representa uma parte de um Payment que foi incluída em um repasse.
 * Permite rastreabilidade completa: Payment → PayoutItem → UnifiedPayout →
 * Beneficiário.
 */
@Entity
@Table(name = "payout_items", uniqueConstraints = @UniqueConstraint(columnNames = { "payout_id", "payment_id" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PayoutItem extends BaseEntity {

    // ============================================================================
    // RELATIONSHIPS
    // ============================================================================

    @NotNull(message = "Repasse é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payout_id", nullable = false)
    @Visible(table = false, form = false, filter = true)
    private UnifiedPayout payout;

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
    // ENUMS
    // ============================================================================

    public enum ValueType {
        COURIER_AMOUNT, // Valor destinado ao motoboy
        ADM_COMMISSION // Comissão destinada ao ADM
    }
}
