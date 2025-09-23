package com.mvt.mvt_events.jpa;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "registrations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Registration extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // Changed from athlete to user

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "registration_date", nullable = false)
    private LocalDateTime registrationDate = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RegistrationStatus status = RegistrationStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "payment_amount", precision = 10, scale = 2)
    private BigDecimal paymentAmount;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @Column(name = "payment_reference")
    private String paymentReference;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public enum RegistrationStatus {
        PENDING, ACTIVE, CANCELLED, COMPLETED
    }

    public enum PaymentStatus {
        PENDING, PAID, FAILED, REFUNDED
    }

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    public boolean isPaid() {
        return paymentStatus == PaymentStatus.PAID;
    }

    public boolean isActive() {
        return status == RegistrationStatus.ACTIVE;
    }

    public boolean canBeCancelled() {
        return status == RegistrationStatus.PENDING || status == RegistrationStatus.ACTIVE;
    }
}