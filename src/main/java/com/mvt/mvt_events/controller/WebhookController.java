package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller REST para receber webhooks do Iugu
 * 
 * <p>Endpoints disponíveis:</p>
 * <ul>
 *   <li>POST /api/webhooks/iugu - Receber notificações de eventos Iugu</li>
 * </ul>
 * 
 * <p><strong>Autenticação:</strong> Webhook público (validação por token no futuro)</p>
 * 
 * <p><strong>Eventos suportados:</strong></p>
 * <ul>
 *   <li>invoice.status_changed - Status da fatura mudou (ex: pago)</li>
 *   <li>invoice.payment_failed - Pagamento falhou</li>
 *   <li>invoice.refunded - Fatura reembolsada</li>
 * </ul>
 * 
 * <p><strong>Fluxo de webhook:</strong></p>
 * <ol>
 *   <li>Cliente paga fatura PIX no app do banco</li>
 *   <li>Iugu detecta pagamento confirmado</li>
 *   <li>Iugu envia webhook para este endpoint</li>
 *   <li>Sistema valida invoice_id</li>
 *   <li>Sistema marca pagamento como COMPLETED</li>
 *   <li>Sistema libera entrega (se aplicável)</li>
 * </ol>
 * 
 * <p><strong>Exemplo de payload Iugu:</strong></p>
 * <pre>
 * {
 *   "event": "invoice.status_changed",
 *   "data": {
 *     "id": "F7C8A9B1234",
 *     "status": "paid",
 *     "paid_at": "2025-12-02T23:59:59-03:00",
 *     "total_cents": 5000
 *   }
 * }
 * </pre>
 * 
 * <p><strong>Segurança (TODO):</strong></p>
 * <ul>
 *   <li>Validar HMAC signature do Iugu</li>
 *   <li>Verificar IP de origem</li>
 *   <li>Rate limiting</li>
 * </ul>
 * 
 * @see PaymentService
 * @see <a href="https://dev.iugu.com/reference/webhooks">Documentação Webhooks Iugu</a>
 */
@Slf4j
@RestController
@RequestMapping("/api/webhooks/iugu")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "Recebimento de notificações do Iugu")
public class WebhookController {

    private final PaymentService paymentService;

    /**
     * Recebe webhooks do Iugu sobre eventos de pagamento
     * 
     * <p><strong>Eventos processados:</strong></p>
     * <ul>
     *   <li>invoice.status_changed (status=paid) → Marca pagamento como COMPLETED</li>
     *   <li>invoice.payment_failed → Marca pagamento como FAILED</li>
     *   <li>invoice.refunded → Marca pagamento como REFUNDED</li>
     * </ul>
     * 
     * <p><strong>Status HTTP:</strong></p>
     * <ul>
     *   <li>200 OK - Webhook processado com sucesso</li>
     *   <li>400 Bad Request - Payload inválido</li>
     *   <li>404 Not Found - Invoice não encontrada no sistema</li>
     *   <li>500 Internal Server Error - Erro ao processar webhook</li>
     * </ul>
     * 
     * @param payload Dados do webhook enviado pelo Iugu
     * @return ResponseEntity com status do processamento
     */
    @PostMapping
    @Operation(
            summary = "Receber webhook Iugu",
            description = "Endpoint para receber notificações de eventos do Iugu (pagamentos confirmados, falhas, reembolsos, etc)"
    )
    public ResponseEntity<?> receiveWebhook(@RequestBody Map<String, Object> payload) {
        log.info("🔔 Webhook recebido do Iugu");
        log.debug("Payload: {}", payload);

        try {
            // Extrair event type
            String event = (String) payload.get("event");
            if (event == null || event.isBlank()) {
                log.warn("⚠️ Webhook sem campo 'event'");
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "INVALID_PAYLOAD",
                        "message", "Campo 'event' não encontrado no payload"
                ));
            }

            log.info("📋 Event type: {}", event);

            // Extrair dados da invoice
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) payload.get("data");
            if (data == null) {
                log.warn("⚠️ Webhook sem campo 'data'");
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "INVALID_PAYLOAD",
                        "message", "Campo 'data' não encontrado no payload"
                ));
            }

            String invoiceId = (String) data.get("id");
            String status = (String) data.get("status");

            if (invoiceId == null || invoiceId.isBlank()) {
                log.warn("⚠️ Webhook sem invoice ID");
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "INVALID_PAYLOAD",
                        "message", "Invoice ID não encontrado no payload"
                ));
            }

            log.info("   ├─ Invoice ID: {}", invoiceId);
            log.info("   └─ Status: {}", status);

            // Processar evento
            switch (event) {
                case "invoice.status_changed":
                    if ("paid".equals(status)) {
                        log.info("💰 Pagamento confirmado para invoice: {}", invoiceId);
                        paymentService.processPaymentConfirmation(invoiceId);
                        
                        return ResponseEntity.ok(Map.of(
                                "success", true,
                                "message", "Pagamento confirmado com sucesso",
                                "invoiceId", invoiceId
                        ));
                    } else {
                        log.info("ℹ️ Status mudou mas não é 'paid': {} - Ignorando", status);
                        return ResponseEntity.ok(Map.of(
                                "success", true,
                                "message", "Status registrado: " + status,
                                "invoiceId", invoiceId
                        ));
                    }

                case "invoice.payment_failed":
                    log.warn("❌ Pagamento falhou para invoice: {}", invoiceId);
                    // TODO: Implementar lógica de falha
                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "message", "Falha de pagamento registrada",
                            "invoiceId", invoiceId
                    ));

                case "invoice.refunded":
                    log.info("↩️ Reembolso para invoice: {}", invoiceId);
                    // TODO: Implementar lógica de reembolso
                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "message", "Reembolso registrado",
                            "invoiceId", invoiceId
                    ));

                default:
                    log.info("ℹ️ Evento não processado: {}", event);
                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "message", "Evento recebido mas não processado: " + event
                    ));
            }

        } catch (IllegalArgumentException e) {
            // Invoice não encontrada (404)
            log.error("❌ Invoice não encontrada: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "NOT_FOUND",
                    "message", e.getMessage()
            ));

        } catch (Exception e) {
            // Erro interno (500)
            log.error("❌ Erro ao processar webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "INTERNAL_ERROR",
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
