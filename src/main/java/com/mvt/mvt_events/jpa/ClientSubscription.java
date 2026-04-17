package com.mvt.mvt_events.jpa;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * Assinatura de um serviço recorrente por um cliente.
 * Cada client pode ter no máximo 1 assinatura ativa por serviço.
 */
@Entity
@Table(name = "client_subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientSubscription {

    private static final ZoneId TZ = ZoneId.of("America/Fortaleza");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    @JsonIgnore
    private User client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private PlatformService service;

    @Column(name = "monthly_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyPrice;

    @Column(name = "billing_due_day", nullable = false)
    private Integer billingDueDay;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        var now = OffsetDateTime.now(TZ);
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (startedAt == null) startedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now(TZ);
    }

    // ============================================================================
    // BILLING HELPERS
    // ============================================================================

    /**
     * Mapa fixo: billing_due_day → dia em que o scheduler gera a fatura.
     *  1 → 25 (mês anterior)
     *  5 → 1
     * 10 → 5
     * 15 → 10
     * 20 → 15
     * 25 → 20
     */
    public int getGenerationDay() {
        return switch (billingDueDay) {
            case 1 -> 25;
            case 5 -> 1;
            case 10 -> 5;
            case 15 -> 10;
            case 20 -> 15;
            case 25 -> 20;
            default -> billingDueDay - 5;
        };
    }

    /**
     * Verifica se hoje é dia de gerar a fatura deste subscription.
     */
    public boolean shouldGenerateToday() {
        int today = LocalDate.now(TZ).getDayOfMonth();
        return today == getGenerationDay();
    }

    /**
     * Próxima data de vencimento a partir de uma data base.
     */
    public LocalDate getNextDueDate(LocalDate from) {
        LocalDate candidate = from.withDayOfMonth(Math.min(billingDueDay, from.lengthOfMonth()));
        if (!candidate.isAfter(from)) {
            LocalDate nextMonth = from.plusMonths(1);
            candidate = nextMonth.withDayOfMonth(Math.min(billingDueDay, nextMonth.lengthOfMonth()));
        }
        return candidate;
    }

    /**
     * Calcula valor pro-rata entre duas datas.
     * Usa dias reais do período / 30 como base.
     */
    public BigDecimal calculateProrata(LocalDate from, LocalDate to) {
        long days = ChronoUnit.DAYS.between(from, to);
        if (days <= 0) return BigDecimal.ZERO;
        BigDecimal dailyRate = monthlyPrice.divide(new BigDecimal("30"), 4, RoundingMode.HALF_UP);
        return dailyRate.multiply(BigDecimal.valueOf(days)).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Cancela a subscription.
     */
    public void cancel() {
        this.cancelledAt = OffsetDateTime.now(TZ);
        this.active = false;
    }
}
