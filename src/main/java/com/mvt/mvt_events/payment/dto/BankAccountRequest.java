package com.mvt.mvt_events.payment.dto;

import com.mvt.mvt_events.jpa.BankAccount.AccountType;
import com.mvt.mvt_events.validation.ValidBankCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

/**
 * DTO simplificado para cadastrar dados bancários e criar recipient no Pagar.me
 * 
 * Contém apenas os campos mínimos necessários:
 * - user (obrigatório) - Objeto com ID do usuário dono da conta
 * - Dados bancários (obrigatórios)
 * - Campos KYC opcionais (motherName, monthlyIncome, professionalOccupation)
 * 
 * IMPORTANTE: Os demais dados (nome, CPF, email, telefone, endereço, data de nascimento)
 * são obtidos automaticamente da entidade User e Address relacionadas.
 */
public record BankAccountRequest(
    
    // ==================== USER (OBRIGATÓRIO) ====================
    @NotNull(message = "Usuário é obrigatório")
    UserRef user,
    
    // ==================== DADOS BANCÁRIOS (OBRIGATÓRIOS) ====================
    @NotBlank(message = "Código do banco é obrigatório")
    @ValidBankCode
    String bankCode,
    
    @NotBlank(message = "Nome do banco é obrigatório")
    String bankName,
    
    @NotBlank(message = "Agência é obrigatória")
    @Pattern(regexp = "^\\d{1,4}$", message = "Agência deve ter no máximo 4 dígitos")
    String agency,
    
    @Pattern(regexp = "^\\d?$", message = "Dígito da agência deve ter no máximo 1 dígito")
    String agencyDigit, // Opcional
    
    @NotBlank(message = "Número da conta é obrigatório")
    @Pattern(regexp = "^\\d{1,13}$", message = "Número da conta deve ter no máximo 13 dígitos")
    String accountNumber,
    
    @NotBlank(message = "Dígito da conta é obrigatório")
    String accountDigit,
    
    @NotNull(message = "Tipo de conta é obrigatório")
    AccountType accountType,
    
    // ==================== CAMPOS KYC OPCIONAIS ====================
    // Estes campos podem ser fornecidos para complementar os dados do recipient no Pagar.me
    // Se não fornecidos, o sistema pode funcionar apenas com dados do User
    String motherName,
    String monthlyIncome,
    String professionalOccupation,
    
    // ==================== CONFIGURAÇÕES DE TRANSFERÊNCIA ====================
    // Transferência automática sempre habilitada, usuário escolhe intervalo e dia
    
    /**
     * Intervalo de transferência:
     * - "Daily" = diário (todo dia útil)
     * - "Weekly" = semanal
     * - "Monthly" = mensal
     * Default: "Daily"
     */
    String transferInterval,
    
    /**
     * Dia da transferência:
     * - Para Daily: sempre 0
     * - Para Weekly: 0-6 (0=domingo, 1=segunda, ..., 6=sábado)
     * - Para Monthly: 1-31 (dia do mês)
     * Default: 0
     */
    Integer transferDay
) {
    /**
     * Referência ao usuário dono da conta bancária
     */
    public record UserRef(
        @NotNull(message = "ID do usuário é obrigatório")
        UUID id
    ) {}
}
