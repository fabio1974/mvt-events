package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.dto.PaymentRequest;
import com.mvt.mvt_events.dto.PaymentResponse;
import com.mvt.mvt_events.jpa.Payment;
import com.mvt.mvt_events.jpa.PaymentStatus;
import com.mvt.mvt_events.repository.PaymentRepository;
import com.mvt.mvt_events.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Controller REST para gerenciamento de pagamentos PIX via Pagar.me
 * 
 * <p>Endpoints disponíveis:</p>
 * <ul>
 *   <li>POST /api/payment/create-with-split - Criar pedido PIX com split automático</li>
 * </ul>
 * 
 * <p><strong>Autenticação:</strong> Requer token JWT válido</p>
 * <p><strong>Autorização:</strong> COURIER, ORGANIZER ou CLIENT</p>
 * 
 * <p><strong>Fluxo de pagamento:</strong></p>
 * <ol>
 *   <li>Cliente solicita pagamento de uma entrega</li>
 *   <li>Sistema cria pedido no Pagar.me com split 87/5/8</li>
 *   <li>Retorna QR Code PIX e código copia-e-cola</li>
 *   <li>Cliente paga via PIX</li>
 *   <li>Pagar.me envia webhook confirmando pagamento</li>
 *   <li>Sistema atualiza status do pagamento</li>
 * </ol>
 * 
 * <p><strong>Exemplo de request:</strong></p>
 * <pre>
 * POST /api/payment/create-with-split
 * {
 *   "deliveryId": 123,
 *   "amount": 50.00,
 *   "clientEmail": "cliente@example.com",
 *   "description": "Pagamento de entrega #123",
 *   "expirationHours": 24
 * }
 * </pre>
 * 
 * <p><strong>Exemplo de response:</strong></p>
 * <pre>
 * {
 *   "paymentId": 789,
 *   "pagarmeOrderId": "or_abc123xyz",
 *   "pixQrCode": "00020126360014BR.GOV.BCB.PIX...",
 *   "pixQrCodeUrl": "https://api.pagar.me/qr/123.png",
 *   "amount": 50.00,
 *   "status": "PENDING",
 *   "expiresAt": "2025-12-03T23:59:59",
 *   "expired": false,
 *   "statusMessage": "⏳ Aguardando pagamento. Escaneie o QR Code ou use o código PIX."
 * }
 * </pre>
 * 
 * @see PaymentService
 * @see PaymentRequest
 * @see PaymentResponse
 */
