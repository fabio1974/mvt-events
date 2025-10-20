package com.mvt.mvt_events.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO para criação de CourierProfile
 */
@Data
public class CourierProfileCreateRequest {

    @NotNull(message = "User ID é obrigatório")
    private String userId; // UUID como String

    @NotNull(message = "Tipo de veículo é obrigatório")
    private String vehicleType; // MOTORCYCLE, CAR, BICYCLE, ON_FOOT

    @NotBlank(message = "Placa do veículo é obrigatória")
    private String vehiclePlate;

    private String vehicleModel;
    private Integer vehicleYear;
}
