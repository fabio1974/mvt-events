package com.mvt.mvt_events.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * DTO público para rastreamento de entrega via link (sem autenticação).
 * Expõe apenas dados necessários para o destinatário acompanhar a entrega.
 * NÃO expõe: telefones, emails, IDs internos, valores financeiros.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackingResponse {

    private Long deliveryId;
    private String status;

    // Endereços
    private String fromAddress;
    private String toAddress;

    // Coordenadas
    private Double fromLatitude;
    private Double fromLongitude;
    private Double toLatitude;
    private Double toLongitude;

    // Destinatário (apenas nome)
    private String recipientName;

    // Courier (dados limitados)
    private String courierFirstName;
    private Double courierLatitude;
    private Double courierLongitude;
    private String vehicleType;
    private String vehicleDescription; // ex: "Honda CG 160 - ***A23"

    // Timestamps
    private OffsetDateTime acceptedAt;
    private OffsetDateTime inTransitAt;
    private OffsetDateTime completedAt;
    private OffsetDateTime cancelledAt;

    // Paradas (dados limitados)
    private List<TrackingStopDTO> stops;

    // Rota planejada (array de coordenadas para desenhar no mapa)
    private List<double[]> plannedRoute;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrackingStopDTO {
        private Integer stopOrder;
        private String address;
        private Double latitude;
        private Double longitude;
        private String recipientName;
        private String status;
        private OffsetDateTime completedAt;
    }
}
