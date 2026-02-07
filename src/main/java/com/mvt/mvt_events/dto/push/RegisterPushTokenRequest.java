package com.mvt.mvt_events.dto.push;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para registro de token push híbrido (Expo + Web Push)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterPushTokenRequest {

    @NotBlank(message = "Token é obrigatório")
    @JsonAlias("pushToken") // Aceita também pushToken vindo do mobile
    private String token;

    @NotBlank(message = "Platform é obrigatória")
    private String platform; // ios, android, web

    @NotBlank(message = "DeviceType é obrigatório")
    @JsonAlias("tokenType") // Aceita também tokenType vindo do mobile
    private String deviceType; // mobile, web, tablet

    // Dados específicos para Web Push (opcional, apenas para deviceType=web)
    private WebPushSubscriptionData subscriptionData;
}