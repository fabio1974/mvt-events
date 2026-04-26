package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.SiteConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calcula splits de pagamento para FoodOrder (Zapi-Food).
 *
 * No CHECKOUT (3 recipients), customer paga (food + delivery) como 1 PIX:
 *   - Estabelecimento (client): 87% × food
 *   - Organizer: 5% × (food + delivery)   [0 se não houver organizer]
 *   - Plataforma: o resto (8% × food + 95% × delivery; ou 13% × food + 95% × delivery sem organizer)
 *
 * No ACCEPT da delivery pelo courier (transfer), a plataforma transfere:
 *   - 87% × delivery → courier
 * Reduzindo o saldo da plataforma para 8% × delivery (consistente com regra de corrida).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FoodOrderSplitCalculator {

    /** 87% — percentual que o estabelecimento (client) recebe sobre a comida. */
    private static final BigDecimal CLIENT_FOOD_PERCENTAGE = new BigDecimal("87");

    /** 87% — percentual que o courier recebe sobre o frete (mesmo de corrida). */
    private static final BigDecimal COURIER_DELIVERY_PERCENTAGE = new BigDecimal("87");

    /** 5% — percentual que o organizer (gerente) recebe sobre o total (comida + frete). */
    private static final BigDecimal ORGANIZER_TOTAL_PERCENTAGE = new BigDecimal("5");

    /**
     * Split do PIX no checkout. Os 3 valores somam exatamente totalCents.
     */
    public CheckoutSplit calculateCheckoutSplit(BigDecimal foodCents,
                                                 BigDecimal deliveryCents,
                                                 SiteConfiguration config,
                                                 boolean hasOrganizer) {
        BigDecimal totalCents = foodCents.add(deliveryCents);
        BigDecimal clientAmount = foodCents
                .multiply(CLIENT_FOOD_PERCENTAGE)
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);

        BigDecimal organizerAmount = BigDecimal.ZERO;
        if (hasOrganizer) {
            organizerAmount = totalCents
                    .multiply(config.getOrganizerPercentage())
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);
        }

        // Plataforma absorve arredondamentos (total - client - organizer)
        BigDecimal platformAmount = totalCents.subtract(clientAmount).subtract(organizerAmount);

        return new CheckoutSplit(clientAmount, organizerAmount, platformAmount, totalCents);
    }

    /**
     * Valor em centavos a transferir da plataforma → courier quando ele aceita a delivery.
     * 87% do frete, arredondado pra baixo (como no split das corridas).
     */
    public BigDecimal calculateCourierTransferAmount(BigDecimal deliveryCents) {
        return deliveryCents
                .multiply(COURIER_DELIVERY_PERCENTAGE)
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);
    }

    /**
     * Valor em centavos a transferir da plataforma → organizer quando o courier aceita.
     * 5% do total (comida + frete), arredondado pra baixo.
     * Como o organizer é indeterminado no checkout (depende de qual courier vai aceitar),
     * o repasse sai sempre via transfer pós-accept, e não pelo split do Pagar.me.
     */
    public BigDecimal calculateOrganizerTransferAmount(BigDecimal totalCents) {
        return totalCents
                .multiply(ORGANIZER_TOTAL_PERCENTAGE)
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);
    }

    public BigDecimal toCents(BigDecimal reais) {
        return reais.multiply(BigDecimal.valueOf(100));
    }

    @Value
    public static class CheckoutSplit {
        BigDecimal clientAmountCents;
        BigDecimal organizerAmountCents;
        BigDecimal platformAmountCents;
        BigDecimal totalCents;
    }
}
