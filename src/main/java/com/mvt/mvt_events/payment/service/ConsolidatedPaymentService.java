package com.mvt.mvt_events.payment.service;

import com.mvt.mvt_events.dto.ConsolidatedInvoiceResponse;
import com.mvt.mvt_events.dto.RecipientSplit;
import com.mvt.mvt_events.jpa.Delivery;
import com.mvt.mvt_events.jpa.Payment;
import com.mvt.mvt_events.jpa.PaymentStatus;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.payment.dto.InvoiceResponse;
import com.mvt.mvt_events.repository.DeliveryRepository;
import com.mvt.mvt_events.repository.PaymentRepository;
import com.mvt.mvt_events.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Serviço de pagamentos consolidados
 * 
 * <p>Orquestra a criação de invoices consolidadas no Iugu,
 * calculando splits para múltiplas deliveries com diferentes
 * motoboys e gerentes.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsolidatedPaymentService {

    private final DeliveryRepository deliveryRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final SplitCalculator splitCalculator;
    private final IuguService iuguService;
    private final EntityManager entityManager;

    /**
     * Cria uma invoice consolidada para múltiplas deliveries
     * 
     * @param deliveryIds IDs das deliveries a pagar
     * @param clientEmail Email do cliente que irá pagar
     * @param clientName Nome do cliente (opcional, usado para criar novo usuário)
     * @param expirationHours Horas até expirar (padrão: 24h)
     * @return Response com QR Code PIX e detalhes dos splits
     */
    @Transactional
    public ConsolidatedInvoiceResponse createConsolidatedInvoice(
            List<Long> deliveryIds,
            String clientEmail,
            String clientName,
            Integer expirationHours
    ) {
        log.info("🎯 Criando invoice consolidada para {} deliveries", deliveryIds.size());

        // 1. Buscar ou criar payer (cliente)
        User payer = findOrCreatePayer(clientEmail, clientName);
        
        log.info("💳 Pagador: {} (ID: {}, Email: {})", 
                payer.getName(), 
                payer.getId(), 
                payer.getUsername());

        // 2. Buscar deliveries - sessão Hibernate ficará aberta devido ao @Transactional
        // Usamos findById simples e deixamos o lazy loading funcionar naturalmente
        List<Delivery> deliveries = new ArrayList<>();
        for (Long id : deliveryIds) {
            deliveryRepository.findById(id).ifPresent(deliveries::add);
        }
        
        if (deliveries.size() != deliveryIds.size()) {
            Set<Long> found = deliveries.stream().map(Delivery::getId).collect(Collectors.toSet());
            Set<Long> notFound = deliveryIds.stream()
                    .filter(id -> !found.contains(id))
                    .collect(Collectors.toSet());
            throw new IllegalArgumentException("Deliveries não encontradas: " + notFound);
        }

        // Log detalhado das deliveries encontradas
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("📦 DELIVERIES ENCONTRADAS: {}", deliveries.size());
        log.info("═══════════════════════════════════════════════════════════════");
        
        BigDecimal totalShippingFee = BigDecimal.ZERO;
        for (Delivery delivery : deliveries) {
            // Validação: shippingFee não pode ser nulo
            if (delivery.getShippingFee() == null || delivery.getShippingFee().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                    String.format("Delivery #%d não tem valor de frete (shippingFee) configurado", delivery.getId())
                );
            }
            
            log.info("📦 Delivery #{}",  delivery.getId());
            log.info("   � Valor do Frete: R$ {}", delivery.getShippingFee());
            log.info("   🛒 Valor do Pedido: R$ {} (não entra no split)", delivery.getTotalAmount());
            log.info("   👨‍🚀 Motoboy: {} (Iugu: {})", 
                    delivery.getCourier() != null ? delivery.getCourier().getName() : "N/A",
                    delivery.getCourier() != null ? delivery.getCourier().getIuguAccountId() : "N/A");
            log.info("   👔 Gerente: {} (Iugu: {})", 
                    delivery.getOrganizer() != null ? delivery.getOrganizer().getName() : "N/A",
                    delivery.getOrganizer() != null ? delivery.getOrganizer().getIuguAccountId() : "N/A");
            log.info("   📍 Status: {}", delivery.getStatus());
            log.info("───────────────────────────────────────────────────────────────");
            
            totalShippingFee = totalShippingFee.add(delivery.getShippingFee());
        }
        
        log.info("💰 VALOR TOTAL DOS FRETES (para split): R$ {}", totalShippingFee);
        log.info("═══════════════════════════════════════════════════════════════");

        // 2. Calcular splits consolidados
        List<RecipientSplit> splits = splitCalculator.calculateSplits(deliveries);

        // 3. Calcular valor total em centavos
        int totalCents = splits.stream()
                .mapToInt(RecipientSplit::getAmountCents)
                .sum();

        BigDecimal totalAmount = BigDecimal.valueOf(totalCents)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        // 4. Criar descrição
        String description = String.format(
                "Pagamento de %d entrega%s",
                deliveries.size(),
                deliveries.size() > 1 ? "s" : ""
        );

        // 5. Criar invoice no Iugu
        InvoiceResponse iuguInvoice = iuguService.createInvoiceWithConsolidatedSplits(
                clientEmail,
                totalCents,
                description,
                expirationHours != null ? expirationHours : 24,
                splits
        );

        // 6. Salvar Payment no banco
        Payment payment = new Payment();
        payment.setIuguInvoiceId(iuguInvoice.id());
        payment.setAmount(totalAmount);
        payment.setCurrency(com.mvt.mvt_events.jpa.Currency.BRL);
        payment.setProvider(com.mvt.mvt_events.jpa.PaymentProvider.IUGU);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setPixQrCode(iuguInvoice.pixQrCode());
        payment.setPixQrCodeUrl(iuguInvoice.pixQrCodeUrl());
        payment.setExpiresAt(parseIuguDate(iuguInvoice.dueDate()));
        payment.setPayer(payer); // Associar pagador
        
        // Salvar Payment PRIMEIRO (sem deliveries)
        Payment savedPayment = paymentRepository.save(payment);
        
        // Fazer flush para garantir que foi persistido
        entityManager.flush();
        
        // Desassociar as deliveries do contexto de persistência
        // Isso evita o ConcurrentModificationException ao adicionar a associação
        for (Delivery delivery : deliveries) {
            entityManager.detach(delivery);
        }
        
        // Agora podemos adicionar as deliveries sem conflito
        // Buscar novamente as deliveries como managed entities (sem inicializar coleções lazy)
        List<Delivery> managedDeliveries = new ArrayList<>();
        for (Long deliveryId : deliveryIds) {
            Delivery managedDelivery = entityManager.getReference(Delivery.class, deliveryId);
            managedDeliveries.add(managedDelivery);
        }
        
        // Associar deliveries ao payment
        savedPayment.getDeliveries().addAll(managedDeliveries);
        
        // Salvar novamente com as associações
        savedPayment = paymentRepository.saveAndFlush(savedPayment);

        log.info("✅ Invoice consolidada criada: Payment #{} → Iugu Invoice {}", 
                savedPayment.getId(), 
                iuguInvoice.id());

        // 7. Montar response com detalhes dos splits
        return buildResponse(savedPayment, iuguInvoice, deliveries, splits);
    }

    /**
     * Parse de data do Iugu (pode vir como String ou LocalDateTime)
     */
    private LocalDateTime parseIuguDate(String dueDateStr) {
        if (dueDateStr == null || dueDateStr.isBlank()) {
            return LocalDateTime.now().plusDays(1);
        }
        try {
            return LocalDateTime.parse(dueDateStr);
        } catch (Exception e) {
            log.warn("Erro ao parsear data do Iugu: {}", dueDateStr);
            return LocalDateTime.now().plusDays(1);
        }
    }

    /**
     * Monta o response consolidado com todos os detalhes
     */
    private ConsolidatedInvoiceResponse buildResponse(
            Payment payment,
            InvoiceResponse iuguInvoice,
            List<Delivery> deliveries,
            List<RecipientSplit> splits
    ) {
        // Agrupar splits por tipo
        Map<RecipientSplit.RecipientType, List<RecipientSplit>> splitsByType = splits.stream()
                .collect(Collectors.groupingBy(RecipientSplit::getType));

        // Contar únicos
        int couriersCount = (int) splitsByType.getOrDefault(RecipientSplit.RecipientType.COURIER, Collections.emptyList())
                .stream()
                .map(RecipientSplit::getIuguAccountId)
                .distinct()
                .count();

        int managersCount = (int) splitsByType.getOrDefault(RecipientSplit.RecipientType.MANAGER, Collections.emptyList())
                .stream()
                .map(RecipientSplit::getIuguAccountId)
                .distinct()
                .count();

        // Somar valores por tipo
        BigDecimal couriersAmount = sumAmounts(splitsByType.get(RecipientSplit.RecipientType.COURIER));
        BigDecimal managersAmount = sumAmounts(splitsByType.get(RecipientSplit.RecipientType.MANAGER));
        BigDecimal platformAmount = sumAmounts(splitsByType.get(RecipientSplit.RecipientType.PLATFORM));

        // Mapa de recipients individuais (para debug/transparência)
        Map<String, BigDecimal> recipients = buildRecipientsMap(deliveries, splits);

        // Montar detalhes dos splits
        ConsolidatedInvoiceResponse.SplitDetails splitDetails = new ConsolidatedInvoiceResponse.SplitDetails(
                couriersCount,
                managersCount,
                couriersAmount,
                managersAmount,
                platformAmount,
                recipients
        );

        // Status message
        String statusMessage = "⏳ Aguardando pagamento. Escaneie o QR Code PIX ou copie o código.";
        boolean expired = payment.getExpiresAt() != null && 
                         payment.getExpiresAt().isBefore(LocalDateTime.now());

        return new ConsolidatedInvoiceResponse(
                payment.getId(),
                payment.getIuguInvoiceId(),
                payment.getPixQrCode(),           // ← QR Code string (copiar/colar)
                payment.getPixQrCodeUrl(),        // ← URL da imagem do QR Code
                iuguInvoice.secureUrl(),          // ← URL para abrir no navegador (do Iugu response)
                payment.getAmount(),
                deliveries.size(),
                splitDetails,
                payment.getStatus().name(),
                payment.getExpiresAt(),
                statusMessage,
                expired
        );
    }

    /**
     * Soma valores de uma lista de splits
     */
    private BigDecimal sumAmounts(List<RecipientSplit> splits) {
        if (splits == null || splits.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return splits.stream()
                .map(RecipientSplit::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Constrói mapa de recipients individuais para transparência
     */
    private Map<String, BigDecimal> buildRecipientsMap(
            List<Delivery> deliveries,
            List<RecipientSplit> splits
    ) {
        Map<String, BigDecimal> recipients = new LinkedHashMap<>();

        // Criar mapa de accountId → User
        Map<String, User> accountToUser = new HashMap<>();
        for (Delivery delivery : deliveries) {
            if (delivery.getCourier() != null && delivery.getCourier().getIuguAccountId() != null) {
                accountToUser.put(delivery.getCourier().getIuguAccountId(), delivery.getCourier());
            }
            if (delivery.getOrganizer() != null && delivery.getOrganizer().getIuguAccountId() != null) {
                accountToUser.put(delivery.getOrganizer().getIuguAccountId(), delivery.getOrganizer());
            }
        }

        // Montar mapa com nomes legíveis
        for (RecipientSplit split : splits) {
            String key;
            if (split.getType() == RecipientSplit.RecipientType.PLATFORM) {
                key = "Plataforma";
            } else {
                User user = accountToUser.get(split.getIuguAccountId());
                String userName = user != null ? user.getName() : "Usuário " + split.getIuguAccountId();
                key = split.getType().name() + " - " + userName;
            }
            recipients.put(key, split.getAmount());
        }

        return recipients;
    }

    /**
     * Busca um usuário pelo email.
     * 
     * <p>Se o cliente não existir, lança exceção informando que o cliente
     * deve ser cadastrado primeiro.</p>
     * 
     * @param email Email do cliente
     * @param name Nome do cliente (não usado, mantido por compatibilidade)
     * @return User existente
     * @throws IllegalArgumentException se cliente não for encontrado
     */
    private User findOrCreatePayer(String email, String name) {
        log.info("🔍 Buscando cliente por email: {}", email);
        
        return userRepository.findByUsername(email)
                .orElseThrow(() -> {
                    log.error("❌ Cliente não encontrado: {}", email);
                    return new IllegalArgumentException(
                        String.format("Cliente com email '%s' não encontrado. " +
                                    "Por favor, cadastre o cliente primeiro antes de criar o pagamento.", 
                                    email)
                    );
                });
    }
}
