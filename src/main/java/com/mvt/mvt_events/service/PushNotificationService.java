package com.mvt.mvt_events.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mvt.mvt_events.dto.push.DeliveryNotificationData;
import com.mvt.mvt_events.dto.push.ExpoPushMessage;
import com.mvt.mvt_events.jpa.UserPushToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Serviço para envio de notificações push híbridas (Expo + Web Push)
 */
@Service
@Slf4j
public class PushNotificationService {

    @Value("${expo.access-token:}")
    private String expoAccessToken;

    @Value("${expo.push-url:https://exp.host/--/api/v2/push/send}")
    private String expoPushUrl;

    @Autowired
    private UserPushTokenService pushTokenService;

    @Autowired
    private WebPushService webPushService;

    @Autowired
    private ObjectMapper objectMapper;

    private final RestTemplate restTemplate;

    public PushNotificationService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Envia notificação de convite de entrega para um motorista específico
     */
    public void sendDeliveryInvite(UUID driverId, UUID deliveryId, String clientName,
            BigDecimal value, String address,
            Double pickupLat, Double pickupLng,
            Double deliveryLat, Double deliveryLng) {
        try {
            log.info("Enviando convite de entrega {} para motorista {}", deliveryId, driverId);

            // Busca tokens ativos do motorista
            List<UserPushToken> tokens = pushTokenService.getActiveTokensByUserId(driverId);

            if (tokens.isEmpty()) {
                log.warn("Nenhum token push ativo encontrado para motorista {}", driverId);
                return;
            }

            // Prepara dados da notificação
            DeliveryNotificationData notificationData = buildDeliveryNotificationData(
                    deliveryId, clientName, value, address, pickupLat, pickupLng, deliveryLat, deliveryLng);

            // Filtra apenas tokens Expo válidos
            List<String> expoTokens = tokens.stream()
                    .map(UserPushToken::getToken)
                    .filter(this::isValidExpoToken)
                    .collect(Collectors.toList());

            if (expoTokens.isEmpty()) {
                log.warn("Nenhum token Expo válido encontrado para motorista {}", driverId);
                return;
            }

            // Cria mensagem push
            ExpoPushMessage pushMessage = ExpoPushMessage.builder()
                    .to(expoTokens)
                    .title("🚚 Nova Entrega Disponível!")
                    .body(String.format("Entrega de R$ %.2f - %s", value, clientName))
                    .data(notificationData)
                    .sound("default")
                    .priority("high")
                    .channelId("delivery-invites")
                    .displayInForeground(true) // ← Força foreground (serializa como _displayInForeground)
                    .badge(1)
                    .ttl(300) // 5 minutos
                    .build();

            // Envia notificação
            boolean success = sendExpoPushNotification(Collections.singletonList(pushMessage));

            if (success) {
                log.info("Notificação enviada com sucesso para {} tokens", expoTokens.size());
            } else {
                log.error("Falha ao enviar notificação para motorista {}", driverId);
            }

        } catch (Exception e) {
            log.error("Erro ao enviar convite de entrega para motorista {}: {}", driverId, e.getMessage(), e);
        }
    }

    /**
     * Envia notificação para múltiplos motoristas
     */
    public void sendDeliveryInviteToMultipleDrivers(List<UUID> driverIds, UUID deliveryId,
            String clientName, BigDecimal value, String address,
            Double pickupLat, Double pickupLng,
            Double deliveryLat, Double deliveryLng) {
        try {
            log.info("Enviando convite de entrega {} para {} motoristas", deliveryId, driverIds.size());

            // Busca tokens ativos de todos os motoristas
            List<UserPushToken> tokens = pushTokenService.getActiveTokensByUserIds(driverIds);

            if (tokens.isEmpty()) {
                log.warn("Nenhum token push ativo encontrado para os motoristas: {}", driverIds);
                return;
            }

            // Filtra apenas tokens Expo válidos
            List<String> expoTokens = tokens.stream()
                    .map(UserPushToken::getToken)
                    .filter(this::isValidExpoToken)
                    .collect(Collectors.toList());

            if (expoTokens.isEmpty()) {
                log.warn("Nenhum token Expo válido encontrado para os motoristas: {}", driverIds);
                return;
            }

            // Prepara dados da notificação
            DeliveryNotificationData notificationData = buildDeliveryNotificationData(
                    deliveryId, clientName, value, address, pickupLat, pickupLng, deliveryLat, deliveryLng);

            // Cria mensagem push
            ExpoPushMessage pushMessage = ExpoPushMessage.builder()
                    .to(expoTokens)
                    .title("🚚 Nova Entrega Disponível!")
                    .body(String.format("Entrega de R$ %.2f - %s", value, clientName))
                    .data(notificationData)
                    .sound("default")
                    .priority("high")
                    .channelId("delivery-invites")
                    .displayInForeground(true) // ← Força foreground (serializa como _displayInForeground)
                    .badge(1)
                    .ttl(300) // 5 minutos
                    .build();

            // Envia notificação
            boolean success = sendExpoPushNotification(Collections.singletonList(pushMessage));

            if (success) {
                log.info("Notificação enviada com sucesso para {} tokens de {} motoristas",
                        expoTokens.size(), driverIds.size());
            } else {
                log.error("Falha ao enviar notificação para motoristas: {}", driverIds);
            }

        } catch (Exception e) {
            log.error("Erro ao enviar convite de entrega para motoristas {}: {}", driverIds, e.getMessage(), e);
        }
    }

