package com.mvt.mvt_events.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Response da criação de invoice no Iugu
 * 
 * <p>Contém o QR Code PIX, URL de pagamento e dados da fatura criada.</p>
 * 
 * @see <a href="https://dev.iugu.com/reference/criar-invoice">Documentação Criar Invoice</a>
 */
public record InvoiceResponse(
        
        /**
         * ID da invoice criada
         */
        @JsonProperty("id")
        String id,
        
        /**
         * Código PIX (QR Code em texto)
         */
        @JsonProperty("pix_qrcode")
        String pixQrCode,
        
        /**
         * URL da imagem do QR Code PIX
         */
        @JsonProperty("pix_qrcode_url")
        String pixQrCodeUrl,
        
        /**
         * URL segura da fatura (para pagamento)
         */
        @JsonProperty("secure_url")
        String secureUrl,
        
        /**
         * Status da invoice
         * Valores: "pending", "paid", "canceled", "refunded", "expired"
         */
        @JsonProperty("status")
        String status,
        
        /**
         * Valor total em centavos
         */
        @JsonProperty("total_cents")
        Integer totalCents,
        
        /**
         * Data de vencimento
         */
        @JsonProperty("due_date")
        String dueDate,
        
        /**
         * Email do pagador
         */
        @JsonProperty("email")
        String email,
        
        /**
         * Variáveis customizadas
         */
        @JsonProperty("custom_variables")
        Map<String, String> customVariables
) {
    
    /**
     * Verifica se a invoice está pendente de pagamento
     * 
     * @return true se status = "pending"
     */
    public boolean isPending() {
        return "pending".equalsIgnoreCase(status);
    }
    
    /**
     * Verifica se a invoice foi paga
     * 
     * @return true se status = "paid"
     */
    public boolean isPaid() {
        return "paid".equalsIgnoreCase(status);
    }
    
    /**
     * Verifica se a invoice foi cancelada
     * 
     * @return true se status = "canceled"
     */
    public boolean isCanceled() {
        return "canceled".equalsIgnoreCase(status);
    }
    
    /**
     * Verifica se a invoice expirou
     * 
     * @return true se status = "expired"
     */
    public boolean isExpired() {
        return "expired".equalsIgnoreCase(status);
    }
    
    /**
     * Obtém o delivery_id das variáveis customizadas
     * 
     * @return ID da entrega ou null
     */
    public String getDeliveryId() {
        return customVariables != null ? customVariables.get("delivery_id") : null;
    }
}
