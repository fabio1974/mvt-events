package com.mvt.mvt_events.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload de webhook recebido do Pagar.me
 * 
 * @see <a href="https://docs.pagar.me/reference/webhooks">Documentação Webhooks</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagarMeWebhookEvent {
    
    private String id;
    private String type; // "order.paid", "order.payment_failed", etc
    private String createdAt;
    private WebhookData data;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WebhookData {
        private String id;
        private String code;
        private String status;
    }
}
