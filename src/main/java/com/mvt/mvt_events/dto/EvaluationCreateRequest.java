package com.mvt.mvt_events.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DTO para criação de Evaluation
 */
@Data
public class EvaluationCreateRequest {

    @NotNull(message = "Delivery ID é obrigatório")
    private Long deliveryId;

    @NotNull(message = "Rating é obrigatório")
    @Min(value = 1, message = "Rating deve ser no mínimo 1")
    @Max(value = 5, message = "Rating deve ser no máximo 5")
    private Integer rating;

    @NotNull(message = "Tipo de avaliação é obrigatório")
    private String evaluationType; // CLIENT_TO_COURIER, COURIER_TO_CLIENT

    private String comments;
}
