package com.mvt.mvt_events.jpa;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mvt.mvt_events.metadata.Visible;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * Vínculo direto N:N entre CLIENT (estabelecimento) e WAITER (garçom).
 * O CLIENT gerencia seus garçons diretamente.
 */
@Entity
@Table(name = "client_waiters", uniqueConstraints = @UniqueConstraint(columnNames = {"client_id", "waiter_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ClientWaiter extends BaseEntity {

    @NotNull(message = "Cliente é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    @JsonIgnore
    @Visible(table = true, form = true, filter = true)
    private User client;

    @NotNull(message = "Garçom é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "waiter_id", nullable = false)
    @JsonIgnore
    @Visible(table = true, form = true, filter = true)
    private User waiter;

    @Column(name = "active", nullable = false)
    @Visible(table = true, form = true, filter = true)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now(ZoneId.of("America/Fortaleza"));
}
