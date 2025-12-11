package com.mvt.mvt_events.payment.dto;

import com.mvt.mvt_events.jpa.BankAccount.AccountType;
import com.mvt.mvt_events.validation.ValidBankCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * DTO simplificado para atualizar dados bancários
 * 
 * Contém apenas os campos mínimos necessários para o Pagar.me:
 * - Dados bancários (banco, agência, conta, tipo)
 * 
 * Os dados do titular (nome, email, CPF) vêm do User e não podem ser alterados aqui.
 */
public record BankAccountUpdateRequest(
    
    @NotBlank(message = "Código do banco é obrigatório")
    @ValidBankCode
    String bankCode,
    
    @NotBlank(message = "Nome do banco é obrigatório")
    String bankName,
    
    @NotBlank(message = "Agência é obrigatória")
    @Pattern(regexp = "^\\d+$", message = "Agência deve conter apenas números")
    String agency,
    
    String agencyDigit, // Opcional
    
    @NotBlank(message = "Número da conta é obrigatório")
    @Pattern(regexp = "^\\d+$", message = "Número da conta deve conter apenas números")
    String accountNumber,
    
    @NotBlank(message = "Dígito da conta é obrigatório")
    String accountDigit,
    
    @NotNull(message = "Tipo de conta é obrigatório")
    AccountType accountType
) {
}
