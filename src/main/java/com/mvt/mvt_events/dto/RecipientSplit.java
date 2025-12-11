package com.mvt.mvt_events.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Representa quanto cada pessoa deve receber em uma invoice
 * 
 * <p>Usado internamente para calcular splits do Pagar.me</p>
 */
@Data
@AllArgsConstructor
public class RecipientSplit {
    
    /**
     * ID do recipient Pagar.me (motoboy ou gerente)
     */
    private String pagarmeRecipientId;
    
    /**
     * Tipo de recipiente
     */
    private RecipientType type;
    
    /**
     * Valor em centavos que esta pessoa deve receber
     */
    private Integer amountCents;
    
    /**
     * Valor formatado (para logs)
     */
    public BigDecimal getAmount() {
        return BigDecimal.valueOf(amountCents).divide(BigDecimal.valueOf(100));
    }
    
    public enum RecipientType {
        COURIER,    // Motoboy
        MANAGER,    // Gerente
        PLATFORM    // Plataforma (conta master)
    }
}
