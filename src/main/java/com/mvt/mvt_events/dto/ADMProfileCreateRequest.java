package com.mvt.mvt_events.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO para criação de ADMProfile
 */
@Data
public class ADMProfileCreateRequest {

    @NotNull(message = "User ID é obrigatório")
    private String userId; // UUID como String

    @NotBlank(message = "Região é obrigatória")
    private String region;

    @NotNull(message = "Percentual de comissão é obrigatório")
    @DecimalMin(value = "0.0", message = "Comissão deve ser maior ou igual a 0")
    @DecimalMax(value = "100.0", message = "Comissão deve ser menor ou igual a 100")
    private BigDecimal commissionPercentage;

    private Long partnershipId;
}
