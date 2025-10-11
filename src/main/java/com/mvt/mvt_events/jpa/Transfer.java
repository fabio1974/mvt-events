package com.mvt.mvt_events.jpa;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "transfers")
@Data
@EqualsAndHashCode(callSuper = true)
public class Transfer extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    // Transfer details
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(length = 3, nullable = false)
    private String currency = "BRL";

    @Enumerated(EnumType.STRING)
    @Column(name = "transfer_type", length = 20, nullable = false)
    private TransferType transferType = TransferType.AUTOMATIC;

    // Transfer method and destination
    @Enumerated(EnumType.STRING)
    @Column(name = "transfer_method", length = 50, nullable = false)
    private TransferMethod transferMethod;

    @Column(name = "destination_key")
    private String destinationKey; // PIX key

    @Column(name = "destination_bank", length = 100)
    private String destinationBank;

    @Column(name = "destination_agency", length = 20)
    private String destinationAgency;

    @Column(name = "destination_account", length = 20)
    private String destinationAccount;

    // Status tracking
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private TransferStatus status = TransferStatus.PENDING;

    // Gateway integration
    @Column(name = "gateway_provider", length = 50)
    private String gatewayProvider;

    @Column(name = "gateway_transfer_id")
    private String gatewayTransferId;

    @Column(name = "gateway_fee", precision = 10, scale = 2)
    private BigDecimal gatewayFee = BigDecimal.ZERO;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "gateway_response")
    private Map<String, Object> gatewayResponse;

    // Timestamps
    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt = LocalDateTime.now();

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    // Failure tracking
    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    // Enums
    public enum TransferType {
        AUTOMATIC("Automatic"),
        MANUAL("Manual"),
        SCHEDULED("Scheduled");

        private final String displayName;

        TransferType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum TransferMethod {
        PIX("PIX"),
        BANK_TRANSFER("Bank Transfer"),
        TED("TED"),
        MANUAL("Manual");

        private final String displayName;

        TransferMethod(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum TransferStatus {
        PENDING("Pending"),
        PROCESSING("Processing"),
        COMPLETED("Completed"),
        FAILED("Failed"),
        CANCELLED("Cancelled");

        private final String displayName;

        TransferStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // Helper methods
    public boolean isPending() {
        return status == TransferStatus.PENDING;
    }

    public boolean isCompleted() {
        return status == TransferStatus.COMPLETED;
    }

    public boolean isFailed() {
        return status == TransferStatus.FAILED;
    }

    public boolean canRetry() {
        return isFailed() && retryCount < 3;
    }

    public BigDecimal getNetAmount() {
        return amount.subtract(gatewayFee != null ? gatewayFee : BigDecimal.ZERO);
    }
}