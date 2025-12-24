package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.Delivery;
import com.mvt.mvt_events.jpa.SiteConfiguration;
import com.mvt.mvt_events.jpa.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utilitário para cálculo de splits de pagamento.
 * 
 * Centraliza a lógica de distribuição de valores entre:
 * - Courier (motoboy)
 * - Organizer (gerente)
 * - Platform (plataforma)
 * 
 * Regras:
 * - Courier recebe: 100% - organizerPercentage - platformPercentage (padrão: 87%)
 * - Organizer recebe: organizerPercentage (padrão: 5%)
 * - Platform recebe: platformPercentage (padrão: 8%)
 * 
 * REGRA ESPECIAL - Quando não há Organizer:
 * - Courier mantém: 87%
 * - Platform incorpora: 8% + 5% = 13%
 * 
 * Esta classe garante que tanto a criação de pagamentos quanto os relatórios
 * usem exatamente a mesma lógica.
 */
@Slf4j
@Component
public class PaymentSplitCalculator {

    /**
     * Calcula o percentual que o courier deve receber de uma delivery.
     * 
     * @param config Configuração com percentuais
     * @return Percentual do courier (ex: 87.00)
     */
    public BigDecimal calculateCourierPercentage(SiteConfiguration config) {
        return BigDecimal.valueOf(100)
                .subtract(config.getOrganizerPercentage())
                .subtract(config.getPlatformPercentage());
    }

    /**
     * Calcula o percentual que a plataforma deve receber de uma delivery.
     * 
     * ATENÇÃO: Quando não há organizer, a plataforma incorpora a percentagem dele.
     * 
     * @param config Configuração com percentuais
     * @param hasOrganizer Se a delivery tem um organizer válido
     * @return Percentual da plataforma (8% com organizer, 13% sem organizer)
     */
    public BigDecimal calculatePlatformPercentage(SiteConfiguration config, boolean hasOrganizer) {
        BigDecimal platformPercentage = config.getPlatformPercentage();
        
        if (!hasOrganizer) {
            // Plataforma incorpora a percentagem do organizer (5%)
            platformPercentage = platformPercentage.add(config.getOrganizerPercentage());
        }
        
        return platformPercentage;
    }

    /**
     * Verifica se uma delivery tem um organizer válido (com pagarmeRecipientId).
     * 
     * @param delivery Delivery a verificar
     * @return true se há organizer com recipient ID configurado
     */
    public boolean hasValidOrganizer(Delivery delivery) {
        User organizer = delivery.getOrganizer();
        return organizer != null && 
               organizer.getPagarmeRecipientId() != null && 
               !organizer.getPagarmeRecipientId().isBlank();
    }

    /**
     * Calcula o valor em centavos que o courier deve receber de uma delivery.
     * 
     * @param shippingFeeCents Valor do frete em centavos
     * @param config Configuração com percentuais
     * @return Valor em centavos (arredondado para baixo)
     */
    public BigDecimal calculateCourierAmount(BigDecimal shippingFeeCents, SiteConfiguration config) {
        BigDecimal courierPercentage = calculateCourierPercentage(config);
        
        return shippingFeeCents
                .multiply(courierPercentage)
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);
    }

    /**
     * Calcula o valor em centavos que o organizer deve receber de uma delivery.
     * 
     * @param shippingFeeCents Valor do frete em centavos
     * @param config Configuração com percentuais
     * @return Valor em centavos (arredondado para baixo)
     */
    public BigDecimal calculateOrganizerAmount(BigDecimal shippingFeeCents, SiteConfiguration config) {
        return shippingFeeCents
                .multiply(config.getOrganizerPercentage())
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);
    }

    /**
     * Calcula o valor em centavos que a plataforma deve receber de uma delivery.
     * 
     * ATENÇÃO: Quando não há organizer, inclui os 5% que seriam dele.
     * 
     * @param shippingFeeCents Valor do frete em centavos
     * @param config Configuração com percentuais
     * @param hasOrganizer Se a delivery tem um organizer válido
     * @return Valor em centavos (arredondado para baixo)
     */
    public BigDecimal calculatePlatformAmount(BigDecimal shippingFeeCents, 
                                             SiteConfiguration config, 
                                             boolean hasOrganizer) {
        BigDecimal platformPercentage = calculatePlatformPercentage(config, hasOrganizer);
        
        return shippingFeeCents
                .multiply(platformPercentage)
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);
    }

    /**
     * Converte valor de reais para centavos.
     * 
     * @param valueInReais Valor em reais (ex: 100.00)
     * @return Valor em centavos (ex: 10000)
     */
    public BigDecimal toCents(BigDecimal valueInReais) {
        return valueInReais.multiply(BigDecimal.valueOf(100));
    }

    /**
     * Converte valor de centavos para reais.
     * 
     * @param valueInCents Valor em centavos (ex: 10000)
     * @param scale Casas decimais (geralmente 2)
     * @return Valor em reais (ex: 100.00)
     */
    public BigDecimal toReais(BigDecimal valueInCents, int scale) {
        return valueInCents.divide(BigDecimal.valueOf(100), scale, RoundingMode.HALF_UP);
    }
}
