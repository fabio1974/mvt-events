package com.mvt.mvt_events.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Response consolidada de invoice com detalhes de splits
 * 
 * <p>Retorna dados do pedido criado no Pagar.me mais informações agregadas
 * sobre como o valor foi distribuído entre motoboys, gerentes e plataforma.</p>
 */
@Data
@AllArgsConstructor
public class ConsolidatedInvoiceResponse {

    /**
     * ID do pagamento no sistema
     */
    private Long paymentId;

    /**
     * ID do pedido no Pagar.me
     */
    private String pagarmeOrderId;

    /**
     * QR Code PIX (string para copiar/colar)
     */
    private String pixQrCode;

    /**
     * URL da imagem do QR Code PIX
     */
    private String pixQrCodeUrl;

    /**
     * URL segura para pagamento (navegador)
     */
    private String secureUrl;

    /**
     * Valor total da invoice
     */
    private BigDecimal amount;

    /**
     * Quantidade de deliveries nesta invoice
     */
    private Integer deliveryCount;

    /**
     * Detalhes dos splits calculados
     */
    private SplitDetails splits;

    /**
     * Status do pagamento
     */
    private String status;

    /**
     * Data/hora de expiração da invoice
     */
    private LocalDateTime expiresAt;

    /**
     * Mensagem amigável sobre o status
     */
    private String statusMessage;

    /**
     * Se a invoice já expirou
     */
    private Boolean expired;

    /**
     * Detalhes dos splits (agregados por tipo)
     */
    @Data
    @AllArgsConstructor
    public static class SplitDetails {
        /**
         * Quantidade de motoboys recebendo pagamento
         */
        private Integer couriersCount;

        /**
         * Quantidade de gerentes recebendo pagamento
         */
        private Integer managersCount;

        /**
         * Valor total para motoboys
         */
        private BigDecimal couriersAmount;

        /**
         * Valor total para gerentes
         */
        private BigDecimal managersAmount;

        /**
         * Valor para plataforma
         */
        private BigDecimal platformAmount;

        /**
         * Mapa de recipients individuais (para debug)
         * Chave: tipo-nome, Valor: valor em reais
         */
        private Map<String, BigDecimal> recipients;
    }
}
