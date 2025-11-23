package com.mvt.mvt_events.jpa;

import com.mvt.mvt_events.metadata.Visible;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Zonas especiais que afetam o cálculo do frete
 * 
 * Cada zona tem um ponto central (latitude/longitude) e um raio de cobertura (radiusMeters).
 * Quando uma delivery tem destino dentro do raio de múltiplas zonas, a ZONA MAIS PRÓXIMA é aplicada.
 * 
 * Tipos de zona:
 * - DANGER: Locais perigosos (aplicam dangerFeePercentage)
 * - HIGH_INCOME: Bairros de alta renda (aplicam highIncomeFeePercentage)
 * 
 * Regra de prioridade: A zona com menor distância do ponto de destino sempre tem prioridade,
 * independente do tipo (DANGER ou HIGH_INCOME).
 */
@Entity
@Table(name = "special_zones")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpecialZone extends BaseEntity {

    /**
     * Latitude do ponto central da zona
     */
    @NotNull(message = "Latitude é obrigatória")
    @Column(name = "latitude", nullable = false)
    @Visible(table = true, form = true, filter = false)
    private Double latitude;

    /**
     * Longitude do ponto central da zona
     */
    @NotNull(message = "Longitude é obrigatória")
    @Column(name = "longitude", nullable = false)
    @Visible(table = true, form = true, filter = false)
    private Double longitude;

    /**
     * Endereço descritivo da zona
     * Ex: "Favela do Moinho, São Paulo - SP"
     */
    @NotBlank(message = "Endereço é obrigatório")
    @Size(max = 500, message = "Endereço deve ter no máximo 500 caracteres")
    @Column(name = "address", columnDefinition = "TEXT", nullable = false)
    @Visible(table = true, form = true, filter = false)
    private String address;

    /**
     * Tipo da zona especial
     */
    @NotNull(message = "Tipo é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(name = "zone_type", nullable = false, length = 20)
    @Visible(table = true, form = true, filter = true)
    private ZoneType zoneType;

    /**
     * Raio de cobertura da zona em metros
     * Define a distância máxima do ponto central para aplicar a taxa especial
     * Padrão: 300 metros
     */
    @NotNull(message = "Raio é obrigatório")
    @Column(name = "radius_meters", nullable = false)
    @Visible(table = true, form = true, filter = false)
    @Builder.Default
    private Double radiusMeters = 300.0;

    /**
     * Indica se esta zona está ativa
     */
    @Column(name = "is_active", nullable = false)
    @Visible(table = true, form = true, filter = true)
    private Boolean isActive = true;

    /**
     * Observações sobre a zona
     */
    @Size(max = 500, message = "Observações podem ter no máximo 500 caracteres")
    @Column(name = "notes", columnDefinition = "TEXT")
    @Visible(table = false, form = true, filter = false)
    private String notes;

    public enum ZoneType {
        DANGER,        // Zona perigosa - aplica dangerFeePercentage
        HIGH_INCOME    // Zona de alta renda - aplica highIncomeFeePercentage
    }
}
