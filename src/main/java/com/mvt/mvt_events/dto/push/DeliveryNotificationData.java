package com.mvt.mvt_events.dto.push;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para dados de notificação de entrega
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryNotificationData {
    private String type;
    private String deliveryId;
    private String message;
    private DeliveryData deliveryData;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeliveryData {
        private String clientName;
        private BigDecimal value;
        private String address;
        private Double pickupLatitude;
        private Double pickupLongitude;
        private Double deliveryLatitude;
        private Double deliveryLongitude;
        private String description;
        private String estimatedTime;
    }
}