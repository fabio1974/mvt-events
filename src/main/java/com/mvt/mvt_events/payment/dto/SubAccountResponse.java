package com.mvt.mvt_events.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response da criação de subconta no Iugu
 * 
 * <p>Contém o ID da subconta criada e dados de configuração.</p>
 * 
 * @see <a href="https://dev.iugu.com/reference/criar-subconta">Documentação Criar Subconta</a>
 */
public record SubAccountResponse(
        
        /**
         * ID da subconta criada no Iugu
         */
        @JsonProperty("account_id")
        String accountId,
        
        /**
         * Nome da subconta
         */
        @JsonProperty("name")
        String name,
        
        /**
         * Email da subconta
         */
        @JsonProperty("email")
        String email,
        
        /**
         * Se a subconta está ativa
         */
        @JsonProperty("is_active")
        Boolean isActive,
        
        /**
         * Se auto-withdraw está habilitado
         */
        @JsonProperty("auto_withdraw")
        Boolean autoWithdraw,
        
        /**
         * Status da verificação de dados bancários
         * Valores: "pending", "verified", "rejected"
         */
        @JsonProperty("verification_status")
        String verificationStatus
) {
    
    /**
     * Verifica se a subconta pode receber pagamentos
     * 
     * @return true se ativa e com dados bancários verificados
     */
    public boolean canReceivePayments() {
        return Boolean.TRUE.equals(isActive) && 
               "verified".equalsIgnoreCase(verificationStatus);
    }
    
    /**
     * Verifica se a subconta está pendente de verificação
     * 
     * @return true se verificação está pendente
     */
    public boolean isPendingVerification() {
        return "pending".equalsIgnoreCase(verificationStatus);
    }
}
