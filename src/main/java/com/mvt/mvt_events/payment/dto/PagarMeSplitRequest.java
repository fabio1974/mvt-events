package com.mvt.mvt_events.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuração de split (divisão) de pagamento no Pagar.me
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagarMeSplitRequest {
    
    private Integer amount; // Valor em centavos
    private String type; // "percentage" ou "flat"
    private String recipientId; // ID do recipient
    private SplitOptions options;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SplitOptions {
        private Boolean liable; // Responsável por chargeback
        private Boolean chargeProcessingFee; // Paga taxa de processamento
        private Boolean chargeRemainderFee; // Paga taxa restante
    }
}
