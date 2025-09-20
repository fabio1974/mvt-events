package com.mvt.mvt_events.jpa;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "event_financials")
@Data
@EqualsAndHashCode(callSuper = true)
public class EventFinancials extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false, unique = true)
    private Event event;

    // Revenue tracking
    @Column(name = "total_revenue", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalRevenue = BigDecimal.ZERO;

    @Column(name = "platform_fees", precision = 12, scale = 2, nullable = false)
    private BigDecimal platformFees = BigDecimal.ZERO;

    @Column(name = "net_revenue", precision = 12, scale = 2, nullable = false)
    private BigDecimal netRevenue = BigDecimal.ZERO;

    // Transfer tracking
    @Column(name = "pending_transfer_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal pendingTransferAmount = BigDecimal.ZERO;

    @Column(name = "transferred_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal transferredAmount = BigDecimal.ZERO;

    @Column(name = "total_transfer_fees", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalTransferFees = BigDecimal.ZERO;

    // Payment counting
    @Column(name = "total_payments", nullable = false)
    private Integer totalPayments = 0;

    // Transfer scheduling
    @Enumerated(EnumType.STRING)
    @Column(name = "transfer_frequency", length = 20, nullable = false)
    private TransferFrequency transferFrequency = TransferFrequency.WEEKLY;

    @Column(name = "last_transfer_date")
    private LocalDateTime lastTransferDate;

    @Column(name = "next_transfer_date")
    private LocalDateTime nextTransferDate;

    // Helper methods
    public BigDecimal getAvailableForTransfer() {
        return pendingTransferAmount;
    }

    public boolean hasMinimumForTransfer(BigDecimal minimumAmount) {
        return pendingTransferAmount.compareTo(minimumAmount) >= 0;
    }

    public boolean isReadyForTransfer() {
        return nextTransferDate != null &&
                LocalDateTime.now().isAfter(nextTransferDate) &&
                pendingTransferAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    public BigDecimal getTotalNetTransferred() {
        return transferredAmount.subtract(totalTransferFees);
    }
}