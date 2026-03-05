package com.mvt.mvt_events.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Response do saldo de um recipient (recebedor) no Pagar.me.
 *
 * Estrutura retornada pela API Pagar.me:
 * GET https://api.pagar.me/core/v5/recipients/{recipient_id}/balance
 *
 * Todos os valores da Pagar.me são em centavos (Integer).
 * Esta classe os converte automaticamente para Reais (BigDecimal) via campos calculados.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipientBalanceResponse {

    /**
     * ID do recebedor no Pagar.me (re_XXXXX)
     */
    private String recipientId;

    /**
     * Valores aguardando pagamento/liberação (em centavos — raw da Pagar.me)
     */
    @JsonProperty("waiting_funds")
    private BalanceItem waitingFunds;

    /**
     * Saldo disponível para saque (em centavos — raw da Pagar.me)
     */
    @JsonProperty("available")
    private BalanceItem available;

    /**
     * Total já transferido/sacado (em centavos — raw da Pagar.me)
     */
    @JsonProperty("transferred")
    private BalanceItem transferred;

    // ── Valores em Reais (calculados pelo BE para facilitar o mobile) ──────────

    /**
     * Saldo disponível em Reais (ex: 12.50)
     */
    public BigDecimal getAvailableBrl() {
        return toBrl(available);
    }

    /**
     * Valores a receber em Reais (ex: 5.00)
     */
    public BigDecimal getWaitingFundsBrl() {
        return toBrl(waitingFunds);
    }

    /**
     * Total transferido em Reais (ex: 200.00)
     */
    public BigDecimal getTransferredBrl() {
        return toBrl(transferred);
    }

    private BigDecimal toBrl(BalanceItem item) {
        if (item == null || item.getAmount() == null) return BigDecimal.ZERO;
        return BigDecimal.valueOf(item.getAmount())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BalanceItem {
        /**
         * Valor em centavos (ex: 1250 = R$ 12,50)
         */
        private Long amount;
    }
}
