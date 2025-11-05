package com.mvt.mvt_events.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mvt.mvt_events.jpa.UserPushToken;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Serviço específico para envio de Web Push Notifications
 */
@Service
@Slf4j
public class WebPushService {

    @Autowired
    private PushService pushService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Envia notificação Web Push para um token específico
     */
    public boolean sendWebPushNotification(UserPushToken token, String title, String body, Map<String, Object> data) {
        try {
            if (!token.isWebPush() || !token.hasWebPushData()) {
                log.warn("Token não é Web Push ou não tem dados completos: {}", token.getId());
                return false;
            }

            // FORÇAR MODO PRODUÇÃO: Sempre tentar enviar Web Push real
            log.info("🚀 MODO PRODUÇÃO FORÇADO: Enviando Web Push REAL");
            log.info("📱 TÍTULO: {}", title);
            log.info("📱 MENSAGEM: {}", body);
            log.info("📱 ENDPOINT: {}", token.getWebEndpoint());
            log.info("📱 DADOS: {}", data);

            // Criar subscription do Web Push
            Subscription subscription = new Subscription(
                    token.getWebEndpoint(),
                    new Subscription.Keys(token.getWebP256dh(), token.getWebAuth()));

            // Montar payload da notificação
            Map<String, Object> payload = new HashMap<>();
            payload.put("title", title);
            payload.put("body", body);
            payload.put("icon", "/icon-192x192.png"); // Ícone padrão
            payload.put("badge", "/badge-72x72.png"); // Badge padrão
            payload.put("tag", "mvt-delivery"); // Tag para agrupamento
            payload.put("requireInteraction", true); // Notificação persistente

            if (data != null && !data.isEmpty()) {
                payload.put("data", data);
            }

            // Adicionar ações (botões) se for notificação de entrega
            if (data != null && "delivery_invite".equals(data.get("type"))) {
                payload.put("actions", List.of(
                        Map.of("action", "accept", "title", "Aceitar", "icon", "/accept-icon.png"),
                        Map.of("action", "reject", "title", "Recusar", "icon", "/reject-icon.png")));
            }

            String payloadJson = objectMapper.writeValueAsString(payload);

            // Criar notificação
            Notification notification = new Notification(subscription, payloadJson);

            // Enviar notificação
            log.info("🔄 Enviando para FCM endpoint: {}", token.getWebEndpoint());
            log.info("📦 Payload: {}", payloadJson);

            pushService.send(notification);

            log.info("✅ Web Push enviado para FCM (token {})", token.getId());
            return true;

        } catch (Exception e) {
            log.error("Erro ao enviar Web Push para token {}: {}", token.getId(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Envia notificação Web Push para múltiplos tokens
     */
    public int sendWebPushNotificationToTokens(List<UserPushToken> tokens, String title, String body,
            Map<String, Object> data) {
        int successCount = 0;

        for (UserPushToken token : tokens) {
            if (sendWebPushNotification(token, title, body, data)) {
                successCount++;
            }
        }

        log.info("Web Push enviado para {}/{} tokens", successCount, tokens.size());
        return successCount;
    }

    /**
     * Valida se os dados de Web Push estão completos
     */
    public boolean isValidWebPushToken(UserPushToken token) {
        return token != null &&
                token.isWebPush() &&
                token.getWebEndpoint() != null &&
                !token.getWebEndpoint().trim().isEmpty() &&
                token.getWebP256dh() != null &&
                !token.getWebP256dh().trim().isEmpty() &&
                token.getWebAuth() != null &&
                !token.getWebAuth().trim().isEmpty();
    }

    /**
     * Detecta se estamos em modo de desenvolvimento
     * Pode ser controlado por variável de ambiente
     */
    private boolean isDevelopmentMode() {
        // Verificar se há VAPID keys configuradas adequadamente
        try {
            boolean hasValidVapid = pushService != null &&
                    pushService.getPublicKey() != null;

            // Se tiver VAPID keys válidas, usar modo produção
            return !hasValidVapid;
        } catch (Exception e) {
            log.debug("Erro ao verificar VAPID keys: {}", e.getMessage());
            return true; // Default para desenvolvimento se houver erro
        }
    }
}