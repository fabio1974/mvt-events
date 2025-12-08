package com.mvt.mvt_events.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Evento de webhook recebido do Iugu
 * 
 * <p>O Iugu envia webhooks para notificar sobre eventos importantes:</p>
 * <ul>
 *   <li><strong>invoice.paid:</strong> Fatura foi paga</li>
 *   <li><strong>invoice.refunded:</strong> Fatura foi reembolsada</li>
 *   <li><strong>invoice.canceled:</strong> Fatura foi cancelada</li>
 *   <li><strong>invoice.expired:</strong> Fatura expirou</li>
 *   <li><strong>withdrawal.completed:</strong> Transferência bancária concluída</li>
 * </ul>
 * 
 * @see <a href="https://dev.iugu.com/reference/webhooks">Documentação Webhooks Iugu</a>
 */
public record WebhookEvent(
        
        /**
         * Tipo do evento
         * Ex: "invoice.paid", "withdrawal.completed"
         */
        @JsonProperty("event")
        String event,
        
        /**
         * Dados do evento (varia conforme o tipo)
         */
        @JsonProperty("data")
        Map<String, Object> data
) {
    
    /**
     * Obtém o ID da invoice do evento
     * 
     * @return ID da invoice ou null
     */
    @SuppressWarnings("unchecked")
    public String getInvoiceId() {
        if (data == null) return null;
        
        // Tenta primeiro como campo direto
        Object id = data.get("id");
        if (id != null) return id.toString();
        
        // Tenta dentro de "invoice"
        Object invoice = data.get("invoice");
        if (invoice instanceof Map) {
            Object invoiceId = ((Map<String, Object>) invoice).get("id");
            return invoiceId != null ? invoiceId.toString() : null;
        }
        
        return null;
    }
    
    /**
     * Obtém o status da invoice
     * 
     * @return Status ou null
     */
    @SuppressWarnings("unchecked")
    public String getInvoiceStatus() {
        if (data == null) return null;
        
        Object status = data.get("status");
        if (status != null) return status.toString();
        
        Object invoice = data.get("invoice");
        if (invoice instanceof Map) {
            Object invoiceStatus = ((Map<String, Object>) invoice).get("status");
            return invoiceStatus != null ? invoiceStatus.toString() : null;
        }
        
        return null;
    }
    
    /**
     * Obtém o ID da conta que recebeu a transferência
     * (disponível apenas em eventos withdrawal.*)
     * 
     * @return ID da conta ou null
     */
    public String getAccountId() {
        if (data == null) return null;
        Object accountId = data.get("account_id");
        return accountId != null ? accountId.toString() : null;
    }
    
    /**
     * Verifica se é um evento de pagamento confirmado
     * 
     * @return true se evento = "invoice.paid"
     */
    public boolean isPaymentConfirmed() {
        return "invoice.paid".equalsIgnoreCase(event);
    }
    
    /**
     * Verifica se é um evento de transferência concluída
     * 
     * @return true se evento = "withdrawal.completed"
     */
    public boolean isWithdrawalCompleted() {
        return "withdrawal.completed".equalsIgnoreCase(event);
    }
    
    /**
     * Verifica se é um evento de reembolso
     * 
     * @return true se evento = "invoice.refunded"
     */
    public boolean isRefunded() {
        return "invoice.refunded".equalsIgnoreCase(event);
    }
    
    /**
     * Verifica se é um evento de cancelamento
     * 
     * @return true se evento = "invoice.canceled"
     */
    public boolean isCanceled() {
        return "invoice.canceled".equalsIgnoreCase(event);
    }
    
    /**
     * Verifica se é um evento de expiração
     * 
     * @return true se evento = "invoice.expired"
     */
    public boolean isExpired() {
        return "invoice.expired".equalsIgnoreCase(event);
    }
}
