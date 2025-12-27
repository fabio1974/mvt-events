package com.mvt.mvt_events.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mvt.mvt_events.jpa.Payment;
import com.mvt.mvt_events.jpa.PaymentStatus;
import com.mvt.mvt_events.payment.service.PagarMeService;
import com.mvt.mvt_events.repository.PaymentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Controller para receber webhooks do Pagar.me sobre mudanças de status de orders (pagamentos).
 * 
 * <p><strong>URL para configurar no Pagar.me:</strong></p>
 * <pre>
 * Produção: https://seu-dominio.com/api/webhooks/order
 * </pre>
 * 
 * <p><strong>Eventos suportados:</strong></p>
 * <ul>
 *   <li>order.paid - Pagamento confirmado → COMPLETED</li>
 *   <li>order.payment_failed - Pagamento falhou → FAILED</li>
 *   <li>order.canceled - Pedido cancelado → CANCELLED</li>
 *   <li>order.pending - Aguardando pagamento → PENDING</li>
 * </ul>
 * 
 * <p><strong>Segurança:</strong></p>
 * <ul>
 *   <li>Validação HMAC SHA256 via header X-Hub-Signature</li>
 *   <li>Secret configurado em application.yml</li>
 *   <li>Endpoint público (sem autenticação JWT)</li>
 * </ul>
 * 
 * @see <a href="https://docs.pagar.me/reference/webhooks">Documentação Webhooks Pagar.me</a>
 */
@Slf4j
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "Recebimento de notificações de mudança de status")
public class OrderWebhookController {

    private final PaymentRepository paymentRepository;
    private final PagarMeService pagarMeService;
    private final ObjectMapper objectMapper;

    /**
     * Recebe webhooks do Pagar.me sobre mudanças de status de orders (payments).
     * 
     * Este endpoint é chamado automaticamente pelo Pagar.me quando o status de uma order muda.
     * 
     * @param signature Assinatura HMAC SHA256 do header X-Hub-Signature
     * @param payload Payload JSON completo do webhook
     * @return ResponseEntity com resultado do processamento
     */
    @PostMapping("/order")
    @Transactional
    @Operation(
            summary = "Receber webhook de mudança de status de order",
            description = "Endpoint chamado pelo Pagar.me quando o status de uma order (payment) muda. " +
                    "Atualiza automaticamente o status do pagamento no banco de dados."
    )
    public ResponseEntity<?> handleOrderWebhook(
            @RequestHeader(value = "X-Hub-Signature", required = false) String signature,
            @RequestBody String payload
    ) {
log.info("🔔 Webhook recebido em /api/webhooks/order");
        
        try {
            // 1. Parse do payload
            JsonNode webhookData = objectMapper.readTree(payload);
            String eventType = webhookData.path("type").asText();
            String eventId = webhookData.path("id").asText();
            
            log.info("📋 Event ID: {}", eventId);
            log.info("📋 Event Type: {}", eventType);
            log.info("📋 Payload: {}", payload);
            
            // 2. Validar signature (se configurado)
            if (signature != null && !signature.isBlank()) {
                if (!pagarMeService.validateWebhookSignature(payload, signature)) {
                    log.warn("⚠️ Webhook com signature inválida");
                    return ResponseEntity.status(401).body(Map.of(
                            "error", "INVALID_SIGNATURE",
                            "message", "Webhook signature inválida"
                    ));
                }
                log.info("✅ Signature validada com sucesso");
            } else {
                log.warn("⚠️ Webhook recebido SEM signature (modo development?)");
            }
            
            // 3. Extrair dados da order
            JsonNode dataNode = webhookData.path("data");
            String orderId = dataNode.path("id").asText();
            String orderStatus = dataNode.path("status").asText();
            
            if (orderId == null || orderId.isBlank()) {
                log.error("❌ Order ID não encontrado no payload");
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "INVALID_PAYLOAD",
                        "message", "Order ID não encontrado no payload"
                ));
            }
            
