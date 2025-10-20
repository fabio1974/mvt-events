package com.mvt.mvt_events.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de resposta para ADMProfile
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ADMProfileResponse {

    private Long id;
    private LocalDateTime createdAt;

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

    // Status
    private String status;

    // Parceria
    private Long partnershipId;
    private String partnershipName;
    private String partnershipCity;
}