@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Pagamentos", description = "Gestão de pagamentos PIX com split automático")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;

    /**
     * Listar todos os pagamentos com paginação e filtros
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COURIER', 'ORGANIZER', 'CLIENT')")
    @Transactional(readOnly = true)
    @Operation(
            summary = "Listar pagamentos",
            description = "Lista todos os pagamentos com suporte a paginação e filtros por status, payer, etc."
    )
    public Page<PaymentResponse> list(
            @RequestParam(required = false) UUID payerId,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) String transactionId,
            Pageable pageable) {
        
        Specification<Payment> spec = (root, query, cb) -> cb.conjunction();
        
        if (payerId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("payer").get("id"), payerId));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (transactionId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("transactionId"), transactionId));
        }
        
        return paymentRepository.findAll(spec, pageable)
                .map(PaymentResponse::from);
    }

    /**
     * Buscar pagamento por ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COURIER', 'ORGANIZER', 'CLIENT')")
    @Transactional(readOnly = true)
    @Operation(
            summary = "Buscar pagamento por ID",
            description = "Retorna os detalhes de um pagamento específico"
    )
    public ResponseEntity<PaymentResponse> getById(@PathVariable Long id) {
        return paymentRepository.findById(id)
                .map(PaymentResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Buscar pagamentos por delivery ID
     */
    @GetMapping("/by-delivery/{deliveryId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COURIER', 'ORGANIZER', 'CLIENT')")
    @Transactional(readOnly = true)
    @Operation(
            summary = "Buscar pagamentos por delivery",
            description = "Lista todos os pagamentos associados a uma entrega específica"
    )
    public ResponseEntity<?> getByDeliveryId(@PathVariable UUID deliveryId) {
        var payments = paymentRepository.findByDeliveryId(deliveryId)
                .stream()
                .map(PaymentResponse::from)
                .toList();
        return ResponseEntity.ok(payments);
    }

    /**
     * Atualizar status de um pagamento (somente ADMIN)
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Operation(
            summary = "Atualizar status do pagamento",
            description = "Permite que administradores atualizem manualmente o status de um pagamento"
    )
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestParam PaymentStatus status) {
        
        return paymentRepository.findById(id)
                .map(payment -> {
                    payment.setStatus(status);
                    if (status == PaymentStatus.COMPLETED) {
                        payment.setPaymentDate(java.time.LocalDateTime.now());
                    }
                    Payment updated = paymentRepository.save(payment);
                    return ResponseEntity.ok(PaymentResponse.from(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Deletar pagamento (somente ADMIN, e apenas se PENDING ou CANCELLED)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Operation(
            summary = "Deletar pagamento",
            description = "Remove um pagamento (apenas PENDING ou CANCELLED)"
    )
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return paymentRepository.findById(id)
                .map(payment -> {
                    if (payment.getStatus() == PaymentStatus.COMPLETED || payment.getStatus() == PaymentStatus.PROCESSING) {
                        return ResponseEntity.badRequest().body(Map.of(
                                "error", "INVALID_STATUS",
                                "message", "Não é possível deletar pagamento com status " + payment.getStatus()
                        ));
                    }
                    paymentRepository.delete(payment);
                    return ResponseEntity.noContent().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Cria um pedido PIX com split automático entre motoboy, gestor e plataforma
     * 
     * <p><strong>Split de valores:</strong></p>
     * <ul>
     *   <li>87% para o motoboy (courier)</li>
     *   <li>5% para o gestor da organização</li>
     *   <li>8% para a plataforma (Zapi10 assume risco e paga taxas)</li>
     * </ul>
     * 
     * <p><strong>Validações:</strong></p>
     * <ul>
     *   <li>Entrega deve existir</li>
     *   <li>Valor mínimo: R$ 1,00</li>
     *   <li>Motoboy deve ter recipient Pagar.me criado</li>
     *   <li>Email do cliente deve ser válido</li>
     * </ul>
     * 
     * <p><strong>Status HTTP:</strong></p>
     * <ul>
     *   <li>201 Created - Pedido criado com sucesso</li>
     *   <li>200 OK - Já existe pedido pendente (retorna o existente)</li>
     *   <li>400 Bad Request - Dados inválidos</li>
     *   <li>404 Not Found - Entrega não encontrada</li>
     *   <li>409 Conflict - Entrega já foi paga</li>
     *   <li>500 Internal Server Error - Erro ao comunicar com Pagar.me</li>
     * </ul>
     * 
     * @param request Dados do pagamento (deliveryId, amount, etc)
     * @return PaymentResponse com QR Code PIX e dados do pedido
     */
    @PostMapping("/create-with-split")
    @PreAuthorize("hasAnyRole('COURIER', 'ORGANIZER', 'CLIENT')")
    @Operation(
            summary = "Criar pedido PIX com split para múltiplas deliveries",
            description = "Cria um pedido PIX no Pagar.me com divisão automática de valores entre motoboy (87%), gestor (5%) e plataforma (8% - Zapi10 assume risco e paga taxas). Suporta 1-10 deliveries em um único pagamento. Retorna QR Code PIX para pagamento."
    )
    public ResponseEntity<?> createPaymentWithSplit(@Valid @RequestBody PaymentRequest request) {
        log.info("📥 Recebida requisição de pagamento - {} deliveries, Amount: R$ {}", 
                request.getDeliveryIds().size(), request.getAmount());

        try {
            // Criar pedido no Pagar.me
            PaymentResponse response = paymentService.createPaymentWithSplit(request);

            // Se retornou um pedido existente pendente, retorna 200 OK
            if (response.getId() != null && response.getStatus().name().equals("PENDING")) {
                log.info("📤 Pedido pendente existente retornado: {}", response.getPagarmeOrderId());
                return ResponseEntity.ok(response);
            }

            // Caso contrário, retorna 201 Created
            log.info("📤 Novo pedido criado com sucesso!");
            log.info("   ├─ Payment ID: {}", response.getId());
            log.info("   ├─ Pagar.me Order ID: {}", response.getPagarmeOrderId());
            log.info("   ├─ Amount: R$ {}", response.getAmount());
            log.info("   ├─ Expires: {}", response.getExpiresAt());
            log.info("   └─ PIX QR Code: {}", response.getPixQrCode() != null ? "✅ Disponível" : "❌ Indisponível");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            // Dados inválidos (400)
            log.warn("⚠️ Dados inválidos: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "INVALID_DATA",
                    "message", e.getMessage()
            ));

        } catch (IllegalStateException e) {
            // Conflito de estado (409) - ex: entrega já paga
            log.warn("⚠️ Conflito de estado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "CONFLICT",
                    "message", e.getMessage()
            ));

        } catch (Exception e) {
            // Erro interno (500)
            log.error("❌ Erro ao criar fatura PIX", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "INTERNAL_ERROR",
                    "message", "Erro ao criar fatura PIX: " + e.getMessage()
            ));
        }
    }

    /**
     * Health check do controller
     */
    @GetMapping("/health")
    @Operation(
            summary = "Health check",
            description = "Verifica se o controller de pagamentos está funcionando"
    )
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "PaymentController",
                "message", "✅ Controller de pagamentos operacional"
        ));
    }
}
