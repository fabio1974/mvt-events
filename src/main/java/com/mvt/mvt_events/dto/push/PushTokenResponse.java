package com.mvt.mvt_events.dto.push;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para resposta de operações com tokens push
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PushTokenResponse {
    private boolean success;
    private String message;
    private Object data; // Para dados adicionais quando necessário
}