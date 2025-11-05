package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.service.PushNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * Controller para testar notificações push no desenvolvimento
 * APENAS PARA DESENVOLVIMENTO - REMOVER EM PRODUÇÃO
 */
@RestController
@RequestMapping("/api/push-test")
@CrossOrigin(origins = "*")
@Slf4j
public class PushTestController {

    @Autowired
    private PushNotificationService pushNotificationService;

    /**
     * Endpoint para testar notificação híbrida para um usuário específico
     */
    @PostMapping("/send/{userId}")
    public ResponseEntity<?> testSendToUser(@PathVariable String userId) {
        try {
            UUID userUuid = UUID.fromString(userId);

            Map<String, Object> testData = Map.of(
                    "type", "test_notification",
                    "message", "Teste de notificação do sistema MVT",
                    "timestamp", System.currentTimeMillis());

            pushNotificationService.sendHybridNotificationToUser(
                    userUuid,
                    "🧪 Teste de Notificação",
                    "Sistema de push híbrido funcionando no desenvolvimento!",
                    testData);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Notificação teste enviada para usuário " + userId,
                    "type", "hybrid",
                    "timestamp", System.currentTimeMillis()));

        } catch (Exception e) {
            log.error("Erro ao enviar notificação teste: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    /**
     * Endpoint para simular criação de entrega (dispara sistema de 3 níveis)
     */
    @PostMapping("/simulate-delivery")
    public ResponseEntity<?> simulateDeliveryCreation(
            @RequestParam(defaultValue = "123") String deliveryId,
            @RequestParam(defaultValue = "João Silva") String clientName,
            @RequestParam(defaultValue = "25.50") String value) {

        try {
            log.info("🚚 SIMULANDO CRIAÇÃO DE ENTREGA: ID={}, Cliente={}, Valor=R${}",
                    deliveryId, clientName, value);

            // Esta seria a simulação - na prática, o sistema real faria:
            // 1. Salvar delivery no banco
            // 2. Disparar deliveryNotificationService.notifyAvailableDrivers()
            // 3. Sistema de 3 níveis rodaria automaticamente

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Simulação de entrega criada",
                    "deliveryId", deliveryId,
                    "clientName", clientName,
                    "value", value,
                    "info", "No sistema real, isso dispararia o algoritmo de 3 níveis automaticamente"));

        } catch (Exception e) {
            log.error("Erro na simulação: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    /**
     * Endpoint para verificar configuração do sistema push
     */
    @GetMapping("/status")
    public ResponseEntity<?> getPushStatus() {
        return ResponseEntity.ok(Map.of(
                "pushSystem", "hybrid",
                "expo", "development-mode-enabled",
                "webPush", "configured-with-bouncy-castle",
                "algorithm", "3-levels-active",
                "developmentMode", true,
                "info", "Sistema híbrido configurado para desenvolvimento local"));
    }

    /**
     * Endpoint para simular Web Push notification no browser
     */
    @PostMapping("/web-push/{userId}")
    public ResponseEntity<?> testWebPushToUser(@PathVariable String userId) {
        try {
            UUID userUuid = UUID.fromString(userId);

            log.info("🌐 TESTE WEB PUSH: Simulando notificação para usuário {} no browser", userId);

            Map<String, Object> testData = Map.of(
                    "type", "test_web_push",
                    "message", "Teste de Web Push notification",
                    "url", "/dashboard/deliveries",
                    "timestamp", System.currentTimeMillis());

            pushNotificationService.sendHybridNotificationToUser(
                    userUuid,
                    "🌐 Teste Web Push",
                    "Notificação de teste para seu browser! Você está logado como motoboy.",
                    testData);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Web Push simulado para usuário " + userId + " no browser",
                    "type", "web-push-simulation",
                    "info", "Verifique os logs para detalhes da simulação",
                    "timestamp", System.currentTimeMillis()));

        } catch (Exception e) {
            log.error("Erro ao enviar Web Push teste: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    /**
     * Endpoint para simular criação de entrega e notificar motoboys próximos
     * TESTE SEM AUTENTICAÇÃO - APENAS DESENVOLVIMENTO
     */
    @PostMapping("/simulate-delivery-notification")
    public ResponseEntity<?> simulateDeliveryNotification(
            @RequestParam(defaultValue = "6008534c-fe16-4d69-8bb7-d54745a3c980") String motoboyId) {

        try {
            UUID motoboyUuid = UUID.fromString(motoboyId);

            log.info("🚚 SIMULANDO NOTIFICAÇÃO DE ENTREGA para motoboy: {}", motoboyId);

            // Dados de uma entrega fictícia
            Map<String, Object> deliveryData = Map.of(
                    "type", "delivery_invite",
                    "deliveryId", "TEST-123",
                    "level", "Teste Manual",
                    "clientName", "Cliente Teste",
                    "value", "25.50",
                    "address", "Rua Teste, 123",
                    "pickupLatitude", "-23.550520",
                    "pickupLongitude", "-46.633308",
                    "deliveryLatitude", "-23.560520",
                    "deliveryLongitude", "-46.643308");

            // Enviar notificação híbrida
            pushNotificationService.sendHybridNotificationToUser(
                    motoboyUuid,
                    "🚚 Nova Entrega Disponível! (TESTE)",
                    "Entrega de R$ 25,50 - Cliente Teste (Simulação)",
                    deliveryData);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Notificação de entrega simulada para motoboy " + motoboyId,
                    "deliveryId", "TEST-123",
                    "info", "Verifique os logs e seu browser para a notificação Web Push",
                    "timestamp", System.currentTimeMillis()));

        } catch (Exception e) {
            log.error("Erro na simulação de entrega: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }
}