package com.mvt.mvt_events.jpa;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Sessão de caixa de um estabelecimento.
 * "Abertura → movimentações → fechamento" de um turno.
 * Apenas uma sessão OPEN por client por vez (índice parcial único).
 */
@Entity
@Table(name = "cash_register_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashRegisterSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    @JsonIgnore
    private User client;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.OPEN;

    @Builder.Default
    @Column(name = "opening_balance", nullable = false, precision = 10, scale = 2)
    private BigDecimal openingBalance = BigDecimal.ZERO;

    @Column(name = "closing_balance_actual", precision = 10, scale = 2)
    private BigDecimal closingBalanceActual;

    @Column(name = "closing_balance_expected", precision = 10, scale = 2)
    private BigDecimal closingBalanceExpected;

    @Column(name = "opened_at", nullable = false)
    private OffsetDateTime openedAt;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opened_by")
    @JsonIgnore
    private User openedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closed_by")
    @JsonIgnore
    private User closedBy;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Builder.Default
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CashRegisterMovement> movements = new ArrayList<>();

    public enum Status { OPEN, CLOSED }

    @PrePersist
    protected void onCreate() {
        if (openedAt == null) openedAt = OffsetDateTime.now();
    }

    @com.fasterxml.jackson.annotation.JsonGetter("clientId")
    public String getClientIdValue() {
        return client != null ? client.getId().toString() : null;
    }

    @com.fasterxml.jackson.annotation.JsonGetter("openedByName")
    public String getOpenedByName() {
        try { return openedBy != null ? openedBy.getName() : null; } catch (Exception e) { return null; }
    }

    @com.fasterxml.jackson.annotation.JsonGetter("closedByName")
    public String getClosedByName() {
        try { return closedBy != null ? closedBy.getName() : null; } catch (Exception e) { return null; }
    }
}
