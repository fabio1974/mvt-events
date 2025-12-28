package com.mvt.mvt_events.payment.dto;

import com.mvt.mvt_events.jpa.BankAccount;
import com.mvt.mvt_events.jpa.BankAccount.AccountType;
import com.mvt.mvt_events.jpa.BankAccount.BankAccountStatus;

import java.time.LocalDateTime;

/**
 * DTO de resposta com dados bancários
 * 
 * <p>Usado em:
 * <ul>
 *   <li>GET /api/motoboy/bank-data - Consultar dados bancários</li>
 *   <li>POST /api/motoboy/bank-data - Resposta após cadastro</li>
 *   <li>PUT /api/motoboy/bank-data - Resposta após atualização</li>
 * </ul>
 */
public record BankAccountResponse(
    Long id,
    String bankCode,
    String bankName,
    String agency,
    String agencyDigit,
    String accountNumber,
    String accountDigit,
    String accountNumberMasked,
    AccountType accountType,
    Boolean automaticTransfer,
    BankAccountStatus status,
    String statusDisplayName,
    LocalDateTime createdAt,
    LocalDateTime validatedAt,
    Boolean canReceivePayments
) {
    
    /**
     * Converte BankAccount para BankAccountResponse
     */
    public static BankAccountResponse from(BankAccount bankAccount) {
        return new BankAccountResponse(
            bankAccount.getId(),
            bankAccount.getBankCode(),
            bankAccount.getBankName(),
            bankAccount.getAgency(),
            bankAccount.getAgencyDigit(),
            bankAccount.getAccountNumber(),
            bankAccount.getAccountDigit(),
            bankAccount.getAccountNumberMasked(),
            bankAccount.getAccountType(),
            bankAccount.getAutomaticTransfer(),
            bankAccount.getStatus(),
            bankAccount.getStatus().getDisplayName(),
            bankAccount.getCreatedAt(),
            bankAccount.getValidatedAt(),
            bankAccount.isActive()
        );
    }
    
    /**
     * Converte BankAccount para BankAccountResponse (mascarando dados sensíveis)
     * 
     * @param maskSensitiveData Se true, mascara número da conta completo
     */
    public static BankAccountResponse from(BankAccount bankAccount, boolean maskSensitiveData) {
        if (maskSensitiveData) {
            return new BankAccountResponse(
                bankAccount.getId(),
                bankAccount.getBankCode(),
                bankAccount.getBankName(),
                bankAccount.getAgency(),
                bankAccount.getAgencyDigit(),
                null, // Não retorna número completo
                null, // Não retorna dígito da conta
                bankAccount.getAccountNumberMasked(),
                bankAccount.getAccountType(),
                bankAccount.getAutomaticTransfer(),
                bankAccount.getStatus(),
                bankAccount.getStatus().getDisplayName(),
                bankAccount.getCreatedAt(),
                bankAccount.getValidatedAt(),
                bankAccount.isActive()
            );
        }
        
        return from(bankAccount);
    }
}
