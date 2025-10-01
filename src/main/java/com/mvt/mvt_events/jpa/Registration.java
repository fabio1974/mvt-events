package com.mvt.mvt_events.jpa;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
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

    @NotNull(message = "User is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // Changed from athlete to user

    @NotNull(message = "Event is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "registration_date", nullable = false)
    private LocalDateTime registrationDate = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RegistrationStatus status = RegistrationStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    // Relacionamento com Payment
    @OneToMany(mappedBy = "registration", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private java.util.List<Payment> payments = new java.util.ArrayList<>();

    public enum RegistrationStatus {
        PENDING, ACTIVE, CANCELLED, COMPLETED
    }

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    public boolean isPaid() {
        return payments.stream()
                .anyMatch(payment -> payment.getStatus() == Payment.PaymentStatus.COMPLETED);
    }

    public boolean isActive() {
        return status == RegistrationStatus.ACTIVE;
    }

    public boolean canBeCancelled() {
        return status == RegistrationStatus.PENDING || status == RegistrationStatus.ACTIVE;
    }

    public Payment getLatestPayment() {
        return payments.stream()
                .max(java.util.Comparator.comparing(Payment::getCreatedAt))
                .orElse(null);
    }
}