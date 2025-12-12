package com.mvt.mvt_events.jpa;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mvt.mvt_events.metadata.DisplayLabel;
import com.mvt.mvt_events.metadata.Visible;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Entidade que representa o endereço de um usuário
 * 
 * Relacionamentos:
 * - N:1 com User (muitos endereços pertencem a um usuário)
 * - N:1 com City (muitos endereços pertencem a uma cidade)
 */
@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Address extends BaseEntity {

    // ============================================================================
    // RELATIONSHIPS
    // ============================================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    @Visible(table = true, form = false, filter = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id") // Nullable - permitir endereços sem cidade cadastrada
    @JsonIgnore
    @Visible(table = true, form = true, filter = true)
    private City city;

    // ============================================================================
    // ADDRESS FIELDS
    // ============================================================================

    @DisplayLabel
    @NotBlank(message = "Rua é obrigatória")
    @Size(max = 200, message = "Rua não pode exceder 200 caracteres")
    @Column(name = "street", nullable = false, length = 200)
    @Visible(table = true, form = true, filter = false)
    private String street;

    @NotBlank(message = "Número é obrigatório")
    @Size(max = 10, message = "Número não pode exceder 10 caracteres")
    @Column(name = "number", nullable = false, length = 10)
    @Visible(table = true, form = true, filter = false)
    private String number;

    @Size(max = 100, message = "Complemento não pode exceder 100 caracteres")
    @Column(name = "complement", length = 100)
    @Visible(table = false, form = true, filter = false)
    private String complement;

    @NotBlank(message = "Bairro é obrigatório")
    @Size(max = 100, message = "Bairro não pode exceder 100 caracteres")
    @Column(name = "neighborhood", nullable = false, length = 100)
    @Visible(table = true, form = true, filter = true)
    private String neighborhood;

    @Size(max = 200, message = "Ponto de referência não pode exceder 200 caracteres")
    @Column(name = "reference_point", length = 200)
    @Visible(table = false, form = true, filter = false)
    private String referencePoint;

    @Size(max = 8, message = "CEP deve ter no máximo 8 caracteres")
    @Column(name = "zip_code", length = 8)
    @Visible(table = false, form = true, filter = false)
    private String zipCode;

    @Column(name = "latitude")
    @Visible(table = false, form = true, filter = false)
    private Double latitude;

    @Column(name = "longitude")
    @Visible(table = false, form = true, filter = false)
    private Double longitude;

    @Column(name = "is_default", nullable = false)
    @Visible(table = true, form = true, filter = true)
    private Boolean isDefault = false;

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    /**
     * Retorna endereço completo formatado
     */
    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        sb.append(street).append(", ").append(number);
        
        if (complement != null && !complement.isBlank()) {
            sb.append(" - ").append(complement);
        }
        
        sb.append(" - ").append(neighborhood);
        
        if (city != null) {
            sb.append(", ").append(city.getName()).append(" - ").append(city.getState());
        }
        
        return sb.toString();
    }

    /**
     * Retorna endereço curto (rua + número)
     */
    public String getShortAddress() {
        return street + ", " + number;
    }

    /**
     * Retorna nome da cidade
     */
    public String getCityName() {
        return city != null ? city.getName() : null;
    }

    /**
     * Retorna UF do estado
     */
    public String getState() {
        return city != null ? city.getState() : null;
    }
}