            log.info("💳 Order ID: {}", orderId);
            log.info("📊 Order Status: {}", orderStatus);
            
            // 4. Buscar payment no banco pelo provider_payment_id
            Payment payment = paymentRepository.findByProviderPaymentId(orderId)
                    .orElse(null);
            
            if (payment == null) {
                log.warn("⚠️ Payment não encontrado para Order ID: {}", orderId);
                // Retornar 200 OK mesmo assim para não causar retry no Pagar.me
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Webhook recebido mas payment não encontrado (pode ser order de teste)",
                        "orderId", orderId
                ));
            }
            
            log.info("✅ Payment encontrado: ID={}, Status Atual={}", payment.getId(), payment.getStatus());
            
            // 5. Mapear status do Pagar.me para PaymentStatus
            PaymentStatus newStatus = mapEventTypeToPaymentStatus(eventType, orderStatus);
            PaymentStatus oldStatus = payment.getStatus();
            
            log.info("🔄 Mudança de status: {} → {}", oldStatus, newStatus);
            
            // 6. Atualizar status do payment
            payment.setStatus(newStatus);
            
            // Se for pagamento confirmado, registrar data de pagamento
            if (newStatus == PaymentStatus.PAID && payment.getPaymentDate() == null) {
                payment.setPaymentDate(LocalDateTime.now());
                log.info("💰 Data de pagamento registrada: {}", payment.getPaymentDate());
            }
            
            // Salvar alterações
            paymentRepository.save(payment);
            
            log.info("✅ Payment #{} atualizado com sucesso: {} → {}", 
                    payment.getId(), oldStatus, newStatus);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Webhook processado com sucesso",
                    "paymentId", payment.getId(),
                    "orderId", orderId,
                    "eventType", eventType,
                    "oldStatus", oldStatus.name(),
                    "newStatus", newStatus.name()
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
     * Mapeia tipo de evento + status da order para PaymentStatus.
     * 
     * Eventos Pagar.me:
     * - order.created → PENDING
     * - order.paid → COMPLETED
     * - order.payment_failed → FAILED
     * - order.canceled → CANCELLED
     * - order.pending → PENDING
     * 
     * @param eventType Tipo do evento (order.paid, order.payment_failed, etc)
     * @param orderStatus Status da order (paid, failed, canceled, etc)
     * @return PaymentStatus correspondente
     */
    private PaymentStatus mapEventTypeToPaymentStatus(String eventType, String orderStatus) {
        // Priorizar event type
        if (eventType != null) {
            switch (eventType.toLowerCase()) {
                case "order.paid":
                    return PaymentStatus.COMPLETED;
                case "order.payment_failed":
                    return PaymentStatus.FAILED;
                case "order.canceled":
                case "order.cancelled":
                    return PaymentStatus.CANCELLED;
                case "order.pending":
                    return PaymentStatus.PENDING;
                case "order.created":
                    return PaymentStatus.PENDING;
            }
        }
        
        // Fallback para order status
        if (orderStatus != null) {
            switch (orderStatus.toLowerCase()) {
                case "paid":
                    return PaymentStatus.COMPLETED;
                case "failed":
                    return PaymentStatus.FAILED;
                case "canceled":
                case "cancelled":
                    return PaymentStatus.CANCELLED;
                case "pending":
                    return PaymentStatus.PENDING;
                case "processing":
                    return PaymentStatus.PROCESSING;
            }
        }
        
        log.warn("⚠️ Status desconhecido: eventType={}, orderStatus={}", eventType, orderStatus);
        return PaymentStatus.PENDING;
    }
    
    /**
     * Health check do webhook
     */
    @GetMapping("/order/health")
    @Operation(
            summary = "Health check do webhook",
            description = "Verifica se o endpoint de webhooks está funcionando"
    )
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "endpoint", "/api/webhooks/order",
                "message", "✅ Webhook endpoint operacional",
                "info", "Configure esta URL no painel do Pagar.me"
        ));
    }
}
