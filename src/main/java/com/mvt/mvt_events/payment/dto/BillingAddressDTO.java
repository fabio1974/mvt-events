package com.mvt.mvt_events.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO para endereço de cobrança no formato do Pagar.me.
 * 
 * Utilizado ao criar cartões de crédito.
 * Este endereço NÃO é persistido no banco - apenas repassado ao Pagar.me.
 * 
 * Formato esperado pelo Pagar.me:
 * - line_1: "{numero}, {rua}, {bairro}"
 * - line_2: complemento (opcional)
 * - zip_code: CEP sem formatação (8 dígitos)
 * - city: nome da cidade
 * - state: UF (2 letras)
 * - country: código do país (ex: "BR")
 */
@Data
public class BillingAddressDTO {
    
    /**
     * Linha 1 do endereço: formato "{numero}, {rua}, {bairro}"
     * Exemplo: "7221, Avenida Dra Ruth Cardoso, Pinheiros"
     */
    @NotBlank(message = "line_1 é obrigatório")
    @Size(max = 255, message = "line_1 deve ter no máximo 255 caracteres")
    private String line1;
    
    /**
     * Linha 2 do endereço: complemento (opcional)
     * Exemplo: "Apto 42", "Bloco B"
     */
    @Size(max = 255, message = "line_2 deve ter no máximo 255 caracteres")
    private String line2;
    
    /**
     * CEP sem formatação (8 dígitos)
     * Exemplo: "01311000"
     */
    @NotBlank(message = "zip_code é obrigatório")
    @Pattern(regexp = "\\d{8}", message = "zip_code deve conter exatamente 8 dígitos")
    private String zipCode;
    
    /**
     * Nome da cidade
     * Exemplo: "São Paulo"
     */
    @NotBlank(message = "city é obrigatório")
    private String city;
    
    /**
     * UF (2 letras)
     * Exemplo: "SP"
     */
    @NotBlank(message = "state é obrigatório")
    @Pattern(regexp = "[A-Z]{2}", message = "state deve conter exatamente 2 letras maiúsculas")
    private String state;
    
    /**
     * Código do país
     * Exemplo: "BR"
     */
    @NotBlank(message = "country é obrigatório")
    @Pattern(regexp = "[A-Z]{2}", message = "country deve conter exatamente 2 letras maiúsculas")
    private String country;
}
