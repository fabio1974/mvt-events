package com.mvt.mvt_events.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request para criação de order (pedido) no Pagar.me
 * 
 * @see <a href="https://docs.pagar.me/reference/criar-pedido">Documentação Criar Order</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {
    
    private List<OrderItem> items;
    private Customer customer;
    private List<Payment> payments;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItem {
        private Integer amount; // Valor em centavos
        private String description;
        private Integer quantity;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Customer {
        private String name;
        private String email;
        private String document; // CPF sem pontuação
        private String type; // "individual" ou "company"
        private String documentType; // "CPF" ou "CNPJ"
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payment {
        private String paymentMethod; // "pix"
        private Pix pix;
        private List<PagarMeSplitRequest> split; // Splits configurados
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Pix {
        private Integer expiresIn; // Tempo de expiração em segundos (ex: 86400 = 24h)
        private String additionalInformation; // Array com informações adicionais
    }
}
