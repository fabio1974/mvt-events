package com.mvt.mvt_events.jpa;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.Map;

@Entity
@Table(name = "payment_events")
@Data
@EqualsAndHashCode(callSuper = true)
public class PaymentEvent extends BaseEntity {

    // Multi-tenant support: referência ao evento como tenant
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_id")
    private Transfer transfer;

    // Event type
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", length = 50, nullable = false)
    private PaymentEventType eventType;

    // Financial impact
    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(length = 3)
    private String currency = "BRL";

    // Event details
    @Column(columnDefinition = "TEXT")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "event_data")
    private Map<String, Object> eventData;

    // Processing information
    @Column(name = "processed_by")
    private String processedBy; // system, user ID, etc.

    @Column(name = "gateway_reference")
    private String gatewayReference;

    // Enums
    public enum PaymentEventType {
        PAYMENT_RECEIVED("Payment Received"),
        PAYMENT_REFUNDED("Payment Refunded"),
        PAYMENT_PARTIALLY_REFUNDED("Payment Partially Refunded"),
        PAYMENT_CHARGED_BACK("Payment Charged Back"),
        PLATFORM_FEE_CALCULATED("Platform Fee Calculated"),
        TRANSFER_INITIATED("Transfer Initiated"),
        TRANSFER_COMPLETED("Transfer Completed"),
        TRANSFER_FAILED("Transfer Failed"),
        FINANCIAL_ADJUSTMENT("Financial Adjustment");

        private final String displayName;

        PaymentEventType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // Helper methods
    public boolean isPositiveImpact() {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isNegativeImpact() {
        return amount != null && amount.compareTo(BigDecimal.ZERO) < 0;
    }

    public boolean isTransferRelated() {
        return eventType == PaymentEventType.TRANSFER_INITIATED ||
                eventType == PaymentEventType.TRANSFER_COMPLETED ||
                eventType == PaymentEventType.TRANSFER_FAILED;
    }

    public boolean isPaymentRelated() {
        return eventType == PaymentEventType.PAYMENT_RECEIVED ||
                eventType == PaymentEventType.PAYMENT_REFUNDED ||
                eventType == PaymentEventType.PAYMENT_PARTIALLY_REFUNDED ||
                eventType == PaymentEventType.PAYMENT_CHARGED_BACK;
    }
}