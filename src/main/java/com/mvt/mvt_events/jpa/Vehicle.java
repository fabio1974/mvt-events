package com.mvt.mvt_events.jpa;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mvt.mvt_events.metadata.DisplayLabel;
import com.mvt.mvt_events.metadata.Visible;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Entidade que representa um veículo
 * 
 * Relacionamentos:
 * - N:1 com User (muitos veículos pertencem a um proprietário)
 */
@Entity
@Table(name = "vehicles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class Vehicle extends BaseEntity {

    // ============================================================================
    // RELATIONSHIPS
    // ============================================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    @JsonIgnore
    @Visible(table = true, form = false, filter = false)
    private User owner;

    // ============================================================================
    // VEHICLE FIELDS
    // ============================================================================

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    @Visible(table = true, form = true, filter = true)
    private VehicleType type;

    @DisplayLabel
    @NotBlank(message = "Placa é obrigatória")
    @Size(max = 10, message = "Placa não pode exceder 10 caracteres")
    @Column(name = "plate", nullable = false, length = 10, unique = true)
    @Visible(table = true, form = true, filter = true)
    private String plate;

    @NotBlank(message = "Marca é obrigatória")
    @Size(max = 50, message = "Marca não pode exceder 50 caracteres")
    @Column(name = "brand", nullable = false, length = 50)
    @Visible(table = true, form = true, filter = true)
    private String brand;

    @NotBlank(message = "Modelo é obrigatório")
    @Size(max = 50, message = "Modelo não pode exceder 50 caracteres")
    @Column(name = "model", nullable = false, length = 50)
    @Visible(table = true, form = true, filter = true)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(name = "color", nullable = false, length = 20)
    @Visible(table = true, form = true, filter = true)
    private VehicleColor color;

    @Size(max = 4, message = "Ano deve ter no máximo 4 caracteres")
    @Column(name = "year", length = 4)
    @Visible(table = true, form = true, filter = true)
    private String year;

    @Column(name = "is_active", nullable = false)
    @Visible(table = true, form = true, filter = true)
    @Builder.Default
    private Boolean isActive = true;

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    /**
     * Retorna descrição completa do veículo
     */
    public String getFullDescription() {
        return String.format("%s %s %s - %s (%s)", 
            color, brand, model, plate, type.getDisplayName());
    }

    /**
     * Retorna descrição curta do veículo
     */
    public String getShortDescription() {
        return String.format("%s %s - %s", brand, model, plate);
    }

    /**
     * Verifica se é uma moto
     */
    public boolean isMotorcycle() {
        return type == VehicleType.MOTORCYCLE;
    }

    /**
     * Verifica se é um automóvel
     */
    public boolean isCar() {
        return type == VehicleType.CAR;
    }

    /**
     * Retorna nome do proprietário
     */
    public String getOwnerName() {
        return owner != null ? owner.getName() : null;
    }
}
