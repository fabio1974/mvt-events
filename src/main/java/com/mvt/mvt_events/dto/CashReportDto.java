package com.mvt.mvt_events.dto;

import com.mvt.mvt_events.jpa.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Relatório diário de caixa de um estabelecimento.
 * Estrutura espelha o relatório-modelo (Tex Burguer 29/03/2026).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashReportDto {

    private String storeName;
    private String storeDocument;
    private String storeAddress;
    private LocalDate date;

    /** Resumo por canal: BALCAO, MESAS, DELIVERY. */
    private List<ChannelSummary> channels;

    /** Totais por forma de pagamento. */
    private Map<PaymentMethod, BigDecimal> paymentMethods;

    /** Saída de itens (agregada por produto). */
    private List<ItemRow> items;

    /** Total geral (soma de todos os canais). */
    private BigDecimal grandTotal;

    /** Resumo do caixa (fundo, adições, retiradas, esperado, contado). Pode ser null se não houver sessão. */
    private CashSummary cash;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CashSummary {
        private BigDecimal openingBalance;       // Fundo de caixa
        private BigDecimal additions;            // Suprimentos
        private BigDecimal withdrawals;          // Retiradas + Sangrias
        private BigDecimal cashSales;            // Vendas em dinheiro no período
        private BigDecimal expectedBalance;      // Calculado (saldo do sistema)
        private BigDecimal actualBalance;        // Contagem manual (null enquanto OPEN)
        private String status;                   // OPEN | CLOSED | NONE
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChannelSummary {
        private Channel channel;
        private int orderCount;
        private BigDecimal itemsTotal;
        private BigDecimal deliveryFeeTotal;
        private BigDecimal total;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemRow {
        private Long productId;
        private String productName;
        private int quantity;
        private BigDecimal total;
    }

    public enum Channel {
        BALCAO,
        MESAS,
        DELIVERY
    }
}
