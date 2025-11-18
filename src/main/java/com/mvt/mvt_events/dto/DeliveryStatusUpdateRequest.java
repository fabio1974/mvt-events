package com.mvt.mvt_events.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Request para atualizar o status de uma delivery
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryStatusUpdateRequest {

    @NotBlank(message = "Status é obrigatório")
    private String status;

    private String reason; // Opcional, usado principalmente para cancelamento
}
