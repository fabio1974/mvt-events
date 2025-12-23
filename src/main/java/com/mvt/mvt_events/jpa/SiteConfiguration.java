package com.mvt.mvt_events.jpa;

import com.mvt.mvt_events.metadata.Visible;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Configurações globais do site/plataforma
 * Apenas uma linha ativa por vez (singleton pattern)
 */
@Entity
@Table(name = "site_configurations")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SiteConfiguration extends BaseEntity {

    /**
     * Preço por km para cálculo do frete (em Reais)
     * Ex: 1.50 = R$ 1,50 por km
     */
    @NotNull(message = "Preço por km é obrigatório")
    @DecimalMin(value = "0.01", message = "Preço por km deve ser maior que zero")
    @DecimalMax(value = "100.00", message = "Preço por km não pode exceder R$ 100,00")
    @Column(name = "price_per_km", precision = 10, scale = 2, nullable = false)
    @Visible(table = true, form = true, filter = false)
    private BigDecimal pricePerKm;

    /**
     * Valor mínimo do frete (em Reais)
     * Ex: 5.00 = R$ 5,00 mínimo
     */
    @NotNull(message = "Valor mínimo do frete é obrigatório")
    @DecimalMin(value = "0.00", message = "Valor mínimo do frete não pode ser negativo")
    @DecimalMax(value = "1000.00", message = "Valor mínimo do frete não pode exceder R$ 1.000,00")
    @Column(name = "minimum_shipping_fee", precision = 10, scale = 2, nullable = false)
    @Visible(table = true, form = true, filter = false)
    private BigDecimal minimumShippingFee;

    /**
     * Percentual de comissão para o gerente/organizador (0-100)
     * Ex: 10.00 = 10%
     */
    @NotNull(message = "Percentual do gerente é obrigatório")
    @DecimalMin(value = "0.00", message = "Percentual do gerente não pode ser negativo")
    @DecimalMax(value = "100.00", message = "Percentual do gerente não pode exceder 100%")
    @Column(name = "organizer_percentage", precision = 5, scale = 2, nullable = false)
    @Visible(table = true, form = true, filter = false)
    private BigDecimal organizerPercentage;

    /**
     * Percentual de comissão para a plataforma (0-100)
     * Ex: 15.00 = 15%
     */
    @NotNull(message = "Percentual da plataforma é obrigatório")
    @DecimalMin(value = "0.00", message = "Percentual da plataforma não pode ser negativo")
    @DecimalMax(value = "100.00", message = "Percentual da plataforma não pode exceder 100%")
    @Column(name = "platform_percentage", precision = 5, scale = 2, nullable = false)
    @Visible(table = true, form = true, filter = false)
    private BigDecimal platformPercentage;

    /**
     * ID do recipient Pagar.me da plataforma/empresa
     * Usado no split de pagamentos para receber a comissão da plataforma
     * Ex: "rp_abc123xyz456"
     */
    @Column(name = "pagarme_recipient_id", length = 100)
    @Size(max = 100, message = "ID do recipient Pagar.me pode ter no máximo 100 caracteres")
    @Visible(table = true, form = true, filter = false)
    private String pagarmeRecipientId;

    /**
     * Taxa de periculosidade (0-100)
     * Ex: 10.00 = 10% de acréscimo por periculosidade
     */
    @NotNull(message = "Taxa de periculosidade é obrigatória")
    @DecimalMin(value = "0.00", message = "Taxa de periculosidade não pode ser negativa")
    @DecimalMax(value = "100.00", message = "Taxa de periculosidade não pode exceder 100%")
    @Column(name = "danger_fee_percentage", precision = 5, scale = 2, nullable = false)
    @Visible(table = true, form = true, filter = false)
    private BigDecimal dangerFeePercentage;

    /**
     * Taxa de renda alta (0-100)
     * Ex: 15.00 = 15% de acréscimo para bairros de renda alta
     */
    @NotNull(message = "Taxa de renda alta é obrigatória")
    @DecimalMin(value = "0.00", message = "Taxa de renda alta não pode ser negativa")
    @DecimalMax(value = "100.00", message = "Taxa de renda alta não pode exceder 100%")
    @Column(name = "high_income_fee_percentage", precision = 5, scale = 2, nullable = false)
    @Visible(table = true, form = true, filter = false)
    private BigDecimal highIncomeFeePercentage;

    /**
     * Indica se esta configuração está ativa
     * Apenas uma configuração pode estar ativa por vez
     */
    @Column(name = "is_active", nullable = false)
    @Visible(table = true, form = true, filter = true)
    private Boolean isActive;

    /**
     * Usuário que criou/atualizou a configuração
     */
    @Column(name = "updated_by")
    @Visible(table = true, form = false, filter = false, readonly = true)
    private String updatedBy;

    /**
     * Observações sobre a configuração
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    @Size(max = 300, message = "Observações podem ter no máximo 300 caracteres")  
    @Visible(table = false, form = true, filter = false)
    private String notes;

    @PrePersist
    protected void onCreate() {
        if (isActive == null) {
            isActive = true;
        }
    }
}
