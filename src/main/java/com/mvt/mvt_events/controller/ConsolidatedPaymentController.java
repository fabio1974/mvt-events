package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.dto.ConsolidatedInvoiceResponse;
import com.mvt.mvt_events.dto.CreateInvoiceRequest;
import com.mvt.mvt_events.payment.service.ConsolidatedPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller para criação de invoices consolidadas (múltiplas deliveries, 1 invoice)
 * 
 * <p>Permite pagamento de várias deliveries em uma única transação PIX,
 * com split automático entre motoboys/gerentes.</p>
 * 
 * @see ConsolidatedPaymentService
 */
@Slf4j
@RestController
@RequestMapping("/api/payment")
@Tag(name = "Payment", description = "Endpoints de pagamento consolidado com PIX")
@RequiredArgsConstructor
public class ConsolidatedPaymentController {

    private final ConsolidatedPaymentService consolidatedPaymentService;

    /**
     * POST /api/payment/create-invoice
     * 
     * <p>Cria uma invoice PIX consolidada para múltiplas deliveries</p>
     * 
     * <p><strong>Request:</strong></p>
     * <pre>
     * {
     *   "deliveryIds": [1, 2, 3],
     *   "clientEmail": "cliente@example.com",
     *   "expirationHours": 24
     * }
     * </pre>
     * 
     * <p><strong>Response:</strong></p>
     * <pre>
     * {
     *   "paymentId": 123,
     *   "iuguInvoiceId": "ABC123DEF456",
     *   "pixQrCode": "00020126580014br.gov.bcb.pix...",  ← Copiar/colar
     *   "pixQrCodeUrl": "https://faturas.iugu.com/qr/...",  ← URL da imagem
     *   "secureUrl": "https://faturas.iugu.com/ABC123DEF456",  ← Abrir no navegador
     *   "amount": 100.00,
     *   "deliveryCount": 3,
     *   "splits": {
     *     "couriersCount": 2,
     *     "managersCount": 2,
     *     "couriersAmount": 87.00,
     *     "managersAmount": 5.00,
     *     "platformAmount": 8.00,
     *     "recipients": {
     *       "COURIER - João Silva": 50.00,
     *       "COURIER - Maria Santos": 37.00,
     *       "MANAGER - Pedro Costa": 3.00,
     *       "MANAGER - Ana Souza": 2.00,
     *       "Plataforma": 8.00
     *     }
     *   },
     *   "status": "PENDING",
     *   "expiresAt": "2025-12-05T19:00:00",
     *   "statusMessage": "⏳ Aguardando pagamento. Escaneie o QR Code PIX ou copie o código.",
     *   "expired": false
     * }
     * </pre>
     * 
     * @param request Dados da invoice (deliveryIds, clientEmail, expirationHours)
     * @return Response com QR Code PIX e detalhes dos splits
     */
    @Operation(summary = "Criar invoice consolidada", 
               description = "Cria uma invoice PIX para múltiplas deliveries com splits automáticos")
    @PostMapping("/create-invoice")
    @PreAuthorize("hasAnyRole('CLIENT', 'COURIER', 'ORGANIZER', 'ADMIN')")
    public ResponseEntity<ConsolidatedInvoiceResponse> createConsolidatedInvoice(
            @Valid @RequestBody CreateInvoiceRequest request
    ) {
        // Log detalhado do request para validação
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("📨 REQUEST RECEBIDO - Invoice Consolidada");
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("📦 Delivery IDs: {}", request.getDeliveryIds());
        log.info("📧 Client Email: {}", request.getClientEmail());
        log.info("👤 Client Name: {}", request.getClientName() != null ? request.getClientName() : "(não informado)");
        log.info("⏰ Expiration Hours: {}", request.getExpirationHours() != null ? request.getExpirationHours() : "24 (padrão)");
        log.info("───────────────────────────────────────────────────────────────");
        
        // Log do cURL equivalente para facilitar testes
        String curlCommand = buildCurlCommand(request);
        log.info("🔧 cURL equivalente:");
        log.info("{}", curlCommand);
        log.info("═══════════════════════════════════════════════════════════════");

        ConsolidatedInvoiceResponse response = consolidatedPaymentService.createConsolidatedInvoice(
                request.getDeliveryIds(),
                request.getClientEmail(),
                request.getClientName(),
                request.getExpirationHours()
        );

        // Log do resultado
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("✅ INVOICE CRIADA COM SUCESSO");
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("💳 Payment ID: {}", response.getPaymentId());
        log.info("🆔 Iugu Invoice ID: {}", response.getIuguInvoiceId());
        log.info("💰 Valor Total: R$ {}", response.getAmount());
        log.info("📦 Deliveries: {}", response.getDeliveryCount());
        log.info("───────────────────────────────────────────────────────────────");
        log.info("👨‍🚀 Motoboys ({} pessoa(s)): R$ {}", 
                response.getSplits().getCouriersCount(), 
                response.getSplits().getCouriersAmount());
        log.info("👔 Gerentes ({} pessoa(s)): R$ {}", 
                response.getSplits().getManagersCount(), 
                response.getSplits().getManagersAmount());
        log.info("🏢 Plataforma: R$ {}", response.getSplits().getPlatformAmount());
        log.info("───────────────────────────────────────────────────────────────");
        log.info("🔗 QR Code URL: {}", response.getPixQrCodeUrl());
        log.info("🌐 Secure URL: {}", response.getSecureUrl());
        log.info("⏰ Expira em: {}", response.getExpiresAt());
        log.info("═══════════════════════════════════════════════════════════════");

        return ResponseEntity.ok(response);
    }

    /**
     * Constrói comando cURL equivalente para facilitar testes
     */
    private String buildCurlCommand(CreateInvoiceRequest request) {
        StringBuilder curl = new StringBuilder();
        curl.append("curl -X POST 'http://localhost:8080/api/payment/create-invoice' \\\n");
        curl.append("  -H 'Content-Type: application/json' \\\n");
        curl.append("  -H 'Authorization: Bearer YOUR_TOKEN' \\\n");
        curl.append("  -d '{\n");
        curl.append("    \"deliveryIds\": [").append(String.join(", ", request.getDeliveryIds().stream().map(String::valueOf).toArray(String[]::new))).append("],\n");
        curl.append("    \"clientEmail\": \"").append(request.getClientEmail()).append("\",\n");
        curl.append("    \"expirationHours\": ").append(request.getExpirationHours() != null ? request.getExpirationHours() : 24).append("\n");
        curl.append("  }'");
        return curl.toString();
    }
}
