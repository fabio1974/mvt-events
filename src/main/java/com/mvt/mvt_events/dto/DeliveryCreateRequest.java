package com.mvt.mvt_events.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO para criação de Delivery.
 * Suporta single-stop (campos flat legados) e multi-stop (lista stops).
 * Se stops estiver presente e não-vazio, os campos flat de destino são ignorados.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryCreateRequest {

    @NotNull(message = "Cliente é obrigatório")
    @Valid
    private EntityReference client;

    @NotBlank(message = "Endereço de origem é obrigatório")
    private String fromAddress;

    @NotNull(message = "Latitude de origem é obrigatória")
    private Double fromLatitude;

    @NotNull(message = "Longitude de origem é obrigatória")
    private Double fromLongitude;

    private String fromCity;
    private String fromState;
    private String fromZipCode;

    // Campos flat de destino (legado, usados quando stops está vazio/null)
    private String toAddress;
    private Double toLatitude;
    private Double toLongitude;
    private String toCity;
    private String toState;
    private String toZipCode;
    private String recipientName;
    private String recipientPhone;

    @DecimalMin(value = "0.0", message = "Valor total não pode ser negativo")
    private BigDecimal totalAmount;

    @DecimalMin(value = "0.0", message = "Distância não pode ser negativa")
    private BigDecimal distanceKm;

    private String itemDescription;
    private EntityReference partnership;
    private String scheduledPickupAt; // ISO DateTime string

    /**
     * Preferência de veículo: MOTORCYCLE, CAR ou ANY (padrão: ANY)
     */
    private String preferredVehicleType;

    /**
     * Lista de paradas (destinos) para entregas multi-stop.
     * Se presente, sobrescreve os campos flat de destino.
     * Apenas CLIENT pode enviar mais de 1 stop.
     */
    @Valid
    private List<StopRequest> stops;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StopRequest {
        @NotBlank(message = "Endereço da parada é obrigatório")
        private String address;

        @NotNull(message = "Latitude da parada é obrigatória")
        private Double latitude;

        @NotNull(message = "Longitude da parada é obrigatória")
        private Double longitude;

        private String recipientName;
        private String recipientPhone;
        private String itemDescription;
    }

    /**
     * DTO interno para referenciar entidades por ID
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EntityReference {
        @NotBlank(message = "ID é obrigatório")
        private String id;
    }

    /**
     * Rota planejada calculada no wizard (lista de pares [latitude, longitude]).
     * Persistida como PostGIS LINESTRING — elimina chamadas ao Google durante corridas ativas.
     */
    private List<List<Double>> plannedRouteCoordinates;

    /**
     * Retorna true se a request tem paradas explícitas (multi-stop).
     */
    public boolean hasStops() {
        return stops != null && !stops.isEmpty();
    }
}
