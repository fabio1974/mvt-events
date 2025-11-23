package com.mvt.mvt_events.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para atualização de Delivery
 */
@Data
public class DeliveryUpdateRequest {

    @NotBlank(message = "Endereço de origem é obrigatório")
    private String fromAddress;

    @NotNull(message = "Latitude de origem é obrigatória")
    private Double fromLatitude;

    @NotNull(message = "Longitude de origem é obrigatória")
    private Double fromLongitude;

    @NotBlank(message = "Endereço de destino é obrigatório")
    private String toAddress;

    @NotNull(message = "Latitude de destino é obrigatória")
    private Double toLatitude;

    @NotNull(message = "Longitude de destino é obrigatória")
    private Double toLongitude;

    @NotBlank(message = "Nome do destinatário é obrigatório")
    private String recipientName;

    @NotBlank(message = "Telefone do destinatário é obrigatório")
    private String recipientPhone;

    @NotBlank(message = "Descrição do item é obrigatória")
    private String itemDescription;

    @NotNull(message = "Valor total é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor total deve ser maior que zero")
    private BigDecimal totalAmount;

    @DecimalMin(value = "0.00", message = "Taxa de entrega deve ser maior ou igual a zero")
    private BigDecimal shippingFee;

    private LocalDateTime scheduledPickupAt;
}
