package com.mvt.mvt_events.payment.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Relatório detalhado de um pagamento consolidado.
 * Mostra a composição completa do pagamento com splits por delivery.
 */
@Data
@Builder
public class PaymentReportResponse {
    
    private Long paymentId;
    private String providerPaymentId;
    private String status;
    private BigDecimal totalAmount;
    private String currency;
    private LocalDateTime createdAt;
    private String pixQrCode;
    private String pixQrCodeUrl;
    private LocalDateTime expiresAt;
    
    private List<DeliveryItem> deliveries;
    private List<SplitItem> consolidatedSplits;
    
    /**
     * Detalhes de cada delivery incluída no pagamento
     */
    @Data
    @Builder
    public static class DeliveryItem {
        private Long deliveryId;
        private BigDecimal shippingFee;
        private String clientName;
        private String pickupAddress;
        private String deliveryAddress;
        private List<SplitItem> splits;
    }
    
    /**
     * Detalhes de cada split (parte do pagamento para um recipient)
     */
    @Data
    @Builder
    public static class SplitItem {
        private String recipientId;
        private String recipientName;
        private String recipientRole; // COURIER, ORGANIZER, PLATFORM
        private BigDecimal amount;
        private BigDecimal percentage;
        private Boolean liable;
    }
}
