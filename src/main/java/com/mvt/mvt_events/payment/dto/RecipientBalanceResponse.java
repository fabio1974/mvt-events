package com.mvt.mvt_events.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
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

    /**
     * Formato atual da API Pagar.me (core v5): valores em centavos no nível raiz.
     */
    @JsonProperty("available_amount")
    private Long availableAmount;

    @JsonProperty("waiting_funds_amount")
    private Long waitingFundsAmount;

    @JsonProperty("transferred_amount")
    private Long transferredAmount;

    // ── Valores em Reais (calculados pelo BE para facilitar o mobile) ──────────

    /**
     * Saldo disponível em Reais (ex: 12.50)
     */
    public BigDecimal getAvailableBrl() {
        return toBrl(resolveAvailableCents());
    }

    /**
     * Valores a receber em Reais (ex: 5.00)
     */
    public BigDecimal getWaitingFundsBrl() {
        return toBrl(resolveWaitingFundsCents());
    }

    /**
     * Total transferido em Reais (ex: 200.00)
     */
    public BigDecimal getTransferredBrl() {
        return toBrl(resolveTransferredCents());
    }

    public Long getAvailableCents() {
        return resolveAvailableCents();
    }

    public Long getWaitingFundsCents() {
        return resolveWaitingFundsCents();
    }

    public Long getTransferredCents() {
        return resolveTransferredCents();
    }

    private Long resolveAvailableCents() {
        if (availableAmount != null) return availableAmount;
        return available != null ? available.getAmount() : null;
    }

    private Long resolveWaitingFundsCents() {
        if (waitingFundsAmount != null) return waitingFundsAmount;
        return waitingFunds != null ? waitingFunds.getAmount() : null;
    }

    private Long resolveTransferredCents() {
        if (transferredAmount != null) return transferredAmount;
        return transferred != null ? transferred.getAmount() : null;
    }

    private BigDecimal toBrl(Long cents) {
        if (cents == null) return BigDecimal.ZERO;
        return BigDecimal.valueOf(cents)
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
