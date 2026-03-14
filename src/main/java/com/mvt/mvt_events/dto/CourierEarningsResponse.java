package com.mvt.mvt_events.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response contendo o histórico de recebimentos do courier.
 * Lista todas as deliveries COMPLETED com pagamento PAID,
 * mostrando o detalhamento da repartição de cada corrida.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourierEarningsResponse {

    /**
     * Total de corridas completadas e pagas
     */
    private Integer totalDeliveries;

    /**
     * Total ganho pelo courier (soma de todos os courierAmount)
     */
    private BigDecimal totalEarnings;

    /**
     * Detalhamento de cada corrida
     */
    private List<DeliveryEarningDetail> deliveries;

    /**
     * Detalhamento de uma corrida com repartição de valores
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeliveryEarningDetail {
        
        // ============================================================================
        // Informações da Delivery
        // ============================================================================
        
        /**
         * ID da delivery
         */
        private Long deliveryId;

        /**
         * Data de conclusão da entrega
         */
        private String completedAt;

        /**
         * Endereço de origem
         */
        private String fromAddress;

        /**
         * Endereço de destino
         */
        private String toAddress;

        /**
         * Distância em km
         */
        private BigDecimal distanceKm;

        /**
         * Nome do cliente que solicitou a entrega
         */
        private String clientName;

        /**
         * Tipo de entrega: DELIVERY ou RIDE
         */
        private String deliveryType;

        // ============================================================================
        // Informações do Pagamento
        // ============================================================================

        /**
         * ID do pagamento
         */
        private Long paymentId;

        /**
         * Valor total da corrida (shipping fee)
         */
        private BigDecimal totalAmount;

        /**
         * Status do pagamento (sempre PAID neste endpoint)
         */
        private String paymentStatus;

        /**
         * Método de pagamento: PIX ou CREDIT_CARD
         */
        private String paymentMethod;

        // ============================================================================
        // Repartição (Split)
        // ============================================================================

        /**
         * Valor que o courier recebeu (87% do total)
         */
        private BigDecimal courierAmount;

        /**
         * Percentual do courier (geralmente 87%)
         */
        private BigDecimal courierPercentage;

        /**
         * Valor que o organizer recebeu (5% se houver, 0 caso contrário)
         */
        private BigDecimal organizerAmount;

        /**
         * Percentual do organizer (5% se houver, 0 caso contrário)
         */
        private BigDecimal organizerPercentage;

        /**
         * Nome do organizer (null se não houver)
         */
        private String organizerName;

        /**
         * Valor que a plataforma recebeu (8% ou 13%)
         */
        private BigDecimal platformAmount;

        /**
         * Percentual da plataforma (8% com organizer, 13% sem)
         */
        private BigDecimal platformPercentage;
    }
}
