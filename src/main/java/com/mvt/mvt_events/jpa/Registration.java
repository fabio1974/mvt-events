package com.mvt.mvt_events.jpa;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "registrations", uniqueConstraints = @UniqueConstraint(columnNames = { "tenant_id", "event_id",
        "athlete_id" }))
@Data
@EqualsAndHashCode(callSuper = true)
public class Registration extends BaseEntity {

    // Multi-tenant support: reference to event as tenant
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "athlete_id", nullable = false)
    private Athlete athlete;

    @Column(length = 100)
    private String category;

    @Column(name = "team_name")
    private String teamName;

    @Column(name = "bib_number")
    private Integer bibNumber;

    @Column(name = "registration_date", updatable = false)
    private LocalDateTime registrationDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "amount_paid", precision = 10, scale = 2)
    private BigDecimal amountPaid;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "special_needs", columnDefinition = "TEXT")
    private String specialNeeds;

    @Column(name = "t_shirt_size", length = 10)
    private String tShirtSize;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RegistrationStatus status = RegistrationStatus.ACTIVE;

    // Enums
    public enum PaymentStatus {
        PENDING("Pending"),
        PAID("Paid"),
        CANCELLED("Cancelled"),
        REFUNDED("Refunded");

        private final String displayName;

        PaymentStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum RegistrationStatus {
        ACTIVE("Active"),
        CANCELLED("Cancelled"),
        DNS("Did Not Start"),
        DNF("Did Not Finish");

        private final String displayName;

        RegistrationStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}