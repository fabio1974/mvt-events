package com.mvt.mvt_events.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request para criar uma fatura (invoice) com split de pagamento
 * 
 * <p>Cria uma fatura PIX com split automático entre subconta do motoboy,
 * subconta do gerente e a plataforma.</p>
 * 
 * <p><strong>Endpoint:</strong> POST /v1/invoices</p>
 * 
 * @see <a href="https://dev.iugu.com/reference/criar-invoice">Documentação Criar Invoice</a>
 */
public record CreateInvoiceRequest(
        
        /**
         * Email do pagador (cliente)
         */
        @JsonProperty("email")
        @NotBlank(message = "Email do pagador é obrigatório")
        String email,
        
        /**
         * Data de vencimento (formato: DD/MM/YYYY)
         */
        @JsonProperty("due_date")
        @NotBlank(message = "Data de vencimento é obrigatória")
        String dueDate,
        
        /**
         * Valor total em centavos
         * Exemplo: 10000 para R$ 100,00
         */
        @JsonProperty("total_cents")
        @NotNull(message = "Valor total é obrigatório")
        @DecimalMin(value = "100", message = "Valor mínimo é R$ 1,00")
        Integer totalCents,
        
        /**
         * Itens da fatura
         */
        @JsonProperty("items")
        @NotNull(message = "Itens são obrigatórios")
        @Size(min = 1, message = "Deve ter pelo menos 1 item")
        List<InvoiceItem> items,
        
        /**
         * Métodos de pagamento aceitos
         * Exemplo: ["pix"]
         */
        @JsonProperty("payable_with")
        List<String> payableWith,
        
        /**
         * Regras de split entre subcontas
         */
        @JsonProperty("splits")
        List<SplitRule> splits,
        
        /**
         * Descrição/observações da fatura
         */
        @JsonProperty("custom_variables")
        List<CustomVariable> customVariables
) {
    
    /**
     * Item de uma fatura
     */
    public record InvoiceItem(
            @JsonProperty("description")
            @NotBlank(message = "Descrição do item é obrigatória")
            String description,
            
            @JsonProperty("quantity")
            @NotNull(message = "Quantidade é obrigatória")
            Integer quantity,
            
            @JsonProperty("price_cents")
            @NotNull(message = "Preço é obrigatório")
            Integer priceCents
    ) {}
    
    /**
     * Variável customizada para metadados
     */
    public record CustomVariable(
            @JsonProperty("name")
            String name,
            
            @JsonProperty("value")
            String value
    ) {}
    
    /**
     * Cria uma invoice PIX para entrega
     * 
     * @param email Email do cliente
     * @param dueDate Data de vencimento (DD/MM/YYYY)
     * @param amount Valor total da entrega
     * @param deliveryId ID da entrega
     * @param splits Regras de split
     * @return Request configurado para PIX
     */
    public static CreateInvoiceRequest forDelivery(
            String email,
            String dueDate,
            BigDecimal amount,
            String deliveryId,
            List<SplitRule> splits
    ) {
        int totalCents = amount.multiply(BigDecimal.valueOf(100)).intValue();
        
        return new CreateInvoiceRequest(
                email,
                dueDate,
                totalCents,
                List.of(new InvoiceItem(
                        "Serviço de entrega #" + deliveryId,
                        1,
                        totalCents
                )),
                List.of("pix"),
                splits,
                List.of(new CustomVariable("delivery_id", deliveryId))
        );
    }
}
