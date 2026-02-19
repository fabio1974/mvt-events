package com.mvt.mvt_events.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mvt.mvt_events.jpa.*;
import com.mvt.mvt_events.payment.service.PagarMeService;
import com.mvt.mvt_events.payment.dto.OrderRequest;
import com.mvt.mvt_events.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Serviço de consolidação automática de pagamentos para deliveries completadas.
 * 
 * Funcionalidade Principal:
 * - Procura deliveries COMPLETED que não têm payments PAID
 * - Agrupa por CLIENT
 * - Filtra apenas deliveries com payments NULL, FAILED ou EXPIRED
 * - Cria um único pagamento consolidado com split automático
 * - Associa o pagamento a todas as deliveries do grupo
 * - Cria order no Pagar.me com PIX
 * 
 * Execução:
 * - Agendado via @Scheduled (cada 4 horas)
 * - Transacional (tudo ou nada)
 * - Com logging detalhado
 * 
 * Split de valores:
 * - Agrupa por COURIER + ORGANIZER (gerente)
 * - Soma todos os fretes envolvidos
 * - Aplica percentuais da SiteConfiguration
 * - Cria recipients no Pagar.me se não existirem
 * 
 * @see ConsolidatedPaymentScheduler
 * @see SiteConfiguration
 * @see Payment
 * @see Delivery
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsolidatedPaymentService {

    private final PaymentRepository paymentRepository;
    private final DeliveryRepository deliveryRepository;
    private final PagarMeService pagarMeService;
    private final SiteConfigurationService siteConfigService;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final ConsolidatedPaymentTaskTracker taskTracker;
    private final ObjectMapper objectMapper;
    private final PaymentSplitCalculator splitCalculator;
    private final PushNotificationService pushNotificationService;
    private final PaymentService paymentService;

    /**
     * Processa consolidação de pagamentos para TODOS os clientes
     * 
     * Fluxo:
     * 1. Busca todos os clientes com deliveries completadas não pagas
     * 2. Para cada cliente:
     *    a. Busca todas as deliveries COMPLETED
     *    b. Filtra apenas as que NÃO têm payments PAID
     *    c. Filtra apenas as que têm payments NULL, FAILED ou EXPIRED
     *    d. Se houver deliveries, cria pagamento consolidado
     *    e. Persiste localmente
     *    f. Cria order no Pagar.me
     * 
     * @return Map com estatísticas de processamento
     */
    @Transactional
    public Map<String, Object> processAllClientsConsolidatedPayments(String taskId) {
        log.info("🚀 Iniciando processamento de pagamentos consolidados - TaskID: {}", taskId);
        
        if (taskId != null) {
            taskTracker.markAsProcessing(taskId);
        }
        
        Map<String, Object> stats = new HashMap<>();
        int totalClientsProcessed = 0;
        int totalPaymentsCreated = 0;
        int totalDeliveriesIncluded = 0;
        List<String> errors = new ArrayList<>();

        try {
            // 1. Buscar todos os clientes com deliveries completadas
            List<UUID> clientIds = deliveryRepository.findClientsWithCompletedDeliveries();
            log.info("   📊 {} clientes com deliveries completadas encontrados", clientIds.size());

            // 2. Processar cada cliente
            for (int i = 0; i < clientIds.size(); i++) {
                UUID clientId = clientIds.get(i);
                try {
                    // Atualizar progresso
                    if (taskId != null) {
                        int progress = 10 + (int) (80.0 * i / clientIds.size());
                        taskTracker.updateProgress(taskId, progress, 
                            String.format("Processando cliente %d de %d", i + 1, clientIds.size()));
                    }

                    Map<String, Object> clientStats = processClientConsolidatedPayments(clientId);
                    
                    if ((Boolean) clientStats.get("processed")) {
                        totalClientsProcessed++;
                        totalPaymentsCreated += (Integer) clientStats.get("paymentsCreated");
                        totalDeliveriesIncluded += (Integer) clientStats.get("deliveriesIncluded");
                        
                        log.info("   ✅ Cliente {}: {} pagamentos, {} deliveries", 
                            clientId, 
                            clientStats.get("paymentsCreated"),
                            clientStats.get("deliveriesIncluded"));
                    }
                } catch (Exception e) {
                    String error = String.format("Cliente %s: %s", clientId, e.getMessage());
                    errors.add(error);
                    log.error("   ❌ Erro processando cliente {}: {}", clientId, e.getMessage(), e);
                }
            }

            // Compilar estatísticas finais
            stats.put("processedClients", totalClientsProcessed);
            stats.put("createdPayments", totalPaymentsCreated);
            stats.put("includedDeliveries", totalDeliveriesIncluded);
            stats.put("errors", errors);
            stats.put("timestamp", LocalDateTime.now());

            log.info("✨ Processamento concluído: {} clientes, {} pagamentos, {} deliveries",
                totalClientsProcessed, totalPaymentsCreated, totalDeliveriesIncluded);

            if (taskId != null) {
                taskTracker.markAsCompleted(taskId, stats);
            }

            return stats;

        } catch (Exception e) {
            log.error("❌ Erro geral ao processar pagamentos consolidados", e);
            stats.put("error", e.getMessage());
            stats.put("errors", errors);
            stats.put("timestamp", LocalDateTime.now());
            
            if (taskId != null) {
                taskTracker.markAsFailed(taskId, e.getMessage(), errors);
            }
            
            return stats;
        }
    }

    /**
     * Processa consolidação de pagamentos para TODOS os clientes (sem task tracking)
     * 
     * Fluxo:
     * 1. Busca todos os clientes com deliveries completadas não pagas
     * 2. Para cada cliente:
     *    a. Busca todas as deliveries COMPLETED
     *    b. Filtra apenas as que NÃO têm payments PAID
     *    c. Filtra apenas as que têm payments NULL, FAILED ou EXPIRED
     *    d. Se houver deliveries, cria pagamento consolidado
     *    e. Persiste localmente
     *    f. Cria order no Pagar.me
     * 
     * @return Map com estatísticas de processamento
     */
    @Transactional
    public Map<String, Object> processAllClientsConsolidatedPayments() {
        log.info("🚀 Iniciando processamento de pagamentos consolidados");
        
        Map<String, Object> stats = new HashMap<>();
        int totalClientsProcessed = 0;
        int totalPaymentsCreated = 0;
        int totalDeliveriesIncluded = 0;
        List<String> errors = new ArrayList<>();

        try {
            // 1. Buscar todos os clientes com deliveries completadas
            List<UUID> clientIds = deliveryRepository.findClientsWithCompletedDeliveries();
            log.info("   📊 {} clientes com deliveries completadas encontrados", clientIds.size());

            // 2. Processar cada cliente
            for (UUID clientId : clientIds) {
                try {
                    Map<String, Object> clientStats = processClientConsolidatedPayments(clientId);
                    
                    if ((Boolean) clientStats.get("processed")) {
                        totalClientsProcessed++;
                        totalPaymentsCreated += (Integer) clientStats.get("paymentsCreated");
                        totalDeliveriesIncluded += (Integer) clientStats.get("deliveriesIncluded");
                        
                        log.info("   ✅ Cliente {}: {} pagamentos, {} deliveries", 
                            clientId, 
                            clientStats.get("paymentsCreated"),
                            clientStats.get("deliveriesIncluded"));
                    }
                } catch (Exception e) {
                    String error = String.format("Cliente %s: %s", clientId, e.getMessage());
                    errors.add(error);
                    log.error("   ❌ Erro processando cliente {}: {}", clientId, e.getMessage(), e);
                }
            }

            // Compilar estatísticas finais
            stats.put("processedClients", totalClientsProcessed);
            stats.put("createdPayments", totalPaymentsCreated);
            stats.put("includedDeliveries", totalDeliveriesIncluded);
            stats.put("errors", errors);
            stats.put("timestamp", LocalDateTime.now());

            log.info("✨ Processamento concluído: {} clientes, {} pagamentos, {} deliveries",
                totalClientsProcessed, totalPaymentsCreated, totalDeliveriesIncluded);

            return stats;

        } catch (Exception e) {
            log.error("❌ Erro geral ao processar pagamentos consolidados", e);
            stats.put("error", e.getMessage());
            stats.put("errors", errors);
            stats.put("timestamp", LocalDateTime.now());
            return stats;
        }
    }

    /**
     * Processa consolidação de pagamentos para UM cliente específico
     * 
     * @param clientId UUID do cliente
     * @return Map com estatísticas (processed, paymentsCreated, deliveriesIncluded)
     */
    @Transactional
    public Map<String, Object> processClientConsolidatedPayments(UUID clientId) {
        log.info("👤 Processando pagamentos consolidados para cliente: {}", clientId);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("processed", false);
        stats.put("paymentsCreated", 0);
        stats.put("deliveriesIncluded", 0);

        try {
            // 1. Buscar cliente
            User client = userRepository.findById(clientId)
                    .orElseThrow(() -> new RuntimeException("Cliente não encontrado: " + clientId));

            // 2. Buscar deliveries COMPLETED deste cliente
            List<Delivery> completedDeliveries = deliveryRepository.findByClientIdAndStatusWithJoins(
                    clientId, 
                    Delivery.DeliveryStatus.COMPLETED
            );
            
            if (completedDeliveries.isEmpty()) {
                log.debug("   └─ Nenhuma delivery COMPLETED encontrada");
                return stats;
            }

            // 3. Filtrar deliveries que NÃO têm payment PAID
            List<Delivery> deliveriesToPay = completedDeliveries.stream()
                    .filter(d -> !hasActivePaidPayment(d))
                    .collect(Collectors.toList());

            if (deliveriesToPay.isEmpty()) {
                log.debug("   └─ Todas as deliveries já possuem payment PAID");
                return stats;
            }

            log.info("   📦 {} deliveries a pagar encontradas", deliveriesToPay.size());

            // 4. Filtrar deliveries que podem ser reprocessadas
            // Incluir apenas: NULL (sem payment), FAILED ou EXPIRED
            List<Delivery> deliverisWithPaymentProblems = deliveriesToPay.stream()
                    .filter(d -> {
                        Set<PaymentStatus> paymentStatuses = d.getPayments().stream()
                                .map(Payment::getStatus)
                                .collect(Collectors.toSet());
                        // Manter se: sem payments OU tem FAILED/EXPIRED
                        return paymentStatuses.isEmpty() || 
                               paymentStatuses.stream().anyMatch(s -> 
                                   s == PaymentStatus.FAILED || 
                                   s == PaymentStatus.EXPIRED);
                    })
                    .collect(Collectors.toList());

            if (deliverisWithPaymentProblems.isEmpty()) {
                log.debug("   └─ Nenhuma delivery com payment FAILED/EXPIRED");
                return stats;
            }

            log.info("   💰 {} deliveries com pagamento a resolver", deliverisWithPaymentProblems.size());

            // 5. Criar pagamento consolidado
            Payment consolidatedPayment = createConsolidatedPayment(
                    client, 
                    deliverisWithPaymentProblems
            );

            // 6. Atualizar estatísticas
            stats.put("processed", true);
            stats.put("paymentsCreated", 1);
            stats.put("deliveriesIncluded", deliverisWithPaymentProblems.size());

            log.info("   ✅ Pagamento consolidado criado: ID {}", consolidatedPayment.getId());

            return stats;

        } catch (Exception e) {
            log.error("   ❌ Erro ao processar pagamentos do cliente {}: {}", clientId, e.getMessage());
            stats.put("error", e.getMessage());
            throw new RuntimeException("Erro ao processar cliente " + clientId, e);
        }
    }

    /**
     * Cria um pagamento consolidado para múltiplas deliveries
     * 
     * Fluxo:
     * 1. Calcula valor total (soma fretes)
     * 2. Calcula split por courier+organizer
     * 3. Cria recipients no Pagar.me se não existirem
     * 4. Persiste Payment local
     * 5. Cria order no Pagar.me
     * 6. Associa a todas as deliveries
     * 
     * @param client Cliente (payer)
     * @param deliveries Lista de deliveries a consolidar
     * @return Payment criado
     */
    @Transactional
    private Payment createConsolidatedPayment(User client, List<Delivery> deliveries) {
        log.info("💳 Criando pagamento consolidado para {} deliveries", deliveries.size());

        // 1. Calcular valor total (baseado no FRETE - shippingFee)
        BigDecimal totalAmount = deliveries.stream()
                .map(Delivery::getShippingFee)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("   💹 Valor total (frete): R$ {}", totalAmount);

        // 2. Buscar configuração ativa
        SiteConfiguration config = siteConfigService.getActiveConfiguration();

        // 3. Calcular split por courier + organizer
        Map<String, SplitItem> splitMap = calculateSplitByRecipient(deliveries, config);
        log.info("   🔀 Split calculado: {} recipients", splitMap.size());

        // 4. Persiste pagamento local
        Payment payment = new Payment();
        payment.setPayer(client);
        payment.setAmount(totalAmount);
        payment.setCurrency(com.mvt.mvt_events.jpa.Currency.BRL);
        payment.setPaymentMethod(PaymentMethod.PIX);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setDeliveries(new ArrayList<>(deliveries));
        payment.setProvider(com.mvt.mvt_events.jpa.PaymentProvider.PAGARME);
        payment.setNotes(String.format("Pagamento consolidado para %d deliveries", deliveries.size()));

        Payment savedPayment = paymentRepository.save(payment);
        log.info("   📊 Payment local salvo: ID {}", savedPayment.getId());

        // 5. Criar order no Pagar.me
        try {
            String orderId = createPagarMeOrder(client, deliveries, totalAmount, splitMap, savedPayment);
            savedPayment.setProviderPaymentId(orderId);
            paymentRepository.save(savedPayment);
            log.info("   ✅ Order Pagar.me criada: {}", orderId);
        } catch (Exception e) {
            log.error("   ⚠️ Erro ao criar order Pagar.me (payment local salvo mesmo assim)", e);
            
            // Salvar mensagem de erro no response (se ainda não tiver sido salvo)
            if (savedPayment.getResponse() == null || savedPayment.getResponse().isEmpty()) {
                try {
                    // Extrair mensagem de erro do Pagar.me se disponível
                    String errorMessage = e.getMessage();
                    if (e.getCause() != null && e.getCause().getMessage() != null) {
                        errorMessage = e.getCause().getMessage();
                    }
                    
                    Map<String, Object> errorResponse = new java.util.HashMap<>();
                    errorResponse.put("error", true);
                    errorResponse.put("message", errorMessage);
                    errorResponse.put("timestamp", java.time.Instant.now().toString());
                    
                    String errorJson = objectMapper.writeValueAsString(errorResponse);
                    savedPayment.setResponse(errorJson);
                    log.info("✅ Erro salvo no response do payment");
                } catch (Exception jsonError) {
                    log.error("⚠️ Erro ao serializar mensagem de erro", jsonError);
                }
            }
            
            savedPayment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(savedPayment);
            
            // Enviar notificação push de falha ao cliente
            try {
                String notificationBody = String.format("Pagamento consolidado de R$ %.2f não foi aprovado. %s Por favor, escolha outro método de pagamento.", 
                    totalAmount, e.getMessage());
                
                Map<String, Object> notificationData = new java.util.HashMap<>();
                notificationData.put("type", "consolidated_payment_failed");
                notificationData.put("paymentId", savedPayment.getId());
                notificationData.put("amount", totalAmount.toString());
                notificationData.put("deliveryCount", deliveries.size());
                notificationData.put("failureReason", e.getMessage());
                
                boolean sent = pushNotificationService.sendNotificationToUser(
                    client.getId(),
                    "❌ Pagamento não aprovado",
                    notificationBody,
                    notificationData
                );
                
                if (sent) {
                    log.info("✅ Notificação de falha enviada ao cliente #{}", client.getId());
                } else {
                    log.warn("⚠️ Não foi possível enviar notificação - cliente #{} sem token push ativo", client.getId());
                }
            } catch (Exception notifError) {
                log.error("❌ Erro ao enviar notificação de falha: {}", notifError.getMessage());
            }
            
            throw new RuntimeException("Erro ao criar order Pagar.me", e);
        }

        // 6. Associar a todas as deliveries
        for (Delivery delivery : deliveries) {
            delivery.getPayments().add(savedPayment);
        }
        deliveryRepository.saveAll(deliveries);

        return savedPayment;
    }

    /**
     * Cria order no Pagar.me com split automático
     * 
     * @param client Cliente/payer
     * @param deliveries Lista de deliveries
     * @param totalAmount Valor total
     * @param splitMap Mapa de splits (recipient -> valor)
     * @param payment Payment local (para referenciar)
     * @return Order ID do Pagar.me
     */
    private String createPagarMeOrder(User client, List<Delivery> deliveries, 
                                     BigDecimal totalAmount, Map<String, SplitItem> splitMap,
                                     Payment payment) {
        log.info("📤 Criando order no Pagar.me");

        // Buscar recipientId da plataforma (da config)
        SiteConfiguration config = siteConfigService.getActiveConfiguration();
        String platformRecipientId = config.getPagarmeRecipientId();

        // Items: apenas referência às deliveries (usando FRETE - shippingFee)
        List<OrderRequest.ItemRequest> items = new ArrayList<>();
        for (Delivery d : deliveries) {
            Long amountCents = (long) (d.getShippingFee().doubleValue() * 100); // Em centavos
            items.add(OrderRequest.ItemRequest.builder()
                    .amount(amountCents)
                    .description(String.format("Frete Entrega #%d (Cliente: %s)", d.getId(), client.getName()))
                    .quantity(1L)
                    .code("DEL-" + d.getId())
                    .build());
        }

        // PIX
        OrderRequest.PixRequest.PixRequestBuilder pixBuilder = OrderRequest.PixRequest.builder()
                .expiresIn("7200");  // 2 horas

        // Split: valores absolutos em centavos para cada recipient
        List<OrderRequest.SplitRequest> splits = new ArrayList<>();
        for (SplitItem item : splitMap.values()) {
            Long amountCents = item.amount != null ? item.amount.longValue() : 0L;
            
            log.debug("   💰 Split {} ({}): R$ {} ({} centavos)", 
                item.userRole, item.userName, 
                item.amount.divide(BigDecimal.valueOf(100)),
                amountCents);
            
            // Apenas a PLATAFORMA pode ter chargeRemainderFee=true
            boolean isPlatform = platformRecipientId != null && 
                                platformRecipientId.equals(item.pagarmeRecipientId);
            
            OrderRequest.SplitRequest split = OrderRequest.SplitRequest.builder()
                    .amount(amountCents.intValue())  // Valor absoluto em centavos
                    .recipientId(item.pagarmeRecipientId)
                    .type("flat")  // flat = valor absoluto (não percentual)
                    .options(OrderRequest.SplitOptionsRequest.builder()
                            .chargeProcessingFee(item.isLiable)
                            .chargeRemainderFee(isPlatform)  // Apenas plataforma recebe remainder
                            .liable(item.isLiable)
                            .build())
                    .build();
            splits.add(split);
        }

        // Payment
        OrderRequest.PaymentRequest paymentRequest = OrderRequest.PaymentRequest.builder()
                .paymentMethod("pix")
                .pix(pixBuilder.build())
                .split(splits)
                .build();

        // Order
        OrderRequest orderRequest = OrderRequest.builder()
                .closed(true)
                .items(items)
                .customer(buildCustomerRequest(client))
                .payments(List.of(paymentRequest))
                .build();

        // Salvar REQUEST antes de enviar (para ter em caso de erro)
        try {
            String requestJson = objectMapper.writeValueAsString(orderRequest);
            payment.setRequest(requestJson);
            paymentRepository.save(payment);
            log.info("✅ Request salvo no payment antes do envio (tamanho: {} bytes)", requestJson.length());
        } catch (Exception e) {
            log.error("⚠️ Erro ao serializar request antes do envio", e);
        }

        // Criar order e capturar response completo
        com.mvt.mvt_events.payment.dto.OrderResponse orderResponse = pagarMeService.createOrderWithFullResponse(orderRequest);
        
        // Salvar response completo e gateway_response no payment para auditoria
        try {
            // Salvar RESPONSE completo (o que o Pagar.me retornou)
            String responseJson = objectMapper.writeValueAsString(orderResponse);
            payment.setResponse(responseJson);
            log.info("✅ Response completo salvo no payment (tamanho: {} bytes)", responseJson.length());
            log.info("📋 Response status: {}, charges: {}", 
                orderResponse.getStatus(), 
                orderResponse.getCharges() != null ? orderResponse.getCharges().size() : 0);
            
            // Mapear e salvar STATUS do Pagar.me para PaymentStatus
            PaymentStatus mappedStatus = mapPagarMeStatusToPaymentStatus(orderResponse.getStatus());
            payment.setStatus(mappedStatus);
            log.info("✅ Status mapeado: '{}' (Pagar.me) -> {} (PaymentStatus)", 
                orderResponse.getStatus(), mappedStatus);
            
            // Extrair e salvar qr_code, qr_code_url, pix_qr_code e pix_qr_code_url de charges[0].last_transaction
            if (orderResponse.getCharges() != null && !orderResponse.getCharges().isEmpty()) {
                var firstCharge = orderResponse.getCharges().get(0);
                log.info("📋 First charge - status: {}, lastTransaction presente: {}", 
                    firstCharge.getStatus(), 
                    firstCharge.getLastTransaction() != null);
                
                if (firstCharge.getLastTransaction() != null) {
                    var transaction = firstCharge.getLastTransaction();
                    log.info("📋 Transaction - status: {}, gatewayResponse presente: {}, qrCode presente: {}", 
                        transaction.getStatus(), 
                        transaction.getGatewayResponse() != null,
                        transaction.getQrCode() != null);
                    
                    // Salvar pixQrCode e pixQrCodeUrl
                    if (transaction.getQrCode() != null) {
                        payment.setPixQrCode(transaction.getQrCode());
                        log.info("✅ PIX QR Code salvo no payment (tamanho: {} caracteres)", 
                            transaction.getQrCode().length());
                    }
                    
                    if (transaction.getQrCodeUrl() != null) {
                        payment.setPixQrCodeUrl(transaction.getQrCodeUrl());
                        log.info("✅ PIX QR Code URL salvo no payment: {}", transaction.getQrCodeUrl());
                    }
                } else {
                    log.warn("⚠️ lastTransaction é NULL no charge");
                }
            } else {
                log.warn("⚠️ Nenhum charge encontrado na response");
            }
        } catch (Exception e) {
            log.error("⚠️ Erro ao serializar request/response/gateway_response", e);
        }

        return orderResponse.getId();
    }

    /**
     * Calcula split de valores por recipient (courier + organizer)
     * 
     * Para cada delivery:
     * - Organizer recebe organizerPercentage% do frete (ex: 5%)
     * - Courier recebe (100% - organizerPercentage - platformPercentage)% do frete (ex: 87%)
     * - Plataforma FICA COM O RESTO automaticamente (platformPercentage%, ex: 8%)
     *   NÃO é enviado no split - o Pagar.me retém automaticamente
     * 
     * Depois soma os valores por pessoa (mesmo courier em múltiplas deliveries)
     * 
     * @param deliveries Deliveries a consolidar
     * @param config Configuração ativa
     * @return Map<recipientKey, SplitItem> com valores em CENTAVOS (apenas courier + organizer)
     */
    private Map<String, SplitItem> calculateSplitByRecipient(List<Delivery> deliveries, 
                                                             SiteConfiguration config) {
        log.info("🧮 Calculando split de {} deliveries", deliveries.size());
        
        Map<String, SplitItem> splitMap = new HashMap<>();
        
        // Percentuais da config (valores entre 0 e 100)
        BigDecimal courierPercentage = splitCalculator.calculateCourierPercentage(config);
        BigDecimal organizerPercentage = config.getOrganizerPercentage();
        String platformRecipientId = config.getPagarmeRecipientId();
        
        if (platformRecipientId != null && !platformRecipientId.isBlank()) {
            log.info("   📊 Percentuais base: Courier={}%, Organizer={}%, Plataforma={}% (split explícito: {})",
                    courierPercentage, organizerPercentage, config.getPlatformPercentage(), platformRecipientId);
        } else {
            log.info("   📊 Percentuais base: Courier={}%, Organizer={}%, Plataforma={}% (retido automaticamente)",
                    courierPercentage, organizerPercentage, config.getPlatformPercentage());
        }
        
        // Processar cada delivery individualmente
        for (Delivery delivery : deliveries) {
            User courier = delivery.getCourier();
            BigDecimal deliveryAmount = delivery.getShippingFee();
            BigDecimal deliveryAmountCents = splitCalculator.toCents(deliveryAmount);
            
            log.debug("   📦 Delivery #{} - Frete: R$ {} ({} centavos)", 
                delivery.getId(), deliveryAmount, deliveryAmountCents.longValue());
            
            // Verificar se há organizer válido
            boolean hasOrganizer = splitCalculator.hasValidOrganizer(delivery);
            
            // Split para Courier
            if (courier != null && courier.getPagarmeRecipientId() != null) {
                String courierKey = "courier-" + courier.getId();
                SplitItem courierSplit = splitMap.get(courierKey);
                
                if (courierSplit == null) {
                    courierSplit = new SplitItem();
                    courierSplit.pagarmeRecipientId = courier.getPagarmeRecipientId();
                    courierSplit.amount = BigDecimal.ZERO;
                    courierSplit.isLiable = false;
                    courierSplit.userName = courier.getName();
                    courierSplit.userRole = "COURIER";
                    splitMap.put(courierKey, courierSplit);
                }
                
                BigDecimal courierAmountCents = splitCalculator.calculateCourierAmount(deliveryAmountCents, config);
                courierSplit.amount = courierSplit.amount.add(courierAmountCents);
                
                log.debug("      💼 Courier {}: +R$ {} ({}% de R$ {})", 
                    courier.getName(), 
                    splitCalculator.toReais(courierAmountCents, 2),
                    courierPercentage,
                    deliveryAmount);
            } else {
                log.warn("      ⚠️ Courier sem pagarmeRecipientId na delivery #{}", delivery.getId());
            }
            
            // Split para Organizer (apenas se existir)
            if (hasOrganizer) {
                User organizer = delivery.getOrganizer();
                String organizerKey = "organizer-" + organizer.getId();
                SplitItem organizerSplit = splitMap.get(organizerKey);
                
                if (organizerSplit == null) {
                    organizerSplit = new SplitItem();
                    organizerSplit.pagarmeRecipientId = organizer.getPagarmeRecipientId();
                    organizerSplit.amount = BigDecimal.ZERO;
                    organizerSplit.isLiable = false;
                    organizerSplit.userName = organizer.getName();
                    organizerSplit.userRole = "ORGANIZER";
                    splitMap.put(organizerKey, organizerSplit);
                }
                
                BigDecimal organizerAmountCents = splitCalculator.calculateOrganizerAmount(deliveryAmountCents, config);
                organizerSplit.amount = organizerSplit.amount.add(organizerAmountCents);
                
                log.debug("      👔 Organizer {}: +R$ {} ({}% de R$ {})", 
                    organizer.getName(),
                    splitCalculator.toReais(organizerAmountCents, 2),
                    organizerPercentage,
                    deliveryAmount);
            } else {
                log.warn("      ⚠️ Sem organizer na delivery #{} - plataforma incorpora +{}%", 
                    delivery.getId(), organizerPercentage);
            }
            
            // Split para Plataforma (SE CONFIGURADO)
            // ATENÇÃO: Quando não há organizer, a plataforma recebe +5% (total 13%)
            if (platformRecipientId != null && !platformRecipientId.isBlank()) {
                String platformKey = "platform";
                SplitItem platformSplit = splitMap.get(platformKey);
                
                if (platformSplit == null) {
                    platformSplit = new SplitItem();
                    platformSplit.pagarmeRecipientId = platformRecipientId;
                    platformSplit.amount = BigDecimal.ZERO;
                    platformSplit.isLiable = true;
                    platformSplit.userName = "Plataforma MVT";
                    platformSplit.userRole = "PLATFORM";
                    splitMap.put(platformKey, platformSplit);
                }
                
                // Calcular valor da plataforma por DIFERENÇA (evita erro de arredondamento)
                BigDecimal courierAmountCents = splitCalculator.calculateCourierAmount(deliveryAmountCents, config);
                BigDecimal organizerAmountCents = hasOrganizer ? splitCalculator.calculateOrganizerAmount(deliveryAmountCents, config) : BigDecimal.ZERO;
                BigDecimal platformAmountCents = splitCalculator.calculatePlatformAmount(
                    deliveryAmountCents, courierAmountCents, organizerAmountCents);
                platformSplit.amount = platformSplit.amount.add(platformAmountCents);
                
                BigDecimal actualPercentage = splitCalculator.calculatePlatformPercentage(config, hasOrganizer);
                log.debug("      🏢 Plataforma: +R$ {} ({}% de R$ {})", 
                    splitCalculator.toReais(platformAmountCents, 2),
                    actualPercentage,
                    deliveryAmount);
            }
        }

        // Ajustar split para garantir que a soma seja exatamente 100% do valor total
        // O Pagar.me exige que a soma dos splits seja igual ao valor total da order
        BigDecimal totalDeliveriesAmount = deliveries.stream()
                .map(Delivery::getShippingFee)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Long totalCents = totalDeliveriesAmount.multiply(BigDecimal.valueOf(100)).longValue();
        
        BigDecimal totalSplitCents = splitMap.values().stream()
                .map(item -> item.amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        Long difference = totalCents - totalSplitCents.longValue();
        
        if (difference != 0) {
            log.warn("   ⚠️ Diferença detectada entre total e soma dos splits: {} centavos", difference);
            log.warn("      Total order: {} centavos | Soma splits: {} centavos", totalCents, totalSplitCents.longValue());
            
            // Ajustar o participant liable (plataforma) para incluir a diferença
            SplitItem liableToAdjust = splitMap.values().stream()
                    .filter(item -> item.isLiable)
                    .findFirst()
                    .orElse(null);
            
            if (liableToAdjust != null) {
                liableToAdjust.amount = liableToAdjust.amount.add(BigDecimal.valueOf(difference));
                log.info("      ✅ Ajustado split do {} {} em {} centavos", 
                    liableToAdjust.userRole, liableToAdjust.userName, difference);
            } else {
                log.error("      ❌ Não foi possível ajustar split - nenhum participant liable encontrado");
            }
        }

        // Log de resumo
        log.info("   ✅ Split calculado: {} recipients", splitMap.size());
        BigDecimal totalSplitFinal = BigDecimal.ZERO;
        for (Map.Entry<String, SplitItem> entry : splitMap.entrySet()) {
            SplitItem item = entry.getValue();
            BigDecimal totalReais = item.amount.divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            totalSplitFinal = totalSplitFinal.add(item.amount);
            log.info("      {} {} ({}): R$ {} ({} centavos)",
                item.userRole,
                item.userName,
                item.pagarmeRecipientId,
                totalReais,
                item.amount.longValue());
        }
        log.info("   📊 Total order: R$ {} ({} centavos) | Total split: {} centavos", 
            totalDeliveriesAmount, totalCents, totalSplitFinal.longValue());
        
        return splitMap;
    }

    /**
     * Constrói chave única para recipient (courier + organizer)
     */
    private String buildRecipientKey(Delivery delivery) {
        UUID courierId = delivery.getCourier() != null ? delivery.getCourier().getId() : UUID.fromString("00000000-0000-0000-0000-000000000000");
        UUID organizerId = delivery.getOrganizer() != null ? delivery.getOrganizer().getId() : UUID.fromString("00000000-0000-0000-0000-000000000000");
        return String.format("%s|%s", courierId, organizerId);
    }

    /**
     * Verifica se delivery já tem um payment PAID
     */
    private boolean hasActivePaidPayment(Delivery delivery) {
        return delivery.getPayments().stream()
                .anyMatch(p -> p.getStatus() == PaymentStatus.PAID);
    }

    /**
     * Constrói customer request para Pagar.me
     */
    private OrderRequest.CustomerRequest buildCustomerRequest(User client) {
        return OrderRequest.CustomerRequest.builder()
                .name(client.getName())
                .type("individual")
                .email(client.getUsername())
                .document(client.getDocumentClean())
                .address(buildAddressRequest(client.getAddress()))
                .phones(buildPhonesRequest(client))
                .build();
    }

    /**
     * Constrói address request para Pagar.me
     */
    private OrderRequest.AddressRequest buildAddressRequest(Address address) {
        if (address == null) {
            return null;
        }
        
        String street = address.getStreet() != null ? address.getStreet() : "";
        String number = address.getNumber() != null ? address.getNumber() : "";
        String zipCode = address.getZipCode() != null ? address.getZipCode() : "00000000";
        String city = address.getCity() != null ? address.getCity().getName() : "";
        String state = address.getCity() != null ? address.getCity().getStateCode() : "";
        
        return OrderRequest.AddressRequest.builder()
                .line1(street)
                .line2(number)
                .zipCode(zipCode)
                .city(city)
                .state(state)
                .country("BR")
                .build();
    }

    /**
     * Constrói phones request para Pagar.me
     * Pegar.me requer pelo menos um telefone (home_phone ou mobile_phone)
     */
    private OrderRequest.PhonesRequest buildPhonesRequest(User client) {
        // Verificar se o cliente tem DDD e número de telefone
        if (client.getPhoneDdd() == null || client.getPhoneNumber() == null) {
            log.warn("⚠️ Cliente {} não tem telefone cadastrado. Pagar.me requer pelo menos um telefone.", client.getName());
            return null;
        }
        
        // Criar mobile_phone (celular) com os dados do cliente
        OrderRequest.PhoneRequest mobilePhone = OrderRequest.PhoneRequest.builder()
                .countryCode("55") // Brasil
                .areaCode(client.getPhoneDdd()) // DDD (ex: "88")
                .number(client.getPhoneNumber()) // Número sem DDD (ex: "999999999")
                .build();
        
        log.debug("📱 Telefone criado para Pagar.me: +55 ({}) {}", client.getPhoneDdd(), client.getPhoneNumber());
        
        return OrderRequest.PhonesRequest.builder()
                .mobilePhone(mobilePhone)
                .build();
    }

    /**
     * Mapeia status do Pagar.me para PaymentStatus
     * 
     * Status Pagar.me:
     * - pending: Aguardando pagamento (PIX gerado mas não pago)
     * - paid: Pagamento confirmado
     * - failed: Pagamento falhou (dados inválidos, split rejeitado, etc)
     * - canceled: Pagamento cancelado
     * - processing: Processando pagamento
     * 
     * @param pagarmeStatus Status retornado pelo Pagar.me
     * @return PaymentStatus correspondente
     */
    private PaymentStatus mapPagarMeStatusToPaymentStatus(String pagarmeStatus) {
        if (pagarmeStatus == null) {
            log.warn("⚠️ Status do Pagar.me é NULL, usando PENDING como fallback");
            return PaymentStatus.PENDING;
        }
        
        return switch (pagarmeStatus.toLowerCase()) {
            case "pending" -> PaymentStatus.PENDING;
            case "paid" -> PaymentStatus.PAID;
            case "failed" -> PaymentStatus.FAILED;
            case "canceled", "cancelled" -> PaymentStatus.CANCELLED;
            case "processing" -> PaymentStatus.PROCESSING;
            case "refunded" -> PaymentStatus.REFUNDED;
            default -> {
                log.warn("⚠️ Status desconhecido do Pagar.me: '{}', usando PENDING como fallback", pagarmeStatus);
                yield PaymentStatus.PENDING;
            }
        };
    }

    /**
     * Classe interna para representar item de split
     */
    private static class SplitItem {
        String pagarmeRecipientId;
        BigDecimal percentage;
        BigDecimal amount;
        boolean isLiable;
        String description;
        String userName;
        String userRole;

        SplitItem() {
            // Construtor vazio para uso com setters diretos
        }

        SplitItem(String recipientId, BigDecimal pct, BigDecimal amt, boolean liable, String desc) {
            this.pagarmeRecipientId = recipientId;
            this.percentage = pct;
            this.amount = amt;
            this.isLiable = liable;
            this.description = desc;
        }
    }
}
