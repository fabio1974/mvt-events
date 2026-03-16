package com.mvt.mvt_events.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * DTO de resposta para ADMProfile
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ADMProfileResponse {

    private Long id;
    private OffsetDateTime createdAt;

    // User
    private String userId;
    private String userName;
    private String userEmail;
    private String userPhone;

    // Região (TENANT)
    private String region;

    // Comissão
    private BigDecimal commissionPercentage;
    private BigDecimal totalCommission;

    // Métricas
    private Integer totalDeliveriesManaged;
    private Integer activeDeliveriesCount;

    // Status
    private String status;

    // Parceria
    private Long partnershipId;
    private String partnershipName;
    private String partnershipCity;
}
