package com.mvt.mvt_events.jpa;

import com.mvt.mvt_events.metadata.Visible;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Tabela de associação N:M entre Courier e ADM.
 * Permite que um motoboy trabalhe com múltiplos gerentes (regiões).
 * 
 * Regra: Cada courier pode ter apenas 1 ADM principal ativo (is_primary = true
 * AND is_active = true)
 */
@Entity
@Table(name = "courier_adm_links", uniqueConstraints = @UniqueConstraint(columnNames = { "courier_id", "adm_id" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CourierADMLink extends BaseEntity {

    // ============================================================================
    // RELATIONSHIPS
    // ============================================================================

    @NotNull(message = "Motoboy é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "courier_id", nullable = false)
    @Visible(table = true, form = true, filter = true)
    private User courier;

    @NotNull(message = "Gerente é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adm_id", nullable = false)
    @Visible(table = true, form = true, filter = true)
    private User adm;

    // ============================================================================
    // LINK METADATA
    // ============================================================================

    @Column(name = "linked_at", nullable = false)
    @Visible(table = true, form = false, filter = false)
    private LocalDateTime linkedAt = LocalDateTime.now();

    @Column(name = "is_primary", nullable = false)
    @Visible(table = true, form = true, filter = true)
    private Boolean isPrimary = false;

    @Column(name = "is_active", nullable = false)
    @Visible(table = true, form = true, filter = true)
    private Boolean isActive = true;

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    @PrePersist
    @PreUpdate
    private void ensureValidation() {
        // Garantir valores não-nulos
        if (linkedAt == null) {
            linkedAt = LocalDateTime.now();
        }
        if (isPrimary == null) {
            isPrimary = false;
        }
        if (isActive == null) {
            isActive = true;
        }
    }
}
