package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.common.JwtUtil;
import com.mvt.mvt_events.dto.PaymentRequest;
import com.mvt.mvt_events.dto.PaymentResponse;
import com.mvt.mvt_events.payment.dto.PaymentReportResponse;
import com.mvt.mvt_events.payment.dto.RecipientBalanceResponse;
import com.mvt.mvt_events.payment.service.PagarMeService;
import com.mvt.mvt_events.jpa.Payment;
import com.mvt.mvt_events.jpa.PaymentStatus;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.repository.PaymentRepository;
import com.mvt.mvt_events.repository.UserRepository;
import com.mvt.mvt_events.service.PaymentService;
import com.mvt.mvt_events.service.SiteConfigurationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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
 *   "providerPaymentId": "or_abc123xyz",
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
    private final JwtUtil jwtUtil;
    private final SiteConfigurationService siteConfigurationService;
    private final UserRepository userRepository;
    private final PagarMeService pagarMeService;

    /**
     * Listar todos os pagamentos com paginação e filtros
     * CLIENT vê apenas seus próprios pagamentos (filtro automático por payer)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COURIER', 'ORGANIZER', 'CLIENT', 'CUSTOMER')")
    @Transactional(readOnly = true)
    @Operation(
            summary = "Listar pagamentos",
            description = "Lista pagamentos. CLIENT/CUSTOMER vê apenas seus próprios pagamentos (payer). " +
                         "ADMIN/ORGANIZER veem todos. Suporte a paginação e filtros."
    )
    public Page<PaymentResponse> list(
            @RequestParam(required = false) UUID payerId,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) String transactionId,
            @RequestParam(required = false, defaultValue = "false") boolean recent,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            jakarta.servlet.http.HttpServletRequest request) {
        
        // Extrair role e userId do token JWT
        String token = extractTokenFromRequest(request);
        String role = jwtUtil.getRoleFromToken(token);
        UUID userIdFromToken = UUID.fromString(jwtUtil.getUserIdFromToken(token));
        
        Specification<Payment> spec = (root, query, cb) -> cb.conjunction();
        
        // Filtro por data: recent=true usa paymentHistoryDays do site_configurations
        if (recent) {
            int days = siteConfigurationService.getActiveConfiguration().getPaymentHistoryDays();
            LocalDateTime since = LocalDateTime.now().minusDays(days);
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), since));
        }

        // CLIENT e CUSTOMER só podem ver seus próprios pagamentos (onde ele é o payer)
        if ("CLIENT".equals(role) || "CUSTOMER".equals(role)) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("payer").get("id"), userIdFromToken));
        } else if ("COURIER".equals(role)) {
            // COURIER vê apenas pagamentos de entregas onde ele foi o motoboy
            spec = spec.and((root, query, cb) -> {
                var deliveriesJoin = root.join("deliveries");
                return cb.equal(deliveriesJoin.get("courier").get("id"), userIdFromToken);
            });
        } else if ("ORGANIZER".equals(role)) {
            // ORGANIZER vê apenas pagamentos de entregas onde ele é o gerente
            // query.distinct(true) evita duplicatas quando um payment tem múltiplas deliveries do mesmo organizer
            spec = spec.and((root, query, cb) -> {
                query.distinct(true);
                var deliveriesJoin = root.join("deliveries");
                return cb.equal(deliveriesJoin.get("organizer").get("id"), userIdFromToken);
            });
        } else if (payerId != null) {
            // ADMIN pode filtrar por payerId se desejado
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
     * Helper para extrair token do request
     */
    private String extractTokenFromRequest(jakarta.servlet.http.HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new RuntimeException("Token JWT não encontrado no header Authorization");
    }

    /**
     * Listar pagamentos do organizer autenticado
     * Retorna todos os payments que contêm ao menos uma delivery onde o gerente logado é o organizer.
     * Suporte a paginação e filtro por status.
     */
    @GetMapping("/organizer")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    @Transactional(readOnly = true)
    @Operation(
            summary = "Listar pagamentos do gerente autenticado",
            description = "Retorna pagamentos que contêm ao menos uma entrega onde o gerente logado é o organizer. " +
                         "O organizerId é extraído do token — nenhum parâmetro de ID necessário."
    )
    public Page<PaymentResponse> listByOrganizer(
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false, defaultValue = "false") boolean recent,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            jakarta.servlet.http.HttpServletRequest request) {

        String token = extractTokenFromRequest(request);
        UUID organizerId = UUID.fromString(jwtUtil.getUserIdFromToken(token));

        Specification<Payment> spec = (root, query, cb) -> {
            query.distinct(true);
            var deliveriesJoin = root.join("deliveries");
            return cb.equal(deliveriesJoin.get("organizer").get("id"), organizerId);
        };

        // Filtro por data: recent=true usa paymentHistoryDays do site_configurations
        if (recent) {
            int days = siteConfigurationService.getActiveConfiguration().getPaymentHistoryDays();
            LocalDateTime since = LocalDateTime.now().minusDays(days);
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), since));
        }

        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        return paymentRepository.findAll(spec, pageable).map(PaymentResponse::from);
    }

    /**
     * Buscar pagamento por ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COURIER', 'ORGANIZER', 'CLIENT', 'CUSTOMER')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'COURIER', 'ORGANIZER', 'CLIENT', 'CUSTOMER')")
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
     * Gerar relatório detalhado de um pagamento
     */
    @GetMapping("/{id}/report")
    @PreAuthorize("hasAnyRole('ADMIN', 'COURIER', 'ORGANIZER', 'CLIENT', 'CUSTOMER')")
    @Transactional(readOnly = true)
    @Operation(
            summary = "Gerar relatório detalhado do pagamento",
            description = "Retorna composição completa do pagamento: deliveries, splits por delivery e splits consolidados"
    )
    public ResponseEntity<PaymentReportResponse> getPaymentReport(@PathVariable Long id) {
        log.info("📊 Requisição de relatório para Payment ID: {}", id);
        PaymentReportResponse report = paymentService.generatePaymentReport(id);
        return ResponseEntity.ok(report);
    }

    /**
     * Verificar status do pagamento (polling)
     * 
     * Endpoint otimizado para polling do mobile enquanto aguarda pagamento PIX.
     * Retorna status atualizado do pagamento.
     * 
     * Mobile deve chamar este endpoint a cada 10 segundos enquanto status != PAID
     */
    @GetMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'COURIER', 'ORGANIZER', 'CLIENT', 'CUSTOMER')")
    @Transactional(readOnly = true)
    @Operation(
            summary = "Verificar status do pagamento (polling)",
            description = "Endpoint otimizado para polling. Mobile chama a cada 10s até status=PAID"
    )
    public ResponseEntity<PaymentResponse> checkPaymentStatus(@PathVariable Long id) {
        log.debug("🔄 Polling status - Payment ID: {}", id);
        
        return paymentRepository.findById(id)
                .map(payment -> {
                    PaymentResponse response = PaymentResponse.from(payment);
                    
                    // Log apenas quando houver mudança de status (não logar todos os polls)
                    if (payment.getStatus() != PaymentStatus.PENDING) {
                        log.info("✅ Payment ID {} - Status: {} (polling)", id, payment.getStatus());
                    }
                    
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
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
                    if (status == PaymentStatus.PAID) {
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
                    if (payment.getStatus() == PaymentStatus.PAID || payment.getStatus() == PaymentStatus.PROCESSING) {
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
    @PreAuthorize("hasAnyRole('COURIER', 'ORGANIZER', 'CLIENT', 'CUSTOMER')")
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
                log.info("📤 Pedido pendente existente retornado: {}", response.getProviderPaymentId());
                return ResponseEntity.ok(response);
            }

            // Caso contrário, retorna 201 Created
            log.info("📤 Novo pedido criado com sucesso!");
            log.info("   ├─ Payment ID: {}", response.getId());
            log.info("   ├─ Provider Payment ID: {}", response.getProviderPaymentId());
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

    /**
     * Processa pagamento automaticamente baseado na preferência do cliente.
     * 
     * <p><strong>Fluxo automático:</strong></p>
     * <ol>
     *   <li>Busca a preferência de pagamento do cliente (PIX ou Cartão)</li>
     *   <li>Se PIX: Gera QR Code para pagamento</li>
     *   <li>Se Cartão: Processa cobrança imediata com cartão padrão</li>
     * </ol>
     * 
     * <p><strong>Resposta PIX:</strong></p>
     * <pre>
     * {
     *   "paymentMethod": "PIX",
     *   "pixQrCode": "00020126360014BR.GOV.BCB.PIX...",
     *   "pixQrCodeUrl": "https://api.pagar.me/qr/123.png",
     *   "status": "PENDING",
     *   "expiresAt": "2026-02-13T19:50:00"
     * }
     * </pre>
     * 
     * <p><strong>Resposta Cartão:</strong></p>
     * <pre>
     * {
     *   "paymentMethod": "CREDIT_CARD",
     *   "cardLastFour": "1234",
     *   "cardBrand": "VISA",
     *   "status": "PAID",
     *   "paymentDate": "2026-02-12T19:50:00"
     * }
     * </pre>
     * 
     * @param deliveryId ID da entrega a pagar
     * @param authentication Autenticação do cliente
     * @param request Request HTTP para extrair token
     * @return PaymentResponse com QR Code (PIX) ou confirmação (Cartão)
     */
    @PostMapping("/pay-delivery/{deliveryId}")
    @PreAuthorize("hasAnyRole('CLIENT', 'CUSTOMER')")
    @Operation(
            summary = "Pagar delivery automaticamente",
            description = "Processa pagamento baseado na preferência do cliente. " +
                         "PIX: retorna QR Code. Cartão: processa pagamento imediato com cartão padrão."
    )
    public ResponseEntity<?> payDelivery(
            @PathVariable Long deliveryId,
            Authentication authentication,
            jakarta.servlet.http.HttpServletRequest request) {
        
        log.info("📥 Requisição de pagamento automático - Delivery: {}", deliveryId);
        
        try {
            // Extrair cliente do token
            String token = extractTokenFromRequest(request);
            UUID clientId = UUID.fromString(jwtUtil.getUserIdFromToken(token));
            
            // Processar pagamento
            PaymentResponse response = paymentService.processAutoPayment(deliveryId, clientId);
            
            log.info("📤 Pagamento processado - Tipo: {}, Status: {}", 
                    response.getPaymentMethod(), response.getStatus());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Dados inválidos: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "INVALID_DATA",
                    "message", e.getMessage()
            ));
            
        } catch (IllegalStateException e) {
            log.warn("⚠️ Conflito de estado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "CONFLICT",
                    "message", e.getMessage()
            ));
            
        } catch (Exception e) {
            log.error("❌ Erro ao processar pagamento", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "INTERNAL_ERROR",
                    "message", "Erro ao processar pagamento: " + e.getMessage()
            ));
        }
    }

    // ============================================================================
    // SALDO DO RECEBEDOR (COURIER / ORGANIZER)
    // ============================================================================

    /**
     * Retorna o saldo do usuário logado no Pagar.me.
     *
     * <p>Fluxo:</p>
     * <ol>
     *   <li>Extrai userId do token JWT</li>
     *   <li>Busca o usuário e obtém seu pagarmeRecipientId</li>
     *   <li>Chama Pagar.me GET /recipients/{id}/balance</li>
     *   <li>Retorna disponível, a receber e total transferido em centavos e em Reais</li>
     * </ol>
     *
     * <p>Exemplo de resposta:</p>
     * <pre>
     * {
     *   "recipientId": "re_abc123",
     *   "available":      { "amount": 1250 },
     *   "waiting_funds":   { "amount": 500 },
     *   "transferred":    { "amount": 20000 },
     *   "availableBrl":   12.50,
     *   "waitingFundsBrl": 5.00,
     *   "transferredBrl": 200.00
     * }
     * </pre>
     */
    @GetMapping("/my-balance")
    @PreAuthorize("hasAnyRole('ADMIN', 'COURIER', 'ORGANIZER')")
    @Operation(summary = "Saldo do recebedor logado", description = "Retorna o saldo disponível, a receber e transferido do usuário logado no Pagar.me")
    public ResponseEntity<?> getMyBalance(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        UUID userId = UUID.fromString(jwtUtil.getUserIdFromToken(token));

        log.info("💰 Consultando saldo Pagar.me do usuário {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        String recipientId = user.getPagarmeRecipientId();
        if (recipientId == null || recipientId.isBlank()) {
            log.warn("⚠️ Usuário {} não possui pagarmeRecipientId cadastrado", userId);
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of(
                    "error", "NO_RECIPIENT",
                    "message", "Usuário não possui conta de recebedor cadastrada no Pagar.me"
            ));
        }

        try {
            RecipientBalanceResponse balance = pagarMeService.getRecipientBalance(recipientId);
            return ResponseEntity.ok(balance);
        } catch (Exception e) {
            log.error("❌ Erro ao consultar saldo do usuário {} (recipient={})", userId, recipientId, e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                    "error", "PAGARME_ERROR",
                    "message", "Erro ao consultar saldo na Pagar.me: " + e.getMessage()
            ));
        }
    }
}
