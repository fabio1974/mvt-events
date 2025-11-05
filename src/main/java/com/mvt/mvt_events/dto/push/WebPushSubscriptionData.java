package com.mvt.mvt_events.dto.push;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para dados de subscription do Web Push
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebPushSubscriptionData {

    private String endpoint;
    private WebPushKeys keys;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WebPushKeys {
        private String p256dh;
        private String auth;
    }
}