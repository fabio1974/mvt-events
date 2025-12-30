package com.mvt.mvt_events.jpa;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mvt.mvt_events.metadata.DisplayLabel;
import com.mvt.mvt_events.metadata.Visible;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

/**
 * Entidade que representa um contrato entre um CLIENT e uma Organization.
 * 
 * Regras de Negócio:
 * - Um cliente pode ter múltiplos contratos
 * - Apenas 1 contrato pode ser titular (is_primary = true)
 * - Contratos têm status: ACTIVE, SUSPENDED, CANCELLED
 * - Contratos têm data de início e fim (opcional)
 */
@Entity
@Table(name = "client_contracts", uniqueConstraints = @UniqueConstraint(columnNames = { "client_id",
        "organization_id" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ClientContract extends BaseEntity {

    // ============================================================================
    // RELATIONSHIPS
    // ============================================================================

    @NotNull(message = "Cliente é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    @JsonIgnore
    @Visible(table = true, form = true, filter = true)
    private User client;

    @NotNull(message = "Organização é obrigatória")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    @JsonIgnore
    @Visible(table = true, form = true, filter = true)
    private Organization organization;

    // ============================================================================
    // CONTRACT METADATA
    // ============================================================================

    @Column(name = "is_primary", nullable = false)
    @Visible(table = true, form = true, filter = true)
    private boolean isPrimary = false;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Visible(table = true, form = true, filter = true)
    private ContractStatus status = ContractStatus.ACTIVE;

    // ============================================================================
    // DATES
    // ============================================================================

    @NotNull(message = "Data de início é obrigatória")
    @Column(name = "start_date", nullable = false)
    @Visible(table = true, form = true, filter = true)
    private LocalDate startDate;

    @Column(name = "end_date")
    @Visible(table = true, form = true, filter = true)
    private LocalDate endDate;

    // ============================================================================
    // ENUMS
    // ============================================================================

    public enum ContractStatus {
        ACTIVE, // Ativo
        SUSPENDED, // Suspenso
        CANCELLED // Cancelado
    }

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    /**
     * Verifica se o contrato está ativo
     */
    public boolean isActive() {
        return status == ContractStatus.ACTIVE;
    }

    /**
     * Verifica se o contrato está vigente (ativo e dentro do período)
     */
    public boolean isValid() {
        LocalDate today = LocalDate.now();
        return isActive()
                && !startDate.isAfter(today)
                && (endDate == null || !endDate.isBefore(today));
    }

    /**
     * Verifica se o contrato está expirado
     */
    public boolean isExpired() {
        return endDate != null && endDate.isBefore(LocalDate.now());
    }

    /**
     * Verifica se o contrato ainda não começou
     */
    public boolean isPending() {
        return startDate.isAfter(LocalDate.now());
    }
}
