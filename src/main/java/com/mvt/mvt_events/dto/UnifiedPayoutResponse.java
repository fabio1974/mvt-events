package com.mvt.mvt_events.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO de resposta para UnifiedPayout
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedPayoutResponse {

    private Long id;
    private LocalDateTime createdAt;

    // Beneficiário
    private String beneficiaryId;
    private String beneficiaryName;
    private String beneficiaryType;

    // Período
    private String period;
    private LocalDate startDate;
    private LocalDate endDate;

    // Valores
    private BigDecimal totalAmount;
    private Integer itemCount;

    // Status
    private String status;
    private LocalDateTime paidAt;

    // Pagamento
    private String paymentMethod;
    private String paymentReference;

    private String notes;
}
