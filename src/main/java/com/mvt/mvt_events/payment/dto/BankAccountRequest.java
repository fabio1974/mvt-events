package com.mvt.mvt_events.payment.dto;

import com.mvt.mvt_events.jpa.BankAccount.AccountType;
import com.mvt.mvt_events.validation.ValidBankCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * DTO para cadastrar/atualizar dados bancários
 * 
 * <p>Usado em:
 * <ul>
 *   <li>POST /api/motoboy/bank-data - Cadastrar dados bancários</li>
 *   <li>PUT /api/motoboy/bank-data - Atualizar dados bancários</li>
 * </ul>
 */
public record BankAccountRequest(
    
    @NotBlank(message = "Código do banco é obrigatório")
    @ValidBankCode
    String bankCode,
    
    @NotBlank(message = "Nome do banco é obrigatório")
    String bankName,
    
    @NotBlank(message = "Agência é obrigatória")
    @Pattern(regexp = "^\\d+$", message = "Agência deve conter apenas números")
    String agency,
    
    @NotBlank(message = "Número da conta é obrigatório")
    @Pattern(regexp = "^\\d+-\\d$", message = "Número da conta deve estar no formato 12345678-9")
    String accountNumber,
    
    @NotNull(message = "Tipo de conta é obrigatório")
    AccountType accountType
) {
    
    /**
     * Valida se os dados estão completos e corretos
     */
    public void validate() {
        if (bankCode == null || bankCode.isBlank()) {
            throw new IllegalArgumentException("Código do banco é obrigatório");
        }
        
        if (!bankCode.matches("^\\d{3}$")) {
            throw new IllegalArgumentException("Código do banco deve ter 3 dígitos");
        }
        
        if (bankName == null || bankName.isBlank()) {
            throw new IllegalArgumentException("Nome do banco é obrigatório");
        }
        
        if (agency == null || agency.isBlank()) {
            throw new IllegalArgumentException("Agência é obrigatória");
        }
        
        if (!agency.matches("^\\d+$")) {
            throw new IllegalArgumentException("Agência deve conter apenas números");
        }
        
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("Número da conta é obrigatório");
        }
        
        if (!accountNumber.matches("^\\d+-\\d$")) {
            throw new IllegalArgumentException("Número da conta deve estar no formato 12345678-9");
        }
        
        if (accountType == null) {
            throw new IllegalArgumentException("Tipo de conta é obrigatório");
        }
    }
}
