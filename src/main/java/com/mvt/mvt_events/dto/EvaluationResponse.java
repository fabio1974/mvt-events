package com.mvt.mvt_events.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de resposta para Evaluation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationResponse {

    private Long id;
    private LocalDateTime createdAt;

    // Delivery
    private Long deliveryId;
    private String deliveryFromAddress;
    private String deliveryToAddress;

    // Avaliador
    private String evaluatorId;
    private String evaluatorName;

    // Avaliação
    private Integer rating;
    private String evaluationType;
    private String comments;

    // Courier (se CLIENT_TO_COURIER)
    private String courierId;
    private String courierName;
}
