package com.mvt.mvt_events.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO para atribuir delivery a courier
 */
@Data
public class DeliveryAssignRequest {

    @NotNull(message = "Courier ID é obrigatório")
    private String courierId; // UUID como String
}
