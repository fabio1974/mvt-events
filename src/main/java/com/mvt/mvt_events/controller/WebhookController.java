package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.payment.dto.PagarMeWebhookEvent;
import com.mvt.mvt_events.payment.service.PagarMeService;
import com.mvt.mvt_events.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller REST para receber webhooks do Pagar.me
 * 
 * <p>Endpoints disponíveis:</p>
 * <ul>
 *   <li>POST /api/webhooks/pagarme - Receber notificações de eventos Pagar.me</li>
 * </ul>
 * 
 * <p><strong>Autenticação:</strong> Validação via HMAC SHA256</p>
 * 
 * <p><strong>Eventos suportados:</strong></p>
 * <ul>
 *   <li>order.paid - Pagamento confirmado</li>
 *   <li>order.payment_failed - Pagamento falhou</li>
 *   <li>order.canceled - Pedido cancelado → expira PIX, reverte delivery para PENDING</li>
 *   <li>charge.expired - QR Code PIX expirou → expira PIX, reverte delivery para PENDING</li>
 *   <li>charge.underpaid - PIX com valor insuficiente → trata como expirado</li>
 * </ul>
 * 
 * <p><strong>Fluxo de webhook:</strong></p>
 * <ol>
 *   <li>Cliente paga order PIX no app do banco</li>
 *   <li>Pagar.me detecta pagamento confirmado</li>
 *   <li>Pagar.me envia webhook para este endpoint</li>
 *   <li>Sistema valida signature HMAC SHA256</li>
 *   <li>Sistema marca pagamento como COMPLETED</li>
 *   <li>Sistema libera entrega (se aplicável)</li>
 * </ol>
 * 
 * @see PaymentService
 * @see PagarMeService
 * @see <a href="https://docs.pagar.me/reference/webhooks">Documentação Webhooks Pagar.me</a>
 */
@Slf4j
@RestController
@RequestMapping("/api/webhooks/pagarme")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "Recebimento de notificações do Pagar.me")
public class WebhookController {

    private final PaymentService paymentService;
    private final PagarMeService pagarMeService;

    /**
     * Recebe webhooks do Pagar.me sobre eventos de pagamento
     * 
     * @param signature Signature do header X-Hub-Signature
     * @param payload String do payload para validação
     * @param event Evento deserializado
     * @return ResponseEntity com status do processamento
     */
    @PostMapping
    @Operation(
            summary = "Receber webhook Pagar.me",
            description = "Endpoint para receber notificações de eventos do Pagar.me (pagamentos confirmados, falhas, cancelamentos, etc)"
    )
    public ResponseEntity<?> receiveWebhook(
            @RequestHeader(value = "X-Hub-Signature", required = false) String signature,
            @RequestBody String payload,
            @RequestBody PagarMeWebhookEvent event
    ) {
        log.info("🔔 Webhook recebido do Pagar.me");
        log.info("📋 Event type: {}", event.getType());

        try {
            // Validar signature
            if (signature == null || !pagarMeService.validateWebhookSignature(payload, signature)) {
                log.warn("⚠️ Webhook com signature inválida");
                return ResponseEntity.status(401).body(Map.of(
                        "error", "INVALID_SIGNATURE",
                        "message", "Webhook signature inválida"
                ));
            }

            // Processar evento
            pagarMeService.processWebhookEvent(event);

            String orderId = event.getData().getId();

            // Processar evento por tipo
            switch (event.getType()) {
                case "order.paid":
                    log.info("💰 Pagamento confirmado para order: {}", orderId);
                    paymentService.processPaymentConfirmation(orderId);
                    break;

                case "charge.underpaid":
                case "charge.expired":
                case "order.canceled":
                    log.info("⏰ PIX expirado/cancelado para order: {} (evento: {})", orderId, event.getType());
                    paymentService.processPaymentExpiration(orderId);
                    break;

                default:
                    log.info("ℹ️ Evento não tratado: {} (order: {})", event.getType(), orderId);
                    break;
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Webhook processado com sucesso",
                    "eventType", event.getType()
            ));

        } catch (IllegalArgumentException e) {
            log.error("❌ Order não encontrada: {}", e.getMessage());
            return ResponseEntity.status(404).body(Map.of(
                    "error", "ORDER_NOT_FOUND",
                    "message", e.getMessage()
            ));

        } catch (Exception e) {
            log.error("❌ Erro ao processar webhook", e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "PROCESSING_ERROR",
                    "message", "Erro ao processar webhook: " + e.getMessage()
            ));
        }
    }

    /**
     * Health check do controller
     */
    @GetMapping("/health")
    @Operation(
            summary = "Health check",
            description = "Verifica se o controller de webhooks está funcionando"
    )
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "WebhookController",
                "message", "✅ Controller de webhooks operacional"
        ));
    }
}
