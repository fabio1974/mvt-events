package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.dto.push.PushTokenResponse;
import com.mvt.mvt_events.dto.push.RegisterPushTokenRequest;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.repository.UserRepository;
import com.mvt.mvt_events.service.PushNotificationService;
import com.mvt.mvt_events.service.UserPushTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Controller para gerenciamento de notificações push
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
@Tag(name = "Push Notifications", description = "Gerenciamento de tokens e notificações push")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class PushNotificationController {

    @Autowired
    private UserPushTokenService pushTokenService;

    @Autowired
    private PushNotificationService pushNotificationService;

    @Autowired
    private UserRepository userRepository;

    /**
     * POST /api/users/push-token
     * Registra token push para notificações
     */
    @PostMapping("/push-token")
    @Operation(summary = "Registrar token push", description = "Registra token do dispositivo para receber notificações push")
    public ResponseEntity<PushTokenResponse> registerPushToken(
            @Valid @RequestBody RegisterPushTokenRequest request,
            Authentication authentication) {

        try {
            log.info("Registrando token push para usuário: {}", authentication.getName());

            UUID userId = getUserIdFromAuthentication(authentication);
            PushTokenResponse response = pushTokenService.registerPushToken(userId, request);

            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            log.error("Erro no endpoint de registro de token push: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(PushTokenResponse.builder()
                            .success(false)
                            .message("Erro interno do servidor")
                            .build());
        }
    }

    /**
     * DELETE /api/users/push-token
     * Remove token push específico
     */
    @DeleteMapping("/push-token")
    @Operation(summary = "Remover token push", description = "Remove token específico para parar de receber notificações")
    public ResponseEntity<PushTokenResponse> unregisterPushToken(
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        try {
            log.info("Removendo token push para usuário: {}", authentication.getName());

            UUID userId = getUserIdFromAuthentication(authentication);
            String token = request.get("token");

            if (token == null || token.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(PushTokenResponse.builder()
                                .success(false)
                                .message("Token é obrigatório")
                                .build());
            }

            PushTokenResponse response = pushTokenService.unregisterPushToken(userId, token);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erro no endpoint de remoção de token push: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(PushTokenResponse.builder()
                            .success(false)
                            .message("Erro interno do servidor")
                            .build());
        }
    }

    /**
     * DELETE /api/users/push-tokens/all
     * Remove todos os tokens de um usuário (útil para logout)
     */
    @DeleteMapping("/push-tokens/all")
    @Operation(summary = "Remover todos os tokens", description = "Remove todos os tokens push do usuário (útil para logout)")
    public ResponseEntity<PushTokenResponse> unregisterAllTokens(Authentication authentication) {

        try {
            log.info("Removendo todos os tokens para usuário: {}", authentication.getName());

            UUID userId = getUserIdFromAuthentication(authentication);
            PushTokenResponse response = pushTokenService.unregisterAllUserTokens(userId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erro no endpoint de remoção de todos os tokens: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(PushTokenResponse.builder()
                            .success(false)
                            .message("Erro interno do servidor")
                            .build());
        }
    }

    /**
     * GET /api/users/push-tokens/count
     * Retorna quantidade de tokens ativos do usuário
     */
    @GetMapping("/push-tokens/count")
    @Operation(summary = "Contar tokens ativos", description = "Retorna a quantidade de tokens push ativos do usuário")
    public ResponseEntity<Map<String, Object>> getTokenCount(Authentication authentication) {

        try {
            UUID userId = getUserIdFromAuthentication(authentication);
            long count = pushTokenService.countActiveTokensByUserId(userId);

            return ResponseEntity.ok(Map.of(
                    "userId", userId,
                    "activeTokens", count,
                    "success", true));

        } catch (Exception e) {
            log.error("Erro ao contar tokens: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Erro interno do servidor"));
        }
    }

    /**
     * POST /api/users/{userId}/test-notification
     * Endpoint de teste para enviar notificação (apenas para desenvolvimento)
     */
    @PostMapping("/{userId}/test-notification")
    @Operation(summary = "Enviar notificação de teste", description = "Endpoint para teste durante desenvolvimento")
    public ResponseEntity<Map<String, Object>> sendTestNotification(
            @PathVariable UUID userId,
            @RequestBody(required = false) Map<String, String> request,
            Authentication authentication) {

        try {
            // Verificar se é o próprio usuário ou um admin
            UUID currentUserId = getUserIdFromAuthentication(authentication);
            if (!currentUserId.equals(userId)) {
                User currentUser = userRepository.findByUsername(authentication.getName())
                        .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

                if (!currentUser.isAdmin()) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of(
                                    "success", false,
                                    "message", "Sem permissão para enviar notificação para este usuário"));
                }
            }

            String title = request != null ? request.getOrDefault("title", "🧪 Teste MVT") : "🧪 Teste MVT";
            String body = request != null ? request.getOrDefault("body", "Notificação de teste do sistema")
                    : "Notificação de teste do sistema";

            pushNotificationService.sendNotificationToUser(userId, title, body, Map.of("type", "test"));

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Notificação de teste enviada",
                    "userId", userId));

        } catch (Exception e) {
            log.error("Erro ao enviar notificação de teste: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Erro interno do servidor"));
        }
    }

    /**
     * Helper para extrair userId do Authentication
     */
    private UUID getUserIdFromAuthentication(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByUsername(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + email));
        return user.getId();
    }
}