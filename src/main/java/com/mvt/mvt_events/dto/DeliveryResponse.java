package com.mvt.mvt_events.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de resposta para Delivery
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryResponse {

    private Long id;
    private LocalDateTime createdAt;

    // Cliente
    private String clientId;
    private String clientName;

    // Courier
    private String courierId;
    private String courierName;
    private String courierPhone;

    // ADM (Tenant)
    private String admId;
    private String admName;
    private String admRegion;

    // Origem
    private String fromAddress;
    private Double fromLatitude;
    private Double fromLongitude;
    private String fromCity;

    // Destino
    private String toAddress;
    private Double toLatitude;
    private Double toLongitude;
    private String toCity;

    // Destinatário
    private String recipientName;
    private String recipientPhone;

    // Valores
    private BigDecimal totalAmount;

    // Status
    private String status;
    private LocalDateTime scheduledPickupAt;
    private LocalDateTime pickedUpAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;

    // Parceria
    private Long partnershipId;
    private String partnershipName;

    // Avaliação
    private Integer rating;
    private Boolean hasEvaluation;

    private String notes;
}
