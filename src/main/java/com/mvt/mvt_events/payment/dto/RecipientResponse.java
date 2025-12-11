package com.mvt.mvt_events.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Response da criação de recipient (subconta) no Pagar.me
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipientResponse {
    private String id;
    private String name;
    private String email;
    private String code;
    private String document;
    private String type; // "individual" ou "company"
    private String paymentMode; // "bank_transfer"
    private String status; // "active", "inactive", etc
    private String createdAt;
    private String updatedAt;
    private DefaultBankAccount defaultBankAccount;
    private TransferSettings transferSettings;
    private Map<String, String> metadata;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DefaultBankAccount {
        private String id;
        private String holderName;
        private String holderType;
        private String holderDocument;
        private String bank;
        private String branchNumber;
        private String branchCheckDigit;
        private String accountNumber;
        private String accountCheckDigit;
        private String type; // "checking" ou "savings"
        private String status;
        private String createdAt;
        private String updatedAt;
        
        // Alias para compatibilidade com API Pagar.me
        public String getBankCode() {
            return bank;
        }
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransferSettings {
        private Boolean transferEnabled;
        private String transferInterval; // "Daily", "Weekly", "Monthly"
        private Integer transferDay;
    }
}
