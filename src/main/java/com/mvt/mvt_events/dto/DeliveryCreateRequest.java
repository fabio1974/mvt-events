package com.mvt.mvt_events.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para criação de Delivery
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

    @NotBlank(message = "Endereço de destino é obrigatório")
    private String toAddress;

    @NotNull(message = "Latitude de destino é obrigatória")
    private Double toLatitude;

    @NotNull(message = "Longitude de destino é obrigatória")
    private Double toLongitude;

    private String toCity;
    private String toState;
    private String toZipCode;

    @NotBlank(message = "Nome do destinatário é obrigatório")
    private String recipientName;

    @NotBlank(message = "Telefone do destinatário é obrigatório")
    private String recipientPhone;

    @NotNull(message = "Valor total é obrigatório")
    @DecimalMin(value = "0.0", inclusive = false, message = "Valor deve ser maior que zero")
    private BigDecimal totalAmount;

    private String itemDescription;
    private EntityReference partnership;
    private String scheduledPickupAt; // ISO DateTime string

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
}