    /**
     * Envia notificação genérica para um usuário
     */
    public void sendNotificationToUser(UUID userId, String title, String body, Object data) {
        try {
            log.info("Enviando notificação para usuário {}: {}", userId, title);

            List<UserPushToken> tokens = pushTokenService.getActiveTokensByUserId(userId);

            if (tokens.isEmpty()) {
                log.warn("Nenhum token push ativo encontrado para usuário {}", userId);
                return;
            }

            List<String> expoTokens = tokens.stream()
                    .map(UserPushToken::getToken)
                    .filter(this::isValidExpoToken)
                    .collect(Collectors.toList());

            if (expoTokens.isEmpty()) {
                log.warn("Nenhum token Expo válido encontrado para usuário {}", userId);
                return;
            }

            ExpoPushMessage pushMessage = ExpoPushMessage.builder()
                    .to(expoTokens)
                    .title(title)
                    .body(body)
                    .data(data)
                    .sound("default")
                    .priority("normal")
                    .build();

            sendExpoPushNotification(Collections.singletonList(pushMessage));

        } catch (Exception e) {
            log.error("Erro ao enviar notificação para usuário {}: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * Envia notificações via Expo Push API
     */
    private boolean sendExpoPushNotification(List<ExpoPushMessage> messages) {
        try {
            if (expoAccessToken == null || expoAccessToken.trim().isEmpty()) {
                log.warn("Token Expo não configurado. Notificação não será enviada.");
                return false;
            }

            // Modo desenvolvimento: simular envio para tokens de teste
            if (expoAccessToken.contains("development-test-token")) {
                log.info("🧪 MODO DESENVOLVIMENTO: Simulando envio de {} notificações push", messages.size());
                for (ExpoPushMessage message : messages) {
                    log.info("📱 SIMULAÇÃO PUSH: {} -> {}", message.getTitle(), message.getBody());
                    log.info("📱 TOKENS ALVO: {}", message.getTo());
                    log.info("📱 DADOS: {}", message.getData());
                }
                log.info("✅ Notificações push simuladas com sucesso (modo desenvolvimento)");
                return true;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + expoAccessToken);
            headers.set("Accept", "application/json");
            headers.set("Accept-Encoding", "gzip, deflate");

            HttpEntity<List<ExpoPushMessage>> request = new HttpEntity<>(messages, headers);

            log.debug("Enviando notificação para Expo: URL={}, Messages={}", expoPushUrl, messages.size());

            ResponseEntity<String> response = restTemplate.postForEntity(expoPushUrl, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Notificações push enviadas com sucesso: status={}", response.getStatusCode());
                log.debug("Resposta Expo: {}", response.getBody());
                return true;
            } else {
                log.error("Erro ao enviar notificações push. Status: {}, Body: {}",
                        response.getStatusCode(), response.getBody());
                return false;
            }

        } catch (Exception e) {
            log.error("Erro na comunicação com Expo Push API: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Constrói dados da notificação de entrega
     */
    private DeliveryNotificationData buildDeliveryNotificationData(UUID deliveryId, String clientName,
            BigDecimal value, String address,
            Double pickupLat, Double pickupLng,
            Double deliveryLat, Double deliveryLng) {
        return DeliveryNotificationData.builder()
                .type("delivery_invite")
                .deliveryId(deliveryId.toString())
                .message("Nova entrega próxima à sua localização")
                .deliveryData(DeliveryNotificationData.DeliveryData.builder()
                        .clientName(clientName != null ? clientName : "Cliente não informado")
                        .value(value)
                        .address(address)
                        .pickupLatitude(pickupLat)
                        .pickupLongitude(pickupLng)
                        .deliveryLatitude(deliveryLat)
                        .deliveryLongitude(deliveryLng)
                        .estimatedTime("15-30 min")
                        .build())
                .build();
    }

    /**
     * Valida se o token é um token Expo válido
     */
    private boolean isValidExpoToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        // Tokens Expo começam com "ExponentPushToken[" ou "ExpoPushToken["
        return token.startsWith("ExponentPushToken[") || token.startsWith("ExpoPushToken[");
    }

    /**
     * Envia notificação híbrida para um usuário (Expo + Web Push)
     */
    public void sendHybridNotificationToUser(UUID userId, String title, String body, Map<String, Object> data) {
        try {
            log.info("Enviando notificação híbrida para usuário: {}", userId);

            // Busca todos os tokens ativos do usuário
            List<UserPushToken> tokens = pushTokenService.getActiveTokensByUserId(userId);

            if (tokens.isEmpty()) {
                log.warn("Nenhum token push ativo encontrado para usuário {}", userId);
                return;
            }

            int totalSent = 0;

            // Separar tokens por tipo
            List<UserPushToken> mobileTokens = tokens.stream()
                    .filter(token -> token.getDeviceType() == UserPushToken.DeviceType.MOBILE)
                    .toList();

            List<UserPushToken> webTokens = tokens.stream()
                    .filter(token -> token.isWebPush() && token.hasWebPushData())
                    .toList();

            // Enviar para dispositivos móveis (Expo)
            if (!mobileTokens.isEmpty()) {
                List<String> expoTokens = mobileTokens.stream()
                        .map(UserPushToken::getToken)
                        .filter(this::isValidExpoToken)
                        .toList();

                if (!expoTokens.isEmpty()) {
                    ExpoPushMessage expoPushMessage = ExpoPushMessage.builder()
                            .to(expoTokens)
                            .title(title)
                            .body(body)
                            .data(data != null ? data : Collections.emptyMap())
                            .sound("default")
                            .priority("high")
                            .channelId("delivery-invites")
                            .displayInForeground(true) // ← Força foreground (serializa como _displayInForeground)
                            .badge(1)
                            .build();

                    boolean expoSuccess = sendExpoPushNotification(Collections.singletonList(expoPushMessage));
                    if (expoSuccess) {
                        totalSent += expoTokens.size();
                        log.info("Notificação Expo enviada para {} dispositivos móveis", expoTokens.size());
                    }
                }
            }

            // Enviar para dispositivos web (Web Push)
            if (!webTokens.isEmpty()) {
                int webSent = webPushService.sendWebPushNotificationToTokens(webTokens, title, body, data);
                totalSent += webSent;
                log.info("Notificação Web Push enviada para {} dispositivos web", webSent);
            }

            log.info("Notificação híbrida enviada para {} dispositivos do usuário {}", totalSent, userId);

        } catch (Exception e) {
            log.error("Erro ao enviar notificação híbrida para usuário {}: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * Envia notificação híbrida para múltiplos usuários
     */
    public void sendHybridNotificationToUsers(List<UUID> userIds, String title, String body, Map<String, Object> data) {
        try {
            log.info("Enviando notificação híbrida para {} usuários", userIds.size());

            for (UUID userId : userIds) {
                sendHybridNotificationToUser(userId, title, body, data);
            }

        } catch (Exception e) {
            log.error("Erro ao enviar notificação híbrida para usuários: {}", e.getMessage(), e);
        }
    }

    /**
     * Método atualizado para usar envio híbrido
     */
    public void sendNotificationToUser(UUID userId, String title, String body, Map<String, Object> data) {
        // Usar o novo método híbrido por padrão
        sendHybridNotificationToUser(userId, title, body, data);
    }

    /**
     * Método de teste para desenvolvimento (remover em produção)
     */
    public void sendTestNotification(UUID userId) {
        sendNotificationToUser(
                userId,
                "🧪 Teste de Notificação",
                "Esta é uma notificação de teste do sistema MVT",
                Collections.singletonMap("type", "test"));
    }
}