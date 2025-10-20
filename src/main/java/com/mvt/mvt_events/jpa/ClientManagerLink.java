package com.mvt.mvt_events.jpa;

import com.mvt.mvt_events.metadata.Visible;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Tabela de associação N:M entre Client e ADM.
 * Permite que um cliente seja gerenciado por múltiplos ADMs
 * e que um ADM gerencie múltiplos clientes.
 * 
 * Usado para multi-tenant onde cada ADM vê apenas seus clientes via
 * Specifications.
 */
@Entity
@Table(name = "client_manager_links", uniqueConstraints = @UniqueConstraint(columnNames = { "client_id", "adm_id" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ClientManagerLink extends BaseEntity {

    // ============================================================================
    // RELATIONSHIPS
    // ============================================================================

    @NotNull(message = "Cliente é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    @Visible(table = true, form = true, filter = true)
    private User client;

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

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    @PrePersist
    private void ensureValidation() {
        if (linkedAt == null) {
            linkedAt = LocalDateTime.now();
        }
    }
}
