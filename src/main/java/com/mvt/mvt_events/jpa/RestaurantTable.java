package com.mvt.mvt_events.jpa;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "restaurant_tables", uniqueConstraints = @UniqueConstraint(columnNames = {"client_id", "number"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestaurantTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User client;

    @Column(nullable = false)
    private Integer number;

    private Integer seats;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @Builder.Default
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TableStatus status = TableStatus.AVAILABLE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @com.fasterxml.jackson.annotation.JsonGetter("clientId")
    public String getClientIdValue() {
        return client != null ? client.getId().toString() : null;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public enum TableStatus {
        AVAILABLE,    // Livre
        RESERVED,     // Reservada
        OCCUPIED,     // Ocupada (com ou sem pedido)
        UNAVAILABLE   // Interditada / limpeza
    }
}
