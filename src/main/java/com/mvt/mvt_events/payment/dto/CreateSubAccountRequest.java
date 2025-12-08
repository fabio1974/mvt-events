package com.mvt.mvt_events.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request para criar uma subconta no Iugu
 * 
 * <p>Usado para criar subcontas de marketplace para motoboys e gerentes.
 * Cada subconta tem seus próprios dados bancários e recebe transferências automáticas.</p>
 * 
 * <p><strong>Endpoint:</strong> POST /v1/marketplace/create_account</p>
 * 
 * @see <a href="https://dev.iugu.com/reference/criar-subconta">Documentação Criar Subconta</a>
 */
public record CreateSubAccountRequest(
        
        /**
         * Nome completo do titular da conta
         */
        @JsonProperty("name")
        @NotBlank(message = "Nome é obrigatório")
        @Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres")
        String name,
        
        /**
         * Email do titular (usado para login no painel Iugu)
         */
        @JsonProperty("email")
        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email inválido")
        String email,
        
        /**
         * CPF/CNPJ do titular (apenas números)
         */
        @JsonProperty("cpf_cnpj")
        @NotBlank(message = "CPF/CNPJ é obrigatório")
        @Pattern(regexp = "\\d{11}|\\d{14}", message = "CPF deve ter 11 dígitos ou CNPJ 14 dígitos")
        String cpfCnpj,
        
        /**
         * Código do banco (3 dígitos)
         */
        @JsonProperty("bank")
        @NotBlank(message = "Código do banco é obrigatório")
        @Pattern(regexp = "\\d{3}", message = "Código do banco deve ter 3 dígitos")
        String bank,
        
        /**
         * Número da agência (sem dígito verificador)
         */
        @JsonProperty("bank_ag")
        @NotBlank(message = "Agência é obrigatória")
        String bankAgency,
        
        /**
         * Número da conta (com dígito verificador)
         */
        @JsonProperty("bank_cc")
        @NotBlank(message = "Conta bancária é obrigatória")
        String bankAccount,
        
        /**
         * Tipo de conta: "Corrente" ou "Poupança"
         */
        @JsonProperty("account_type")
        @NotBlank(message = "Tipo de conta é obrigatório")
        String accountType,
        
        /**
         * Habilitar transferências automáticas D+1
         */
        @JsonProperty("auto_withdraw")
        Boolean autoWithdraw,
        
        /**
         * Percentual de comissão da plataforma sobre cada transação
         * Exemplo: 8.0 para 8%
         */
        @JsonProperty("commission_percent")
        BigDecimal commissionPercent
) {
    
    /**
     * Construtor com valores padrão para marketplace
     * 
     * @param name Nome completo
     * @param email Email
     * @param cpfCnpj CPF/CNPJ apenas números
     * @param bank Código do banco (3 dígitos)
     * @param bankAgency Agência
     * @param bankAccount Conta com dígito
     * @param accountType "Corrente" ou "Poupança"
     * @return Request configurado com auto-withdraw habilitado
     */
    public static CreateSubAccountRequest withDefaults(
            String name,
            String email,
            String cpfCnpj,
            String bank,
            String bankAgency,
            String bankAccount,
            String accountType
    ) {
        return new CreateSubAccountRequest(
                name,
                email,
                cpfCnpj,
                bank,
                bankAgency,
                bankAccount,
                accountType,
                true, // auto_withdraw habilitado por padrão
                BigDecimal.ZERO // comissão será configurada na plataforma
        );
    }
}
