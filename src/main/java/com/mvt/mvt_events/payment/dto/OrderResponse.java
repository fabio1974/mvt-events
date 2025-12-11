package com.mvt.mvt_events.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response da criação de order (pedido) no Pagar.me
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    
    private String id;
    private String code;
    private Integer amount;
    private String currency;
    private String status; // "pending", "paid", "canceled", etc
    private String createdAt;
    private String updatedAt;
    private List<Charge> charges;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Charge {
        private String id;
        private String code;
        private Integer amount;
        private String status;
        private String paymentMethod;
        private LastTransaction lastTransaction;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LastTransaction {
        private String id;
        private String transactionType;
        private String status;
        private String qrCode; // PIX QR Code (copia e cola)
        private String qrCodeUrl; // URL da imagem do QR Code
        private String expiresAt;
    }
}
