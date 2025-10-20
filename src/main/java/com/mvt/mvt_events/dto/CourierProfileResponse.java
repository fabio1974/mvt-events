package com.mvt.mvt_events.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de resposta para CourierProfile
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourierProfileResponse {

    private Long id;
    private LocalDateTime createdAt;

    // User
    private String userId;
    private String userName;
    private String userEmail;
    private String userPhone;

    // Veículo
    private String vehicleType;
    private String vehiclePlate;
    private String vehicleModel;
    private Integer vehicleYear;

    // Métricas
    private BigDecimal rating;
    private Integer totalDeliveries;
    private Integer completedDeliveries;
    private Integer cancelledDeliveries;

    // Status
    private String status;
    private LocalDateTime lastLocationUpdate;

    // ADM Primário
    private String primaryAdmId;
    private String primaryAdmName;
    private String primaryAdmRegion;
}
