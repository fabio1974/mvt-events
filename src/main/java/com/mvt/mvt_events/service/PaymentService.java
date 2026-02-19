package com.mvt.mvt_events.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mvt.mvt_events.dto.PaymentRequest;
import com.mvt.mvt_events.dto.PaymentResponse;
import com.mvt.mvt_events.payment.dto.PaymentReportResponse;
import com.mvt.mvt_events.payment.dto.OrderRequest;
import com.mvt.mvt_events.payment.dto.OrderResponse;
import com.mvt.mvt_events.payment.service.PagarMeService;
import com.mvt.mvt_events.jpa.*;
import com.mvt.mvt_events.repository.DeliveryRepository;
import com.mvt.mvt_events.repository.PaymentRepository;
import com.mvt.mvt_events.repository.UserRepository;
import com.mvt.mvt_events.repository.SiteConfigurationRepository;
import com.mvt.mvt_events.repository.CustomerCardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Serviço responsável pela criação e gestão de pagamentos PIX via Pagar.me
 * 
 * Funcionalidades principais:
 * 1. Criar order PIX com split automático (87% courier, 5% manager, 8% plataforma - Zapi10 assume risco e paga taxas)
 * 2. Validar dados antes de enviar ao Pagar.me
 * 3. Salvar informações localmente
 * 4. Processar webhooks de confirmação de pagamento
 * 
 * IMPORTANTE: Suporta MÚLTIPLAS DELIVERIES em um único pagamento!
 * Isso permite que o cliente pague várias entregas com um único QR Code PIX,
 * economizando taxas e melhorando a UX.
 * 
 * @see PagarMeService
 * @see PaymentRequest
 * @see PaymentResponse
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PagarMeService pagarMeService;
    private final PaymentRepository paymentRepository;
    private final DeliveryRepository deliveryRepository;
    private final UserRepository userRepository;
    private final SiteConfigurationRepository siteConfigurationRepository;
    private final ObjectMapper objectMapper;
    private final PaymentSplitCalculator splitCalculator;
    private final CustomerPaymentPreferenceService preferenceService;
    private final CustomerCardRepository cardRepository;
    private final PushNotificationService pushNotificationService;
    private final DeliveryNotificationService deliveryNotificationService;

    /**
     * Cria um pedido PIX com split automático para MÚLTIPLAS DELIVERIES.
     * 
     * Fluxo:
     * 1. Valida request (deliveryIds, amount, etc)
     * 2. Busca todas as deliveries por IDs
     * 3. Valida TODAS as deliveries:
     *    - Status COMPLETED
     *    - Não têm payment PAID
     *    - Pertencem ao mesmo cliente (payer)
     * 4. Calcula split 87/5/8
     * 5. Cria pedido no Pagar.me com PIX
     * 6. Salva Payment local associado a TODAS as deliveries (N:M)
     * 
     * Split de valores:
     * - 87% para o motoboy (courier)
     * - 5% para o gestor da organização
     * - 8% para a plataforma (Zapi10 assume risco e paga taxas)
     * 
     * @param request Dados do pagamento (deliveryIds, amount, email)
     * @return PaymentResponse com QR Code PIX e dados do pedido
     * @throws IllegalArgumentException Se dados inválidos
     * @throws IllegalStateException Se entregas não encontradas ou inválidas
     */
    @Transactional
    public PaymentResponse createPaymentWithSplit(PaymentRequest request) {
        log.info("💰 Criando pagamento PIX com split - Deliveries: {}", request.getDeliveryIds());
        
        // 1. Buscar deliveries
        List<Delivery> deliveries = deliveryRepository.findAllById(request.getDeliveryIds());
        
        if (deliveries.isEmpty()) {
            throw new IllegalArgumentException("Nenhuma delivery encontrada com os IDs fornecidos");
        }
        
        if (deliveries.size() != request.getDeliveryIds().size()) {
            throw new IllegalArgumentException("Algumas deliveries não foram encontradas");
        }
        
        // 2. Validar deliveries
        validateDeliveriesForPayment(deliveries);
        
        // 3. Pegar primeira delivery como referência (todas pertencem ao mesmo cliente)
        Delivery firstDelivery = deliveries.get(0);
        User client = firstDelivery.getClient();
        
        // 4. Construir OrderRequest PIX (2 horas de expiração - padrão)
        OrderRequest orderRequest = buildPixOrderRequest(deliveries, client, 7200);
        log.info("🔍 OrderRequest PIX construído - {} deliveries, valor total: R$ {}", 
                deliveries.size(), request.getAmount());
        
        try {
            // 5. Criar order no Pagar.me
            OrderResponse orderResponse = pagarMeService.createOrderWithFullResponse(orderRequest);
            log.info("✅ OrderResponse recebido - Order ID: {}, Status: {}", 
                    orderResponse.getId(), orderResponse.getStatus());
            
            // 6. Salvar Payment local
            Payment payment = new Payment();
            payment.setProviderPaymentId(orderResponse.getId());
            payment.setAmount(request.getAmount());
            payment.setCurrency(com.mvt.mvt_events.jpa.Currency.BRL);
            payment.setPaymentMethod(PaymentMethod.PIX);
            payment.setProvider(PaymentProvider.PAGARME);
            payment.setPayer(client);
            payment.setStatus(PaymentStatus.PENDING); // PIX sempre começa PENDING
            
            // Associar todas as deliveries
            for (Delivery delivery : deliveries) {
                payment.addDelivery(delivery);
            }
            
            // 7. Extrair QR Code e URL
            String qrCodeExtracted = null;
            String qrCodeUrlExtracted = null;
            String expiresAtStr = null;
            
            if (orderResponse.getCharges() != null && !orderResponse.getCharges().isEmpty()) {
                OrderResponse.Charge charge = orderResponse.getCharges().get(0);
                if (charge.getLastTransaction() != null) {
                    OrderResponse.LastTransaction transaction = charge.getLastTransaction();
                    qrCodeExtracted = transaction.getQrCode();
                    qrCodeUrlExtracted = transaction.getQrCodeUrl();
                    expiresAtStr = transaction.getExpiresAt();
                    
                    payment.setPixQrCode(qrCodeExtracted);
                    payment.setPixQrCodeUrl(qrCodeUrlExtracted);
                    
                    log.info("📱 QR Code extraído - Length: {}, URL presente: {}", 
                            qrCodeExtracted != null ? qrCodeExtracted.length() : 0,
                            qrCodeUrlExtracted != null ? "SIM" : "NÃO");
                    
                    // Expiration - Pagar.me retorna no formato ISO 8601 com timezone UTC (ex: 2026-02-18T23:41:17Z)
                    // Precisamos converter para o timezone local do servidor (BRT/UTC-3)
                    if (expiresAtStr != null) {
                        try {
                            // Parse usando OffsetDateTime (suporta timezone Z)
                            OffsetDateTime offsetDateTime = OffsetDateTime.parse(expiresAtStr);
                            // Converter para o timezone local do servidor mantendo o mesmo instante
                            LocalDateTime expiresAt = offsetDateTime.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
                            payment.setExpiresAt(expiresAt);
                            log.info("⏰ Expiração configurada: {} (UTC: {}, Timezone: {})", 
                                    expiresAt, expiresAtStr, ZoneId.systemDefault());
                        } catch (Exception e) {
                            log.warn("⚠️ Erro ao parsear expiresAt '{}': {}", expiresAtStr, e.getMessage());
                            // Fallback: calcular manualmente (2 horas)
                            LocalDateTime calculatedExpiration = LocalDateTime.now().plusHours(2);
                            payment.setExpiresAt(calculatedExpiration);
                            log.info("⏰ Expiração calculada manualmente após erro de parse: {}", calculatedExpiration);
                        }
                    } else {
                        // Se Pagar.me não retornou expiresAt, calcular manualmente (2 horas)
                        LocalDateTime calculatedExpiration = LocalDateTime.now().plusHours(2);
                        payment.setExpiresAt(calculatedExpiration);
                        log.info("⏰ Expiração calculada manualmente (Pagar.me não retornou): {}", calculatedExpiration);
                    }
                }
            }
            
            // 8. Salvar request/response completos
            try {
                String requestJson = objectMapper.writeValueAsString(orderRequest);
                String responseJson = objectMapper.writeValueAsString(orderResponse);
                
                log.info("🔍 Request JSON: {} caracteres", requestJson != null ? requestJson.length() : "NULL");
                log.info("🔍 Response JSON: {} caracteres", responseJson != null ? responseJson.length() : "NULL");
                
                payment.setRequest(requestJson);
                payment.setResponse(responseJson);
                
                log.info("✅ Request/Response setados no Payment");
                
            } catch (Exception e) {
                log.error("❌ Erro ao serializar request/response: {}", e.getMessage(), e);
                // Continua sem salvar request/response para não bloquear o pagamento
            }
            
            // 9. Adicionar notes com informações do pagamento
            String orderStatus = orderResponse.getStatus();
            String deliveryIdsStr = deliveries.stream()
                    .map(d -> "#" + d.getId())
                    .collect(Collectors.joining(", "));
            String notes = String.format("Pagamento PIX %s - Order ID: %s - Entregas: %s - Valor: R$ %.2f",
                orderStatus != null ? orderStatus.toUpperCase() : "UNKNOWN",
                orderResponse.getId(),
                deliveryIdsStr,
                request.getAmount());
            payment.setNotes(notes);
            log.info("📝 Notes preenchido: {}", notes);
            
            // 10. Salvar no banco
            payment = paymentRepository.save(payment);
            log.info("💾 Payment PIX salvo - ID: {}, Provider ID: {}, Status: {}, Request length: {}, Response length: {}, QR Code presente: {}", 
                    payment.getId(), 
                    payment.getProviderPaymentId(),
                    payment.getStatus(),
                    payment.getRequest() != null ? payment.getRequest().length() : 0,
                    payment.getResponse() != null ? payment.getResponse().length() : 0,
                    qrCodeExtracted != null ? "SIM" : "NÃO");
            
            log.info("✅ Pagamento PIX criado com sucesso - Cliente deve escanear QR Code ou usar copia-e-cola");
            
            return PaymentResponse.from(payment);
            
        } catch (Exception e) {
            log.error("❌ Erro ao criar pagamento PIX - Deliveries: {}, Valor: R$ {}, Cliente: {}", 
                    request.getDeliveryIds(), request.getAmount(), client.getId(), e);
            
            // Salvar payment FAILED em transação separada
            saveFailedPayment(request.getAmount(), PaymentMethod.PIX, client, firstDelivery, e.getMessage());
            
            throw new RuntimeException("Erro ao processar pagamento PIX: " + e.getMessage(), e);
        }
    }

    /**
     * Valida todas as deliveries para pagamento.
     * 
     * Validações:
     * 1. Todas devem estar COMPLETED
     * 2. Nenhuma deve ter payment PAID ou PENDING
     * 3. Todas devem pertencer ao mesmo cliente (payer)
     * 4. Não deve existir payment PENDING com o mesmo conjunto de deliveries
     * 
     * @param deliveries Lista de deliveries a validar
     * @throws IllegalStateException Se alguma validação falhar
     */
    private void validateDeliveriesForPayment(List<Delivery> deliveries) {
        if (deliveries == null || deliveries.isEmpty()) {
            throw new IllegalArgumentException("Lista de deliveries vazia");
        }

        User firstPayer = deliveries.get(0).getClient();
        UUID firstPayerId = firstPayer.getId();
        List<Long> deliveryIds = deliveries.stream().map(Delivery::getId).collect(Collectors.toList());

        // 4. PRIMEIRO: Verificar se já existe um payment PENDING/COMPLETED com essas deliveries
        List<Payment> existingPayments = paymentRepository.findPendingOrCompletedPaymentsForDeliveries(deliveryIds);
        
        if (!existingPayments.isEmpty()) {
            Payment existingPayment = existingPayments.get(0);
            List<Long> existingDeliveryIds = existingPayment.getDeliveries().stream()
                    .map(Delivery::getId)
                    .sorted()
                    .collect(Collectors.toList());
            
            List<Long> requestedDeliveryIds = deliveryIds.stream()
                    .sorted()
                    .collect(Collectors.toList());
            
            // Verificar se é exatamente o mesmo conjunto ou subconjunto
            boolean hasOverlap = existingDeliveryIds.stream().anyMatch(requestedDeliveryIds::contains);
            
            if (hasOverlap) {
                String status = existingPayment.getStatus() == PaymentStatus.PAID ? "PAGO" : "PENDENTE";
                String deliveriesStr = existingDeliveryIds.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(", "));
                
                throw new IllegalStateException(
                        String.format("❌ Já existe um pagamento %s (ID: %s) para as entregas: %s. " +
                                "Não é possível criar um novo pagamento para entregas que já possuem pagamento ativo.",
                                status, existingPayment.getId(), deliveriesStr));
            }
        }

        // Validações individuais de cada delivery
        for (int i = 0; i < deliveries.size(); i++) {
            Delivery delivery = deliveries.get(i);
            
            // 1. Validar status COMPLETED
            if (delivery.getStatus() != Delivery.DeliveryStatus.COMPLETED) {
                throw new IllegalStateException(
                        String.format("❌ A entrega %s não está COMPLETED (status atual: %s). " +
                                "Apenas entregas completadas podem ser pagas.", 
                                delivery.getId(), delivery.getStatus()));
            }

            // 3. Validar mesmo cliente (payer)
            if (!delivery.getClient().getId().equals(firstPayerId)) {
                throw new IllegalStateException(
                        String.format("❌ A entrega %s pertence a outro cliente. " +
                                "Todas as entregas devem pertencer ao mesmo cliente. " +
                                "Esperado: %s, Encontrado: %s", 
                                delivery.getId(), firstPayerId, delivery.getClient().getId()));
            }

            log.info("✅ Delivery {} validada (status: {}, payer: {})", 
                    delivery.getId(), delivery.getStatus(), delivery.getClient().getUsername());
        }

        log.info("✅ Todas as {} deliveries validadas com sucesso!", deliveries.size());
    }

    /**
     * Processa confirmação de pagamento via webhook Pagar.me.
     * 
     * Este método é chamado quando o Pagar.me envia um webhook confirmando
     * que o pagamento foi realizado. Atualiza o status do Payment para COMPLETED.
     * 
     * @param orderId ID da order Pagar.me que foi paga
     */
    @Transactional
    public void processPaymentConfirmation(String orderId) {
        log.info("🔔 Processando confirmação de pagamento - Order: {}", orderId);

        Payment payment = paymentRepository.findByProviderPaymentId(orderId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Payment não encontrado para order: " + orderId));

        if (payment.getStatus() == PaymentStatus.PAID) {
            log.warn("⚠️ Payment já estava COMPLETED: {}", payment.getId());
            return;
        }

        payment.markAsCompleted();
        paymentRepository.save(payment);

        log.info("✅ Payment {} marcado como COMPLETED ({} deliveries pagas)", 
                payment.getId(), payment.getDeliveriesCount());

        // Se alguma delivery está em WAITING_PAYMENT → transicionar para ACCEPTED
        if (payment.getDeliveries() != null) {
            for (Delivery delivery : payment.getDeliveries()) {
                if (delivery.getStatus() == Delivery.DeliveryStatus.WAITING_PAYMENT) {
                    delivery.setStatus(Delivery.DeliveryStatus.ACCEPTED);
                    delivery.setPaymentCompleted(true);
                    delivery.setPaymentCaptured(true);
                    deliveryRepository.save(delivery);
                    log.info("✅ Delivery #{} atualizada: WAITING_PAYMENT → ACCEPTED (pagamento PIX confirmado)", 
                            delivery.getId());
                }
            }
        }
    }

    /**
     * Processa expiração de pagamento PIX via webhook (charge.expired, order.canceled).
     * 
     * Somente pagamentos de pagadores CUSTOMER são processados com Opção B:
     * marca pagamento como EXPIRED, desassocia courier da delivery,
     * retorna para PENDING e notifica motoboys disponíveis.
     * 
     * @param orderId ID da order no Pagar.me
     */
    @Transactional
    public void processPaymentExpiration(String orderId) {
        log.info("⏰ Processando expiração de pagamento - Order: {}", orderId);

        Payment payment = paymentRepository.findByProviderPaymentId(orderId)
                .orElse(null);

        if (payment == null) {
            log.warn("⚠️ Payment não encontrado para order expirada: {} — ignorando", orderId);
            return;
        }

        if (payment.getStatus() == PaymentStatus.EXPIRED) {
            log.warn("⚠️ Payment #{} já estava EXPIRED — ignorando webhook duplicado", payment.getId());
            return;
        }

        if (payment.getStatus() == PaymentStatus.PAID) {
            log.warn("⚠️ Payment #{} já está PAID — ignorando expiração tardia", payment.getId());
            return;
        }

        // Somente processar Opção B para CUSTOMER
        if (payment.getPayer() == null || payment.getPayer().getRole() != User.Role.CUSTOMER) {
            log.info("⏭️ Payment #{} não é de CUSTOMER — apenas marcando como EXPIRED", payment.getId());
            payment.setStatus(PaymentStatus.EXPIRED);
            paymentRepository.save(payment);
            return;
        }

        // Marcar como EXPIRED
        payment.setStatus(PaymentStatus.EXPIRED);
        paymentRepository.save(payment);
        log.info("⏰ Payment #{} CUSTOMER PIX marcado como EXPIRED via webhook", payment.getId());

        // Reverter deliveries em WAITING_PAYMENT → PENDING (Opção B) e notificar motoboys
        if (payment.getDeliveries() != null) {
            for (Delivery delivery : payment.getDeliveries()) {
                if (delivery.getStatus() == Delivery.DeliveryStatus.WAITING_PAYMENT) {
                    log.info("   ├─ Delivery #{}: WAITING_PAYMENT → PENDING (webhook expiração)", delivery.getId());
                    delivery.setStatus(Delivery.DeliveryStatus.PENDING);
                    delivery.setCourier(null);
                    delivery.setAcceptedAt(null);
                    delivery.setVehicle(null);
                    delivery.setPaymentCompleted(false);
                    delivery.setPaymentCaptured(false);
                    deliveryRepository.save(delivery);

                    // Notificar motoboys disponíveis (mesmo fluxo de delivery nova)
                    try {
                        deliveryNotificationService.notifyAvailableDrivers(delivery);
                        log.info("   ├─ 📢 Push enviado para motoboys disponíveis (delivery #{})", delivery.getId());
                    } catch (Exception e) {
                        log.error("   ├─ ❌ Falha ao enviar push para motoboys (delivery #{}): {}", 
                                delivery.getId(), e.getMessage());
                    }

                    log.info("   └─ ✅ Delivery #{} revertida para PENDING — motoboys notificados", delivery.getId());
                }
            }
        }
    }

    /**
     * Gera relatório detalhado de um pagamento consolidado.
     * Mostra a composição completa: deliveries, splits por delivery, e splits consolidados.
     * 
     * @param paymentId ID do pagamento
     * @return Relatório detalhado
     */
    @Transactional(readOnly = true)
    public PaymentReportResponse generatePaymentReport(Long paymentId) {
        log.info("📊 Gerando relatório para Payment ID: {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment não encontrado: " + paymentId));

        // Buscar configuração ativa para obter percentuais
        SiteConfiguration config = siteConfigurationRepository.findActiveConfiguration()
                .orElseThrow(() -> new IllegalStateException("Nenhuma configuração ativa encontrada"));

        // Lista de deliveries do pagamento
        List<Delivery> deliveries = payment.getDeliveries();
        
        // Mapa para acumular splits consolidados por recipient
        Map<String, PaymentReportResponse.SplitItem> consolidatedSplitsMap = new HashMap<>();

        // Processar cada delivery
        List<PaymentReportResponse.DeliveryItem> deliveryItems = new ArrayList<>();
        
        for (Delivery delivery : deliveries) {
            BigDecimal shippingFee = delivery.getShippingFee();
            BigDecimal shippingFeeCents = splitCalculator.toCents(shippingFee);
            
            List<PaymentReportResponse.SplitItem> deliverySplits = new ArrayList<>();
            
            // Verificar se há organizer válido
            boolean hasOrganizer = splitCalculator.hasValidOrganizer(delivery);
            
            // Calcular valores em centavos (necessário para cálculo da plataforma)
            BigDecimal courierAmountCents = delivery.getCourier() != null ? 
                splitCalculator.calculateCourierAmount(shippingFeeCents, config) : BigDecimal.ZERO;
            BigDecimal organizerAmountCents = hasOrganizer ? 
                splitCalculator.calculateOrganizerAmount(shippingFeeCents, config) : BigDecimal.ZERO;
            
            // Split do COURIER (87% padrão)
            if (delivery.getCourier() != null) {
                BigDecimal courierPercentage = splitCalculator.calculateCourierPercentage(config);
                
                PaymentReportResponse.SplitItem courierSplit = PaymentReportResponse.SplitItem.builder()
                        .recipientId(delivery.getCourier().getPagarmeRecipientId())
                        .recipientName(delivery.getCourier().getName())
                        .recipientRole("COURIER")
                        .amount(splitCalculator.toReais(courierAmountCents, 2))
                        .percentage(courierPercentage)
                        .liable(false)
                        .build();
                
                deliverySplits.add(courierSplit);
                
                // Acumular no consolidado
                String key = delivery.getCourier().getId() + "_COURIER";
                consolidatedSplitsMap.merge(key, courierSplit, (existing, newSplit) -> 
                    PaymentReportResponse.SplitItem.builder()
                            .recipientId(existing.getRecipientId())
                            .recipientName(existing.getRecipientName())
                            .recipientRole(existing.getRecipientRole())
                            .amount(existing.getAmount().add(newSplit.getAmount()))
                            .percentage(existing.getPercentage()) // Mantém percentual
                            .liable(existing.getLiable())
                            .build()
                );
            }
            
            // Split do ORGANIZER (5% padrão) - apenas se existir
            if (hasOrganizer) {
                User organizer = delivery.getOrganizer();
                BigDecimal organizerPercentage = config.getOrganizerPercentage();
                
                PaymentReportResponse.SplitItem organizerSplit = PaymentReportResponse.SplitItem.builder()
                        .recipientId(organizer.getPagarmeRecipientId())
                        .recipientName(organizer.getName())
                        .recipientRole("ORGANIZER")
                        .amount(splitCalculator.toReais(organizerAmountCents, 2))
                        .percentage(organizerPercentage)
                        .liable(false)
                        .build();
                
                deliverySplits.add(organizerSplit);
                
                // Acumular no consolidado
                String key = organizer.getId() + "_ORGANIZER";
                consolidatedSplitsMap.merge(key, organizerSplit, (existing, newSplit) -> 
                    PaymentReportResponse.SplitItem.builder()
                            .recipientId(existing.getRecipientId())
                            .recipientName(existing.getRecipientName())
                            .recipientRole(existing.getRecipientRole())
                            .amount(existing.getAmount().add(newSplit.getAmount()))
                            .percentage(existing.getPercentage())
                            .liable(existing.getLiable())
                            .build()
                );
            }
            
            // Split da PLATAFORMA
            // ATENÇÃO: Calculado por DIFERENÇA para evitar erros de arredondamento
            BigDecimal platformPercentage = splitCalculator.calculatePlatformPercentage(config, hasOrganizer);
            BigDecimal platformAmountCents = splitCalculator.calculatePlatformAmount(shippingFeeCents, courierAmountCents, organizerAmountCents);
            
            PaymentReportResponse.SplitItem platformSplit = PaymentReportResponse.SplitItem.builder()
                    .recipientId(config.getPagarmeRecipientId())
                    .recipientName("Plataforma Zapi10")
                    .recipientRole("PLATFORM")
                    .amount(splitCalculator.toReais(platformAmountCents, 2))
                    .percentage(platformPercentage)
                    .liable(true)
                    .build();
            
            deliverySplits.add(platformSplit);
            
            // Acumular no consolidado
            String platformKey = "PLATFORM";
            consolidatedSplitsMap.merge(platformKey, platformSplit, (existing, newSplit) -> 
                PaymentReportResponse.SplitItem.builder()
                        .recipientId(existing.getRecipientId())
                        .recipientName(existing.getRecipientName())
                        .recipientRole(existing.getRecipientRole())
                        .amount(existing.getAmount().add(newSplit.getAmount()))
                        .percentage(existing.getPercentage())
                        .liable(existing.getLiable())
                        .build()
            );
            
            // Criar item de delivery
            PaymentReportResponse.DeliveryItem deliveryItem = PaymentReportResponse.DeliveryItem.builder()
                    .deliveryId(delivery.getId())
                    .shippingFee(shippingFee)
                    .clientName(delivery.getClient() != null ? delivery.getClient().getName() : "N/A")
                    .pickupAddress(delivery.getFromAddress())
                    .deliveryAddress(delivery.getToAddress())
                    .splits(deliverySplits)
                    .build();
            
            deliveryItems.add(deliveryItem);
        }
        
        // Montar relatório
        PaymentReportResponse report = PaymentReportResponse.builder()
                .paymentId(payment.getId())
                .providerPaymentId(payment.getProviderPaymentId())
                .status(payment.getStatus() != null ? payment.getStatus().name() : "UNKNOWN")
                .totalAmount(payment.getAmount())
                .currency(payment.getCurrency() != null ? payment.getCurrency().name() : "BRL")
                .createdAt(payment.getCreatedAt())
                .pixQrCode(payment.getPixQrCode())
                .pixQrCodeUrl(payment.getPixQrCodeUrl())
                .expiresAt(payment.getExpiresAt())
                .deliveries(deliveryItems)
                .consolidatedSplits(new ArrayList<>(consolidatedSplitsMap.values()))
                .build();
        
        log.info("✅ Relatório gerado: {} deliveries, {} recipients", 
                deliveryItems.size(), consolidatedSplitsMap.size());
        
        return report;
    }

    /**
     * Salva um Payment com status FAILED em transação independente (REQUIRES_NEW).
     * Garante que o registro persiste mesmo que a transação principal faça rollback.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment saveFailedPayment(BigDecimal amount, PaymentMethod paymentMethod, User payer, Delivery delivery, String errorMessage) {
        Payment failedPayment = new Payment();
        failedPayment.setAmount(amount);
        failedPayment.setCurrency(com.mvt.mvt_events.jpa.Currency.BRL);
        failedPayment.setPaymentMethod(paymentMethod);
        failedPayment.setProvider(PaymentProvider.PAGARME);
        failedPayment.setPayer(payer);
        failedPayment.setStatus(PaymentStatus.FAILED);
        failedPayment.setNotes(errorMessage);
        failedPayment.addDelivery(delivery);
        Payment saved = paymentRepository.save(failedPayment);
        log.info("💾 Payment FAILED #{} salvo (transação independente) para delivery #{}", saved.getId(), delivery.getId());
        return saved;
    }

    /**
     * Processa pagamento automaticamente baseado na preferência do cliente.
     * 
     * Fluxo:
     * 1. Busca delivery e valida status
     * 2. Busca preferência de pagamento do cliente
     * 3. Se PIX → Gera QR Code
     * 4. Se CREDIT_CARD → Processa cobrança imediata
     * 
     * @param deliveryId ID da delivery
     * @param clientId ID do cliente autenticado
     * @return PaymentResponse com dados do pagamento (QR Code ou confirmação)
     */
    @Transactional
    public PaymentResponse processAutoPayment(Long deliveryId, java.util.UUID clientId) {
        log.info("🤖 Processando pagamento automático - Delivery: {}, Client: {}", deliveryId, clientId);
        
        // 1. Buscar delivery
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException("Delivery não encontrada: " + deliveryId));
        
        // 2. Validar que pertence ao cliente
        if (!delivery.getClient().getId().equals(clientId)) {
            throw new IllegalArgumentException("Delivery não pertence ao cliente autenticado");
        }
        
        // 3. Validar status - cliente só pode pagar após motoboy aceitar
        List<Delivery.DeliveryStatus> allowedStatuses = List.of(
            Delivery.DeliveryStatus.ACCEPTED,
            Delivery.DeliveryStatus.IN_TRANSIT,
            Delivery.DeliveryStatus.COMPLETED
        );
        
        if (!allowedStatuses.contains(delivery.getStatus())) {
            throw new IllegalStateException(
                "Delivery não pode ser paga no status atual: " + delivery.getStatus() + 
                ". Status permitidos: ACCEPTED (após motoboy aceitar), IN_TRANSIT, COMPLETED"
            );
        }
        
        // 4. Verificar se já tem pagamento PAID
        if (delivery.getPaymentCompleted() && delivery.getPaymentCaptured()) {
            throw new IllegalStateException("Delivery já foi paga");
        }
        
        // 5. Buscar preferência do cliente
        com.mvt.mvt_events.jpa.CustomerPaymentPreference preference = preferenceService.getPreference(clientId);
        
        log.info("📋 Preferência detectada: {}", preference.getPreferredPaymentType());
        
        // 6. Processar baseado na preferência
        if (preference.prefersPix()) {
            return processPixPayment(delivery, preference);
        } else if (preference.prefersCreditCard()) {
            return processCreditCardPayment(delivery, preference);
        } else {
            throw new IllegalStateException("Preferência de pagamento inválida");
        }
    }

    /**
     * Processa pagamento PIX - retorna QR Code
     */
    private PaymentResponse processPixPayment(Delivery delivery, com.mvt.mvt_events.jpa.CustomerPaymentPreference preference) {
        log.info("💳 Processando pagamento PIX - Delivery: {}, Valor: R$ {}, Cliente: {}", 
                delivery.getId(), delivery.getShippingFee(), delivery.getClient().getUsername());
        
        User client = delivery.getClient();
        
        // Validar que o courier tem recipient
        if (delivery.getCourier() == null || delivery.getCourier().getPagarmeRecipientId() == null) {
            log.error("❌ Motoboy sem conta Pagar.me - Delivery: {}, Courier: {}", 
                    delivery.getId(), delivery.getCourier() != null ? delivery.getCourier().getId() : "NULL");
            throw new IllegalStateException("Motoboy não possui conta Pagar.me configurada");
        }
        
        log.info("✅ Motoboy validado - Recipient ID: {}", delivery.getCourier().getPagarmeRecipientId());
        
        // ⚠️ VALIDAÇÃO CRÍTICA: Verificar se já existe pagamento PENDING para esta delivery
        if (paymentRepository.existsPendingPaymentForDelivery(delivery.getId())) {
            log.warn("❌ Já existe um pagamento PENDING para a entrega #{}. Abortando nova tentativa.", delivery.getId());
            throw new IllegalStateException(
                String.format("Já existe um pagamento pendente para a entrega #%d. " +
                             "Aguarde a conclusão ou cancele o pagamento anterior.", delivery.getId())
            );
        }
        
        log.info("✅ Validação de duplicidade OK - Nenhum pagamento PENDING para delivery #{}", delivery.getId());
        
        PaymentRequest request = new PaymentRequest();
        request.setDeliveryIds(List.of(delivery.getId()));
        request.setAmount(delivery.getShippingFee());
        request.setClientEmail(client.getUsername());
        request.setMotoboyAccountId(delivery.getCourier().getPagarmeRecipientId());
        
        if (delivery.getOrganizer() != null && delivery.getOrganizer().getPagarmeRecipientId() != null) {
            request.setManagerAccountId(delivery.getOrganizer().getPagarmeRecipientId());
            log.info("🏢 Organizer detectado - Recipient ID: {}", delivery.getOrganizer().getPagarmeRecipientId());
        }
        
        request.setDescription("Pagamento entrega #" + delivery.getId());
        
        log.info("📋 PaymentRequest PIX preparado - Chamando createPaymentWithSplit...");
        
        return createPaymentWithSplit(request);
    }

    /**
     * Processa pagamento com cartão de crédito - executa cobrança imediata
     */
    private PaymentResponse processCreditCardPayment(Delivery delivery, com.mvt.mvt_events.jpa.CustomerPaymentPreference preference) {
        log.info("💳 Processando pagamento com Cartão - Delivery: {}", delivery.getId());
        
        // Validar que tem cartão padrão
        if (preference.getDefaultCard() == null) {
            throw new IllegalStateException("Cliente não possui cartão padrão cadastrado. Configure um cartão em suas preferências.");
        }
        
        com.mvt.mvt_events.jpa.CustomerCard card = preference.getDefaultCard();
        
        if (!card.getIsActive()) {
            throw new IllegalStateException("Cartão padrão está inativo. Por favor, ative-o ou selecione outro cartão.");
        }
        
        // Validar que o courier tem recipient
        if (delivery.getCourier() == null || delivery.getCourier().getPagarmeRecipientId() == null) {
            throw new IllegalStateException("Motoboy não possui conta Pagar.me configurada");
        }
        
        // ⚠️ VALIDAÇÃO CRÍTICA: Verificar se já existe pagamento PENDING para esta delivery
        if (paymentRepository.existsPendingPaymentForDelivery(delivery.getId())) {
            log.warn("❌ Já existe um pagamento PENDING para a entrega #{}. Abortando nova tentativa.", delivery.getId());
            throw new IllegalStateException(
                String.format("Já existe um pagamento pendente para a entrega #%d. " +
                             "Aguarde a conclusão ou cancele o pagamento anterior.", delivery.getId())
            );
        }
        
        try {
            // Criar order com cartão no Pagar.me
            OrderRequest orderRequest = buildCardOrderRequest(delivery, card);
            log.info("🔍 OrderRequest construído - Delivery: {}", delivery.getId());
            
            OrderResponse orderResponse = pagarMeService.createOrderWithFullResponse(orderRequest);
            log.info("✅ OrderResponse recebido - Order ID: {}", orderResponse.getId());
            
            // Salvar payment
            Payment payment = new Payment();
            payment.setProviderPaymentId(orderResponse.getId());
            payment.setAmount(delivery.getShippingFee());
            payment.setCurrency(com.mvt.mvt_events.jpa.Currency.BRL);
            payment.setPaymentMethod(PaymentMethod.CREDIT_CARD);
            payment.setProvider(PaymentProvider.PAGARME);
            payment.setPayer(delivery.getClient());
            payment.addDelivery(delivery);
            
            // Armazenar request/response completos
            try {
                String requestJson = objectMapper.writeValueAsString(orderRequest);
                String responseJson = objectMapper.writeValueAsString(orderResponse);
                
                log.info("🔍 Request JSON: {} caracteres", requestJson != null ? requestJson.length() : "NULL");
                log.info("🔍 Response JSON: {} caracteres", responseJson != null ? responseJson.length() : "NULL");
                
                payment.setRequest(requestJson);
                payment.setResponse(responseJson);
                
                log.info("✅ Request/Response setados no Payment");
                
            } catch (Exception e) {
                log.error("❌ Erro ao serializar request/response: {}", e.getMessage(), e);
                // Continua sem salvar request/response para não bloquear o pagamento
            }
            
            // Determinar status real do pagamento (verificando order + charges + transactions + antifraude)
            PaymentStatus finalStatus = determinePaymentStatusFromOrder(orderResponse);
            payment.setStatus(finalStatus);
            
            // Adicionar notes com informações do pagamento
            String orderStatus = orderResponse.getStatus();
            String notes = String.format("Pagamento %s - Order ID: %s - Cartão: %s****%s",
                orderStatus != null ? orderStatus.toUpperCase() : "UNKNOWN",
                orderResponse.getId(),
                card.getBrand(),
                card.getLastFourDigits());
            payment.setNotes(notes);
            log.info("📝 Notes preenchido: {}", notes);
            
            if (finalStatus == PaymentStatus.PAID) {
                payment.setPaymentDate(LocalDateTime.now());
                delivery.setPaymentCaptured(true);
                delivery.setPaymentCompleted(true);
                deliveryRepository.save(delivery);
                log.info("✅ Pagamento com cartão aprovado imediatamente - Delivery: {}", delivery.getId());
            } else if (finalStatus == PaymentStatus.FAILED) {
                log.warn("❌ Pagamento FAILED - Order Status: {}", orderStatus);
                
                // Enviar notificação push para o cliente informando falha
                try {
                    String failureMessage = extractPaymentFailureMessage(orderResponse);
                    String notificationBody = String.format("Pagamento de R$ %.2f não foi aprovado. %s Por favor, escolha outro método de pagamento.", 
                        delivery.getShippingFee(), failureMessage);
                    
                    java.util.Map<String, Object> notificationData = new java.util.HashMap<>();
                    notificationData.put("type", "payment_failed");
                    notificationData.put("deliveryId", delivery.getId());
                    notificationData.put("paymentId", payment.getId());
                    notificationData.put("amount", delivery.getShippingFee().toString());
                    notificationData.put("failureReason", failureMessage);
                    
                    boolean sent = pushNotificationService.sendNotificationToUser(
                        delivery.getClient().getId(),
                        "❌ Pagamento não aprovado",
                        notificationBody,
                        notificationData
                    );
                    
                    if (sent) {
                        log.info("📱 Notificação de falha enviada para cliente {}", delivery.getClient().getId());
                    } else {
                        log.warn("⚠️ Não foi possível enviar notificação - cliente {} sem token push ativo", delivery.getClient().getId());
                    }
                } catch (Exception notifError) {
                    log.error("Erro ao enviar notificação de falha: {}", notifError.getMessage());
                }
            } else {
                log.info("⏳ Pagamento com cartão pendente de aprovação - Delivery: {}", delivery.getId());
            }
            
            payment = paymentRepository.save(payment);
            log.info("💾 Payment salvo - ID: {}, Provider ID: {}, Status: {}, Request length: {}, Response length: {}",
                payment.getId(), 
                payment.getProviderPaymentId(),
                payment.getRequest() != null ? payment.getRequest().length() : 0,
                payment.getResponse() != null ? payment.getResponse().length() : 0);
            
            // Construir response customizado com dados do cartão
            PaymentResponse response = PaymentResponse.from(payment);
            response.setCardLastFour(card.getLastFourDigits());
            response.setCardBrand(card.getBrand().name());
            
            return response;
            
        } catch (Exception e) {
            log.error("❌ Erro ao processar pagamento com cartão - Delivery: {}", delivery.getId(), e);
            
            // Salvar payment FAILED em transação separada
            saveFailedPayment(delivery.getShippingFee(), PaymentMethod.CREDIT_CARD, 
                    delivery.getClient(), delivery, e.getMessage());
            
            throw new RuntimeException("Erro ao processar pagamento: " + e.getMessage(), e);
        }
    }

    /**
     * Constrói request de order para Pagar.me com cartão tokenizado
     */
    private com.mvt.mvt_events.payment.dto.OrderRequest buildCardOrderRequest(
            Delivery delivery, 
            com.mvt.mvt_events.jpa.CustomerCard card) {
        
        // Buscar configuração de splits
        SiteConfiguration config = siteConfigurationRepository.findActiveConfiguration()
                .orElseThrow(() -> new IllegalStateException("Configuração do site não encontrada"));
        
        BigDecimal amountCents = splitCalculator.toCents(delivery.getShippingFee());
        
        // Calcular splits
        boolean hasOrganizer = delivery.getOrganizer() != null && 
                               delivery.getOrganizer().getPagarmeRecipientId() != null;
        
        BigDecimal courierAmountCents = splitCalculator.calculateCourierAmount(amountCents, config);
        BigDecimal organizerAmountCents = hasOrganizer ? 
                splitCalculator.calculateOrganizerAmount(amountCents, config) : BigDecimal.ZERO;
        BigDecimal platformAmountCents = splitCalculator.calculatePlatformAmount(
                amountCents, courierAmountCents, organizerAmountCents);
        
        // Construir request
        com.mvt.mvt_events.payment.dto.OrderRequest orderRequest = 
                new com.mvt.mvt_events.payment.dto.OrderRequest();
        
        // Item
        com.mvt.mvt_events.payment.dto.OrderRequest.ItemRequest item = 
                new com.mvt.mvt_events.payment.dto.OrderRequest.ItemRequest();
        item.setCode(String.valueOf(delivery.getId()));
        item.setDescription("Entrega #" + delivery.getId());
        item.setAmount(amountCents.longValue());
        item.setQuantity(1L);
        orderRequest.setItems(List.of(item));
        
        // Customer
        com.mvt.mvt_events.payment.dto.OrderRequest.CustomerRequest customer = 
                new com.mvt.mvt_events.payment.dto.OrderRequest.CustomerRequest();
        customer.setName(delivery.getClient().getName());
        customer.setEmail(delivery.getClient().getUsername());
        customer.setType("individual");
        orderRequest.setCustomer(customer);
        
        // Pagamento com cartão tokenizado
        com.mvt.mvt_events.payment.dto.OrderRequest.PaymentRequest payment = 
                new com.mvt.mvt_events.payment.dto.OrderRequest.PaymentRequest();
        payment.setPaymentMethod("credit_card");
        
        // Cartão tokenizado
        com.mvt.mvt_events.payment.dto.OrderRequest.CreditCardRequest creditCard = 
                new com.mvt.mvt_events.payment.dto.OrderRequest.CreditCardRequest();
        creditCard.setCardId(card.getPagarmeCardId()); // Usar cartão salvo
        creditCard.setOperationType("auth_and_capture"); // Capturar imediatamente
        creditCard.setInstallments(1); // Sem parcelamento
        payment.setCreditCard(creditCard);
        
        // Splits
        List<com.mvt.mvt_events.payment.dto.OrderRequest.SplitRequest> splits = new ArrayList<>();
        
        // Courier (87% padrão)
        com.mvt.mvt_events.payment.dto.OrderRequest.SplitRequest courierSplit = 
                new com.mvt.mvt_events.payment.dto.OrderRequest.SplitRequest();
        courierSplit.setRecipientId(delivery.getCourier().getPagarmeRecipientId());
        courierSplit.setAmount(courierAmountCents.intValue());
        courierSplit.setType("flat");
        
        com.mvt.mvt_events.payment.dto.OrderRequest.SplitOptionsRequest courierOptions = 
                new com.mvt.mvt_events.payment.dto.OrderRequest.SplitOptionsRequest();
        courierOptions.setChargeProcessingFee(false);
        courierOptions.setChargeRemainderFee(false);
        courierOptions.setLiable(false);
        courierSplit.setOptions(courierOptions);
        splits.add(courierSplit);
        
        // Organizer (5% se existir)
        if (hasOrganizer) {
            com.mvt.mvt_events.payment.dto.OrderRequest.SplitRequest organizerSplit = 
                    new com.mvt.mvt_events.payment.dto.OrderRequest.SplitRequest();
            organizerSplit.setRecipientId(delivery.getOrganizer().getPagarmeRecipientId());
            organizerSplit.setAmount(organizerAmountCents.intValue());
            organizerSplit.setType("flat");
            
            com.mvt.mvt_events.payment.dto.OrderRequest.SplitOptionsRequest organizerOptions = 
                    new com.mvt.mvt_events.payment.dto.OrderRequest.SplitOptionsRequest();
            organizerOptions.setChargeProcessingFee(false);
            organizerOptions.setChargeRemainderFee(false);
            organizerOptions.setLiable(false);
            organizerSplit.setOptions(organizerOptions);
            splits.add(organizerSplit);
        }
        
        // Platform (resto, liable=true)
        com.mvt.mvt_events.payment.dto.OrderRequest.SplitRequest platformSplit = 
                new com.mvt.mvt_events.payment.dto.OrderRequest.SplitRequest();
        platformSplit.setRecipientId(config.getPagarmeRecipientId());
        platformSplit.setAmount(platformAmountCents.intValue());
        platformSplit.setType("flat");
        
        com.mvt.mvt_events.payment.dto.OrderRequest.SplitOptionsRequest platformOptions = 
                new com.mvt.mvt_events.payment.dto.OrderRequest.SplitOptionsRequest();
        platformOptions.setChargeProcessingFee(true);
        platformOptions.setChargeRemainderFee(true);
        platformOptions.setLiable(true);
        platformSplit.setOptions(platformOptions);
        splits.add(platformSplit);
        
        payment.setSplit(splits);
        orderRequest.setPayments(List.of(payment));
        
        return orderRequest;
    }
    
    /**
     * Constrói request de order para Pagar.me com PIX
     * @param deliveries Lista de deliveries
     * @param client Cliente pagador
     * @param expiresInSeconds Tempo de expiração em segundos (ex: 300 = 5 min, 7200 = 2h)
     */
    private OrderRequest buildPixOrderRequest(List<Delivery> deliveries, User client, int expiresInSeconds) {
        // Buscar configuração de splits
        SiteConfiguration config = siteConfigurationRepository.findActiveConfiguration()
                .orElseThrow(() -> new IllegalStateException("Configuração do site não encontrada"));
        
        // Calcular valor total
        BigDecimal totalAmount = deliveries.stream()
                .map(Delivery::getShippingFee)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalAmountCents = splitCalculator.toCents(totalAmount);
        
        // Construir request
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setClosed(true); // Fechar order automaticamente
        
        // Items - uma para cada delivery
        List<OrderRequest.ItemRequest> items = new ArrayList<>();
        for (Delivery delivery : deliveries) {
            OrderRequest.ItemRequest item = new OrderRequest.ItemRequest();
            item.setCode(String.valueOf(delivery.getId()));
            item.setDescription("Entrega #" + delivery.getId() + " - " + 
                    delivery.getFromAddress() + " → " + delivery.getToAddress());
            item.setAmount(splitCalculator.toCents(delivery.getShippingFee()).longValue());
            item.setQuantity(1L);
            items.add(item);
        }
        orderRequest.setItems(items);
        
        // Customer
        OrderRequest.CustomerRequest customer = new OrderRequest.CustomerRequest();
        customer.setName(client.getName());
        customer.setEmail(client.getUsername());
        customer.setType("individual");
        
        // Document (CPF sem pontuação)
        if (client.getDocumentNumber() != null) {
            customer.setDocument(client.getDocumentNumber().replaceAll("[^0-9]", ""));
        }
        
        orderRequest.setCustomer(customer);
        
        // Pagamento PIX
        OrderRequest.PaymentRequest payment = new OrderRequest.PaymentRequest();
        payment.setPaymentMethod("pix");
        payment.setAmount(totalAmountCents.longValue());
        
        // Configuração PIX
        OrderRequest.PixRequest pix = new OrderRequest.PixRequest();
        pix.setExpiresIn(String.valueOf(expiresInSeconds));
        
        // Additional info
        List<OrderRequest.AdditionalInfoRequest> additionalInfo = new ArrayList<>();
        OrderRequest.AdditionalInfoRequest info = new OrderRequest.AdditionalInfoRequest();
        info.setName("Entregas");
        info.setValue(deliveries.stream()
                .map(d -> "#" + d.getId())
                .collect(Collectors.joining(", ")));
        additionalInfo.add(info);
        pix.setAdditionalInformation(additionalInfo);
        
        payment.setPix(pix);
        
        // Splits - acumular por recipient (caso múltiplas deliveries do mesmo courier)
        Map<String, SplitAccumulator> splitsMap = new HashMap<>();
        
        for (Delivery delivery : deliveries) {
            BigDecimal deliveryAmountCents = splitCalculator.toCents(delivery.getShippingFee());
            
            boolean hasOrganizer = delivery.getOrganizer() != null && 
                                   delivery.getOrganizer().getPagarmeRecipientId() != null;
            
            BigDecimal courierAmountCents = delivery.getCourier() != null ?
                    splitCalculator.calculateCourierAmount(deliveryAmountCents, config) : BigDecimal.ZERO;
            BigDecimal organizerAmountCents = hasOrganizer ?
                    splitCalculator.calculateOrganizerAmount(deliveryAmountCents, config) : BigDecimal.ZERO;
            BigDecimal platformAmountCents = splitCalculator.calculatePlatformAmount(
                    deliveryAmountCents, courierAmountCents, organizerAmountCents);
            
            // Courier
            if (delivery.getCourier() != null) {
                String courierId = delivery.getCourier().getPagarmeRecipientId();
                splitsMap.computeIfAbsent(courierId, k -> new SplitAccumulator("COURIER", courierId))
                        .addAmount(courierAmountCents);
            }
            
            // Organizer
            if (hasOrganizer) {
                String organizerId = delivery.getOrganizer().getPagarmeRecipientId();
                splitsMap.computeIfAbsent(organizerId, k -> new SplitAccumulator("ORGANIZER", organizerId))
                        .addAmount(organizerAmountCents);
            }
            
            // Platform
            String platformId = config.getPagarmeRecipientId();
            splitsMap.computeIfAbsent(platformId, k -> new SplitAccumulator("PLATFORM", platformId))
                    .addAmount(platformAmountCents);
        }
        
        // Converter Map para List de SplitRequest
        List<OrderRequest.SplitRequest> splits = new ArrayList<>();
        
        for (SplitAccumulator accumulator : splitsMap.values()) {
            OrderRequest.SplitRequest split = new OrderRequest.SplitRequest();
            split.setRecipientId(accumulator.recipientId);
            split.setAmount(accumulator.totalAmount.intValue());
            split.setType("flat");
            
            OrderRequest.SplitOptionsRequest options = new OrderRequest.SplitOptionsRequest();
            
            if ("PLATFORM".equals(accumulator.role)) {
                options.setChargeProcessingFee(true);
                options.setChargeRemainderFee(true);
                options.setLiable(true);
            } else {
                options.setChargeProcessingFee(false);
                options.setChargeRemainderFee(false);
                options.setLiable(false);
            }
            
            split.setOptions(options);
            splits.add(split);
        }
        
        payment.setSplit(splits);
        orderRequest.setPayments(List.of(payment));
        
        return orderRequest;
    }
    
    /**
     * Helper class para acumular splits por recipient
     */
    private static class SplitAccumulator {
        String role;
        String recipientId;
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        SplitAccumulator(String role, String recipientId) {
            this.role = role;
            this.recipientId = recipientId;
        }
        
        void addAmount(BigDecimal amount) {
            this.totalAmount = this.totalAmount.add(amount);
        }
    }
    
    /**
     * Determina o status final do Payment baseado na OrderResponse do Pagar.me.
     * 
     * Regras:
     * - Status "paid" → PAID
     * - Status "failed", "canceled", "cancelled" → FAILED
     * - Transação com status "not_authorized", "refused", "failed" → FAILED
     * - Antifraude "reproved" → FAILED
     * - Caso contrário → PENDING
     * 
     * @param orderResponse Response recebida do Pagar.me
     * @return PaymentStatus apropriado
     */
    private PaymentStatus determinePaymentStatusFromOrder(OrderResponse orderResponse) {
        if (orderResponse == null) {
            return PaymentStatus.PENDING;
        }
        
        String orderStatus = orderResponse.getStatus();
        
        // 1. Verificar status da order
        if ("paid".equalsIgnoreCase(orderStatus)) {
            return PaymentStatus.PAID;
        }
        
        if ("failed".equalsIgnoreCase(orderStatus) || 
            "canceled".equalsIgnoreCase(orderStatus) || 
            "cancelled".equalsIgnoreCase(orderStatus)) {
            return PaymentStatus.FAILED;
        }
        
        // 2. Verificar charges e última transação
        if (orderResponse.getCharges() != null && !orderResponse.getCharges().isEmpty()) {
            OrderResponse.Charge charge = orderResponse.getCharges().get(0);
            
            // Status da charge
            if ("failed".equalsIgnoreCase(charge.getStatus())) {
                log.warn("🚫 Charge com status FAILED");
                return PaymentStatus.FAILED;
            }
            
            // Última transação
            if (charge.getLastTransaction() != null) {
                OrderResponse.LastTransaction transaction = charge.getLastTransaction();
                
                // Status da transação
                String txStatus = transaction.getStatus();
                if ("not_authorized".equalsIgnoreCase(txStatus) ||
                    "refused".equalsIgnoreCase(txStatus) ||
                    "failed".equalsIgnoreCase(txStatus)) {
                    log.warn("🚫 Transação com status de falha: {}", txStatus);
                    return PaymentStatus.FAILED;
                }
                
                // Success flag
                if (transaction.getSuccess() != null && !transaction.getSuccess()) {
                    log.warn("🚫 Transação marcada como success=false");
                    return PaymentStatus.FAILED;
                }
                
                // Antifraude (pode estar como Map ou objeto complexo)
                if (transaction.getAntifraudResponse() != null) {
                    try {
                        // Tentar como Map
                        if (transaction.getAntifraudResponse() instanceof java.util.Map) {
                            @SuppressWarnings("unchecked")
                            java.util.Map<String, Object> antifraudMap = 
                                (java.util.Map<String, Object>) transaction.getAntifraudResponse();
                            Object status = antifraudMap.get("status");
                            if ("reproved".equalsIgnoreCase(String.valueOf(status))) {
                                log.warn("🚫 Antifraude reprovou a transação");
                                return PaymentStatus.FAILED;
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Erro ao verificar antifraude: {}", e.getMessage());
                    }
                }
            }
        }
        
        // Default: PENDING
        return PaymentStatus.PENDING;
    }
    
    /**
     * Extrai mensagem de erro amigável da OrderResponse para mostrar ao cliente.
     * 
     * Traduz status da transação:
     * - not_authorized → "Transação não autorizada"
     * - refused → "Transação recusada"  
     * - failed → "Transação falhou"
     * - antifraud reproved → "Transação bloqueada por segurança"
     * 
     * @param orderResponse Response do Pagar.me
     * @return Mensagem amigável em português
     */
    public String extractPaymentFailureMessage(OrderResponse orderResponse) {
        if (orderResponse == null) {
            return "Pagamento não processado";
        }
        
        // Verificar charges e última transação
        if (orderResponse.getCharges() != null && !orderResponse.getCharges().isEmpty()) {
            OrderResponse.Charge charge = orderResponse.getCharges().get(0);
            
            if (charge.getLastTransaction() != null) {
                OrderResponse.LastTransaction transaction = charge.getLastTransaction();
                
                // 1. Verificar antifraude primeiro
                if (transaction.getAntifraudResponse() != null) {
                    try {
                        if (transaction.getAntifraudResponse() instanceof java.util.Map) {
                            @SuppressWarnings("unchecked")
                            java.util.Map<String, Object> antifraudMap = 
                                (java.util.Map<String, Object>) transaction.getAntifraudResponse();
                            Object status = antifraudMap.get("status");
                            if ("reproved".equalsIgnoreCase(String.valueOf(status))) {
                                return "Transação bloqueada por segurança";
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Erro ao verificar antifraude para mensagem: {}", e.getMessage());
                    }
                }
                
                // 2. Traduzir status da transação
                String txStatus = transaction.getStatus();
                if (txStatus != null) {
                    switch (txStatus.toLowerCase()) {
                        case "not_authorized":
                            return "Transação não autorizada";
                        case "refused":
                            return "Transação recusada";
                        case "failed":
                            return "Transação falhou";
                        case "authorized":
                        case "paid":
                            return "Transação aprovada";
                        default:
                            return "Transação " + txStatus;
                    }
                }
            }
        }
        
        // Fallback para status da order
        String orderStatus = orderResponse.getStatus();
        if ("failed".equalsIgnoreCase(orderStatus)) {
            return "Pagamento não processado";
        }
        
        return "Status do pagamento: " + (orderStatus != null ? orderStatus : "desconhecido");
    }
}
