package com.mvt.mvt_events.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request para criação de order (pedido) no Pagar.me
 * 
 * Suporta:
 * - Múltiplos items (deliveries)
 * - Customer com dados completos (nome, email, documento, endereço)
 * - Pagamento via PIX com split automático
 * - Split: 87% courier, 5% manager, 8% plataforma
 * 
 * @see <a href="https://docs.pagar.me/reference/criar-pedido">Documentação Criar Order</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {
    
    private Boolean closed; // true para encerrar a order imediatamente
    private List<ItemRequest> items;
    private CustomerRequest customer;
    private List<PaymentRequest> payments;
    
    // ============================================================================
    // NESTED CLASSES
    // ============================================================================
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemRequest {
        private Long amount; // Valor em centavos (ex: 2990 = R$ 29,90)
        private String description; // Descrição do item
        private Long quantity; // Quantidade
        private String code; // Código único
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerRequest {
        private String name; // Nome completo
        private String type; // "individual" ou "company"
        private String email; // Email
        private String document; // CPF/CNPJ sem pontuação
        private AddressRequest address; // Endereço
        private PhonesRequest phones; // Telefones
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressRequest {
        @JsonProperty("line_1")
        private String line1; // Rua/Avenida
        
        @JsonProperty("line_2")
        private String line2; // Número/Complemento
        
        @JsonProperty("zip_code")
        private String zipCode; // CEP (ex: 05425070)
        
        private String city; // Cidade
        private String state; // Estado (UF)
        private String country; // País (BR)
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PhonesRequest {
        @JsonProperty("home_phone")
        private PhoneRequest homePhone;
        
        @JsonProperty("mobile_phone")
        private PhoneRequest mobilePhone;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PhoneRequest {
        @JsonProperty("country_code")
        private String countryCode; // "55"
        
        @JsonProperty("area_code")
        private String areaCode; // "11"
        
        private String number; // "999999999"
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentRequest {
        @JsonProperty("payment_method")
        private String paymentMethod; // "pix" ou "credit_card"
        
        private PixRequest pix; // Configurações PIX
        @JsonProperty("credit_card")
        private CreditCardRequest creditCard; // Configurações Cartão de Crédito
        private List<SplitRequest> split; // Split de valores
    }
    
    /**
     * Request para pagamento via Cartão de Crédito
     * 
     * @see <a href="https://docs.pagar.me/reference/criar-pedido">Documentação Pagar.me</a>
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreditCardRequest {
        @JsonProperty("operation_type")
        private String operationType; // "auth_and_capture" ou "auth_only"
        
        private Integer installments; // Número de parcelas (1-12)
        
        @JsonProperty("statement_descriptor")
        private String statementDescriptor; // Nome na fatura (máx 13 caracteres)
        
        @JsonProperty("card_token")
        private String cardToken; // Token do cartão gerado via Pagar.me
        
        private CardRequest card; // Dados do cartão (billing address)
    }
    
    /**
     * Dados do cartão com billing address
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CardRequest {
        @JsonProperty("billing_address")
        private BillingAddressRequest billingAddress;
    }
    
    /**
     * Endereço de cobrança do cartão
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BillingAddressRequest {
        @JsonProperty("line_1")
        private String line1; // "Número, Rua, Bairro"
        
        @JsonProperty("zip_code")
        private String zipCode; // CEP (ex: 05425070)
        
        private String city; // Cidade
        private String state; // Estado (UF)
        private String country; // País (BR)
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PixRequest {
        @JsonProperty("expires_in")
        private String expiresIn; // Tempo de expiração em segundos (ex: "7200" = 2h)
        
        @JsonProperty("additional_information")
        private List<AdditionalInfoRequest> additionalInformation; // Informações extras
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdditionalInfoRequest {
        private String name; // Nome da info
        private String value; // Valor da info
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SplitRequest {
        private Integer amount; // Percentual (ex: 8700 = 87.00%) ou valor em centavos
        @JsonProperty("recipient_id")
        private String recipientId; // ID do recipient no Pagar.me
        private String type; // "percentage" ou "fixed"
        private SplitOptionsRequest options; // Opções de split
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SplitOptionsRequest {
        @JsonProperty("charge_processing_fee")
        private Boolean chargeProcessingFee; // Cobrar taxa de processamento?
        
        @JsonProperty("charge_remainder_fee")
        private Boolean chargeRemainderFee; // Cobrar taxa de remainder?
        
        private Boolean liable; // É liable (responsável por chargebacks)?
    }
}
