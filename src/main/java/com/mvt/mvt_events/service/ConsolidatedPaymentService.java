package com.mvt.mvt_events.service;

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

            // 4. Agrupar deliveries por conjunto de problemas de pagamento
            // (Para consolidar: remover aquelas que já têm PENDING/COMPLETED)
            List<Delivery> deliverisWithPaymentProblems = deliveriesToPay.stream()
                    .filter(d -> {
                        Set<PaymentStatus> paymentStatuses = d.getPayments().stream()
                                .map(Payment::getStatus)
                                .collect(Collectors.toSet());
                        // Manter apenas se tem FAILED ou EXPIRED
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

        // 1. Calcular valor total
        BigDecimal totalAmount = deliveries.stream()
                .map(Delivery::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("   💹 Valor total: R$ {}", totalAmount);

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
        payment.setDeliveries(new HashSet<>(deliveries));
        payment.setProvider(com.mvt.mvt_events.jpa.PaymentProvider.PAGARME);
        payment.setNotes(String.format("Pagamento consolidado para %d deliveries", deliveries.size()));

        Payment savedPayment = paymentRepository.save(payment);
        log.info("   📊 Payment local salvo: ID {}", savedPayment.getId());

        // 5. Criar order no Pagar.me
        try {
            String orderId = createPagarMeOrder(client, deliveries, totalAmount, splitMap, savedPayment);
            savedPayment.setPagarmeOrderId(orderId);
            paymentRepository.save(savedPayment);
            log.info("   ✅ Order Pagar.me criada: {}", orderId);
        } catch (Exception e) {
            log.error("   ⚠️ Erro ao criar order Pagar.me (payment local salvo mesmo assim)", e);
            savedPayment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(savedPayment);
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

        // Items: apenas referência às deliveries
        List<OrderRequest.ItemRequest> items = new ArrayList<>();
        for (Delivery d : deliveries) {
            Long amountCents = (long) (d.getTotalAmount().doubleValue() * 100); // Em centavos
            items.add(OrderRequest.ItemRequest.builder()
                    .amount(amountCents)
                    .description(String.format("Entrega #%d (Cliente: %s)", d.getId(), client.getName()))
                    .quantity(1L)
                    .code("DEL-" + d.getId())
                    .build());
        }

        // PIX
        OrderRequest.PixRequest.PixRequestBuilder pixBuilder = OrderRequest.PixRequest.builder()
                .expiresIn("7200");  // 2 horas

        // Split: 87% courier, 5% manager, 8% platform
        List<OrderRequest.SplitRequest> splits = new ArrayList<>();
        for (SplitItem item : splitMap.values()) {
            Integer percentageInt = item.percentage != null ? item.percentage.intValue() : 0;
            OrderRequest.SplitRequest split = OrderRequest.SplitRequest.builder()
                    .amount(percentageInt)  // Percentual como integer
                    .recipientId(item.pagarmeRecipientId)
                    .type("percentage")
                    .options(OrderRequest.SplitOptionsRequest.builder()
                            .chargeProcessingFee(item.isLiable)
                            .chargeRemainderFee(item.isLiable)
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

        // Criar order
        String orderId = pagarMeService.createOrder(orderRequest);

        return orderId;
    }

    /**
     * Calcula split de valores por recipient (courier + organizer)
     * 
     * Agrupa deliveries por (courier + organizer) e soma os fretes
     * Aplica percentuais da SiteConfiguration
     * 
     * @param deliveries Deliveries a consolidar
     * @param config Configuração ativa
     * @return Map<recipientKey, SplitItem>
     */
    private Map<String, SplitItem> calculateSplitByRecipient(List<Delivery> deliveries, 
                                                             SiteConfiguration config) {
        log.info("🧮 Calculando split de {} deliveries", deliveries.size());
        
        Map<String, SplitItem> splitMap = new HashMap<>();
        
        // Agrupar por (courier + organizer)
        Map<String, List<Delivery>> groupedByRecipient = new HashMap<>();
        for (Delivery d : deliveries) {
            String key = buildRecipientKey(d);
            groupedByRecipient.computeIfAbsent(key, k -> new ArrayList<>()).add(d);
        }

        log.info("   🔀 {} grupos de recipients encontrados", groupedByRecipient.size());

        // Calcular valores para cada grupo
        for (Map.Entry<String, List<Delivery>> entry : groupedByRecipient.entrySet()) {
            String recipientKey = entry.getKey();
            List<Delivery> recipientDeliveries = entry.getValue();

            // Soma fretes
            BigDecimal subtotal = recipientDeliveries.stream()
                    .map(Delivery::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            log.debug("   └─ Recipient {} com subtotal R$ {}", recipientKey, subtotal);

            // TODO: Split seguindo percentuais de SiteConfiguration
            // Exemplo: 87% courier, 5% manager, 8% platform
            // Por enquanto, colocar placeholder
        }

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
                .anyMatch(p -> p.getStatus() == PaymentStatus.COMPLETED);
    }

    /**
     * Constrói customer request para Pagar.me
     */
    private OrderRequest.CustomerRequest buildCustomerRequest(User client) {
        return OrderRequest.CustomerRequest.builder()
                .name(client.getName())
                .type("individual")
                .email(client.getUsername())
                .document(client.getCpfClean())
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
     */
    private OrderRequest.PhonesRequest buildPhonesRequest(User client) {
        // TODO: Implementar conforme estrutura de phones do User
        // Por enquanto, retornar vazio
        return null;
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

        SplitItem(String recipientId, BigDecimal pct, BigDecimal amt, boolean liable, String desc) {
            this.pagarmeRecipientId = recipientId;
            this.percentage = pct;
            this.amount = amt;
            this.isLiable = liable;
            this.description = desc;
        }
    }
}
