package com.mvt.mvt_events.jpa;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * Base entity class with common audit fields and ID.
 * All entities should extend this class to inherit id, createdAt and updatedAt
 * fields.
 * 
 * Uses OffsetDateTime to properly handle timezone information.
 * Timestamps are stored with America/Fortaleza timezone (UTC-3).
 */
@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        // Garante que createdAt é sempre definido com timezone de Fortaleza
        if (createdAt == null) {
            createdAt = OffsetDateTime.now(ZoneId.of("America/Fortaleza"));
        }
        if (updatedAt == null) {
            updatedAt = OffsetDateTime.now(ZoneId.of("America/Fortaleza"));
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now(ZoneId.of("America/Fortaleza"));
    }
}