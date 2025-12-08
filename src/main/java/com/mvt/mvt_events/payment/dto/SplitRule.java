package com.mvt.mvt_events.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Regra de split de pagamento para distribuição entre subcontas
 * 
 * <p>Define como o valor de uma transação será dividido entre diferentes
 * subcontas do marketplace (motoboy, gerente, plataforma).</p>
 * 
 * <p><strong>Tipos de split:</strong></p>
 * <ul>
 *   <li><strong>percentage:</strong> Percentual do valor total (ex: 87% para motoboy)</li>
 *   <li><strong>cents_fixed:</strong> Valor fixo em centavos (ex: R$ 0,59 taxa Iugu)</li>
 * </ul>
 * 
 * @see <a href="https://dev.iugu.com/reference/split-de-pagamentos">Documentação Split Iugu</a>
 */
public record SplitRule(
        
        /**
         * ID da subconta que receberá este split
         * Se null, o valor vai para a conta master (plataforma)
         */
        @JsonProperty("receiver_id")
        String receiverId,
        
        /**
         * Percentual do split (0.00 a 100.00)
         * Usado quando splitType = "percentage"
         */
        @JsonProperty("percent")
        @DecimalMin(value = "0.00", message = "Percentual deve ser >= 0")
        @DecimalMax(value = "100.00", message = "Percentual deve ser <= 100")
        BigDecimal percent,
        
        /**
         * Valor fixo em centavos
         * Usado quando splitType = "cents_fixed"
         */
        @JsonProperty("cents")
        Integer cents,
        
        /**
         * Tipo de split: "percentage" ou "cents_fixed"
         */
        @JsonProperty("split_type")
        @NotBlank(message = "Tipo de split é obrigatório")
        String splitType,
        
        /**
         * Descrição do split (opcional)
         */
        @JsonProperty("description")
        String description
) {
    
    /**
     * Cria um split por percentual
     * 
     * @param receiverId ID da subconta receptora (null = plataforma)
     * @param percent Percentual (0.00 a 100.00)
     * @param description Descrição
     * @return SplitRule configurado
     */
    public static SplitRule percentage(String receiverId, BigDecimal percent, String description) {
        return new SplitRule(
                receiverId,
                percent,
                null,
                "percentage",
                description
        );
    }
    
    /**
     * Cria um split por valor fixo
     * 
     * @param receiverId ID da subconta receptora (null = plataforma)
     * @param cents Valor em centavos
     * @param description Descrição
     * @return SplitRule configurado
     */
    public static SplitRule fixedCents(String receiverId, Integer cents, String description) {
        return new SplitRule(
                receiverId,
                null,
                cents,
                "cents_fixed",
                description
        );
    }
    
    /**
     * Cria um split para o motoboy (87%)
     * 
     * @param motoboyAccountId ID da subconta do motoboy
     * @return SplitRule configurado
     */
    public static SplitRule forCourier(String motoboyAccountId) {
        return percentage(
                motoboyAccountId,
                BigDecimal.valueOf(87.0),
                "Pagamento ao motoboy"
        );
    }
    
    /**
     * Cria um split para o gerente (5%)
     * 
     * @param managerAccountId ID da subconta do gerente
     * @return SplitRule configurado
     */
    public static SplitRule forManager(String managerAccountId) {
        return percentage(
                managerAccountId,
                BigDecimal.valueOf(5.0),
                "Comissão do gerente"
        );
    }
    
    /**
     * Cria um split para a plataforma (8%)
     * 
     * @return SplitRule configurado (receiverId = null)
     */
    public static SplitRule forPlatform() {
        return percentage(
                null, // null = conta master (plataforma)
                BigDecimal.valueOf(8.0),
                "Taxa da plataforma"
        );
    }
    
    /**
     * Cria um split para a taxa fixa do Iugu (R$ 0,59)
     * 
     * @return SplitRule configurado
     */
    public static SplitRule forIuguFee() {
        return fixedCents(
                null,
                59, // R$ 0,59
                "Taxa Iugu"
        );
    }
    
    /**
     * Valida se o split está corretamente configurado
     * 
     * @throws IllegalStateException se configuração inválida
     */
    public void validate() {
        if ("percentage".equals(splitType) && percent == null) {
            throw new IllegalStateException("Split tipo 'percentage' requer campo 'percent'");
        }
        if ("cents_fixed".equals(splitType) && cents == null) {
            throw new IllegalStateException("Split tipo 'cents_fixed' requer campo 'cents'");
        }
        if (percent != null && (percent.compareTo(BigDecimal.ZERO) < 0 || percent.compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new IllegalStateException("Percentual deve estar entre 0 e 100");
        }
    }
}
