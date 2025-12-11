package com.mvt.mvt_events.payment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request para criação de recipient (subconta) no Pagar.me
 * 
 * @see <a href="https://docs.pagar.me/reference/criar-recebedor">Documentação Criar Recipient</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecipientRequest {
    
    private String name;
    private String email;
    private String description;
    private String document; // CPF ou CNPJ sem pontuação
    private String type; // "individual" ou "company"
    private String code; // Referência externa única
    @JsonProperty("default_bank_account")
    private DefaultBankAccount defaultBankAccount;
    @JsonProperty("transfer_settings")
    private TransferSettings transferSettings;
    @JsonProperty("register_information")
    private RegisterInformation registerInformation;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DefaultBankAccount {
        @JsonProperty("holder_name")
        private String holderName;
        @JsonProperty("holder_type")
        private String holderType; // "individual" ou "company"
        @JsonProperty("holder_document")
        private String holderDocument; // CPF/CNPJ sem pontuação
        private String bank; // Código do banco (3 dígitos)
        @JsonProperty("branch_number")
        private String branchNumber; // Agência
        @JsonProperty("branch_check_digit")
        private String branchCheckDigit; // Dígito verificador da agência (opcional)
        @JsonProperty("account_number")
        private String accountNumber; // Número da conta
        @JsonProperty("account_check_digit")
        private String accountCheckDigit; // Dígito verificador da conta
        private String type; // "checking" ou "savings"
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TransferSettings {
        @JsonProperty("transfer_enabled")
        private Boolean transferEnabled;
        @JsonProperty("transfer_interval")
        private String transferInterval; // "Daily", "Weekly", "Monthly"
        @JsonProperty("transfer_day")
        private Integer transferDay; // 0-6 (domingo a sábado) ou 1-31 (dia do mês)
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RegisterInformation {
        private String email;
        private String document;
        private String type; // "individual" ou "company"
        private String name;
        @JsonProperty("mother_name")
        private String motherName;
        private String birthdate; // Formato: "DD/MM/YYYY"
        @JsonProperty("monthly_income")
        private String monthlyIncome;
        @JsonProperty("professional_occupation")
        private String professionalOccupation;
        @JsonProperty("site_url")
        private String siteUrl;
        @JsonProperty("phone_numbers")
        private List<PhoneNumber> phoneNumbers;
        private Address address;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PhoneNumber {
        private String ddd;
        private String number;
        private String type; // "mobile", "home", "commercial"
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Address {
        private String street;
        @JsonProperty("street_number")
        private String streetNumber;
        private String complementary;
        private String neighborhood;
        private String city;
        private String state; // Sigla UF (2 letras)
        @JsonProperty("zip_code")
        private String zipCode; // CEP sem pontuação
        @JsonProperty("reference_point")
        private String referencePoint;
    }
}
