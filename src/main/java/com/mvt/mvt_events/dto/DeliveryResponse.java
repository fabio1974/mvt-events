package com.mvt.mvt_events.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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

    // Veículo usado na entrega (objeto aninhado)
    private VehicleDTO vehicle;

    // Status
    private String status;
    private String preferredVehicleType;
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

    // Pagamentos associados (lista simplificada com apenas IDs)
    private List<PaymentSummary> payments;

    /**
     * Status consolidado do pagamento (derivado de payments)
     * Valores: PAID, PENDING, UNPAID, EXPIRED, FAILED
     */
    private String paymentStatus;

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

    /**
     * DTO simplificado para Vehicle (evitar lazy loading)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehicleDTO {
        private Long id;
        private String type;
        private String plate;
        private String brand;
        private String model;
        private String color;
        private String year;
        private Boolean isActive;
    }

    /**
     * DTO simplificado para Payment (ID, status e dados PIX quando aplicável)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentSummary {
        private Long id;
        private String status;
        private String paymentMethod;
        private BigDecimal amount;
        private String pixQrCode;
        private String pixQrCodeUrl;
        private LocalDateTime expiresAt;
    }
}
