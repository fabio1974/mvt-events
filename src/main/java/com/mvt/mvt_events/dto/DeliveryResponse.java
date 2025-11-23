package com.mvt.mvt_events.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de resposta para Delivery
 * IMPORTANTE: Usa objetos aninhados para relacionamentos (não flat)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryResponse {

    private Long id;
    private LocalDateTime createdAt;

    // Cliente (objeto aninhado)
    private UserDTO client;

    // Courier (objeto aninhado)
    private UserDTO courier;

    // Organizador (dono da organização responsável pela entrega)
    private UserDTO organizer;

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

    // Item
    private String itemDescription;

    // Valores
    private BigDecimal totalAmount;
    private BigDecimal shippingFee;
    private BigDecimal distanceKm;

    // Status
    private String status;
    private LocalDateTime scheduledPickupAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime pickedUpAt;
    private LocalDateTime inTransitAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private String cancellationReason;

    // Parceria (objeto aninhado)
    private PartnershipDTO partnership;

    // Avaliação
    private Integer rating;
    private Boolean hasEvaluation;

    private String notes;

    /**
     * DTO simplificado para User (evitar lazy loading)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDTO {
        private String id;
        private String name;
        private String phone;
        private Double gpsLatitude;
        private Double gpsLongitude;
    }

    /**
     * DTO simplificado para Partnership (evitar lazy loading)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartnershipDTO {
        private Long id;
        private String name;
    }

    /**
     * DTO simplificado para Organization (substitui ADM)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrganizationDTO {
        private Long id;
        private String name;
    }
}
