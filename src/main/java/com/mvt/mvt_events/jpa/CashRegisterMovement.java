package com.mvt.mvt_events.jpa;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Movimentação manual dentro de uma sessão de caixa.
 * ADDITION = entrada de dinheiro (suprimento)
 * WITHDRAWAL = retirada
 * SANGRIA = retirada para cofre/banco
 */
@Entity
@Table(name = "cash_register_movements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashRegisterMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    @JsonIgnore
    private CashRegisterSession session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Type type;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    @JsonIgnore
    private User createdBy;

    public enum Type { ADDITION, WITHDRAWAL, SANGRIA }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }

    @com.fasterxml.jackson.annotation.JsonGetter("createdByName")
    public String getCreatedByName() {
        try { return createdBy != null ? createdBy.getName() : null; } catch (Exception e) { return null; }
    }

    @com.fasterxml.jackson.annotation.JsonGetter("sessionId")
    public Long getSessionIdValue() {
        return session != null ? session.getId() : null;
    }
}
