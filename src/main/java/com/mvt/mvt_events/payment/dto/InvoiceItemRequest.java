package com.mvt.mvt_events.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Item de uma invoice no Iugu
 * 
 * <p>Representa um produto/serviço na fatura.</p>
 */
@Data
public class InvoiceItemRequest {

    /**
     * Descrição do item
     */
    @JsonProperty("description")
    private String description;

    /**
     * Quantidade
     */
    @JsonProperty("quantity")
    private Integer quantity;

    /**
     * Preço unitário em centavos
     */
    @JsonProperty("price_cents")
    private Integer priceCents;
}
