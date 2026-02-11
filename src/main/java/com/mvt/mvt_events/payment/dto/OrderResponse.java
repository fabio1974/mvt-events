package com.mvt.mvt_events.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    private String status; // "pending", "paid", "canceled", "failed", etc
    
    @JsonProperty("created_at")
    private String createdAt;
    
    @JsonProperty("updated_at")
    private String updatedAt;
    
    private Boolean closed;
    private List<Charge> charges;
    private Object customer; // Pode ser mapeado completo se necessário
    private List<Object> items; // Pode ser mapeado completo se necessário
    
    // Campos para auditoria (não vêm do JSON do Pagar.me)
    private transient String requestPayload; // Request enviado ao Pagar.me (para salvar no DB)
    private transient String responsePayload; // Response recebido do Pagar.me (para salvar no DB)
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Charge {
        private String id;
        private String code;
        private Integer amount;
        private String status;
        
        @JsonProperty("payment_method")
        private String paymentMethod;
        
        @JsonProperty("last_transaction")
        private LastTransaction lastTransaction;
        
        @JsonProperty("created_at")
        private String createdAt;
        
        @JsonProperty("updated_at")
        private String updatedAt;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LastTransaction {
        private String id;
        
        @JsonProperty("transaction_type")
        private String transactionType;
        
        private String status;
        private Boolean success;
        
        @JsonProperty("qr_code")
        private String qrCode; // PIX QR Code (copia e cola)
        
        @JsonProperty("qr_code_url")
        private String qrCodeUrl; // URL da imagem do QR Code
        
        @JsonProperty("expires_at")
        private String expiresAt;
        
        @JsonProperty("gateway_response")
        private GatewayResponse gatewayResponse; // Gateway response com códigos de erro
        
        @JsonProperty("acquirer_message")
        private String acquirerMessage; // Mensagem da adquirente/bandeira
        
        @JsonProperty("acquirer_name")
        private String acquirerName; // Nome da adquirente
        
        @JsonProperty("acquirer_return_code")
        private String acquirerReturnCode; // Código de retorno da adquirente
        
        @JsonProperty("antifraud_response")
        private Object antifraudResponse; // Resposta completa do antifraude
        
        @JsonProperty("created_at")
        private String createdAt;
        
        @JsonProperty("updated_at")
        private String updatedAt;
        
        private Integer amount;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GatewayResponse {
        private String code; // Código HTTP (ex: "400", "200")
        private List<ErrorDetail> errors; // Lista de erros, se houver
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorDetail {
        private String message; // Mensagem de erro (ex: "At least one customer phone is required.")
    }
}
