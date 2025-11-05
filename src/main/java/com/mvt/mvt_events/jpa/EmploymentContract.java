package com.mvt.mvt_events.jpa;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mvt.mvt_events.metadata.DisplayLabel;
import com.mvt.mvt_events.metadata.Visible;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidade que representa um contrato de trabalho entre COURIER e Organization.
 * 
 * Representa a relação empregado-empresa onde um motoboy trabalha para uma
 * organização.
 * 
 * Regras de Negócio:
 * - Um motoboy pode ter múltiplos contratos de trabalho (trabalhar para várias
 * organizações)
 * - Contratos podem ser ativados/desativados
 * - Histórico de contratação é mantido via linkedAt
 */
@Entity
@Table(name = "employment_contracts", uniqueConstraints = @UniqueConstraint(columnNames = { "courier_id",
        "organization_id" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EmploymentContract extends BaseEntity {

    // ============================================================================
    // RELATIONSHIPS
    // ============================================================================

    @NotNull(message = "Motoboy é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "courier_id", nullable = false)
    @JsonIgnore
    @Visible(table = true, form = true, filter = true)
    private User courier;

    @NotNull(message = "Organização é obrigatória")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    @JsonIgnore
    @Visible(table = true, form = true, filter = true)
    private Organization organization;

    // ============================================================================
    // EMPLOYMENT METADATA
    // ============================================================================

    @Column(name = "linked_at", nullable = false)
    @Visible(table = true, form = false, filter = false)
    private LocalDateTime linkedAt = LocalDateTime.now();

    @Column(name = "is_active", nullable = false)
    @Visible(table = true, form = true, filter = true)
    private boolean isActive = true;

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    /**
     * Ativa o contrato de trabalho
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * Desativa o contrato de trabalho
     */
    public void deactivate() {
        this.isActive = false;
    }
}
