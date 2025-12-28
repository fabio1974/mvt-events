package com.mvt.mvt_events.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mvt.mvt_events.dto.PaymentRequest;
import com.mvt.mvt_events.dto.PaymentResponse;
import com.mvt.mvt_events.payment.dto.PaymentReportResponse;
import com.mvt.mvt_events.payment.service.PagarMeService;
import com.mvt.mvt_events.jpa.*;
import com.mvt.mvt_events.repository.DeliveryRepository;
import com.mvt.mvt_events.repository.PaymentRepository;
import com.mvt.mvt_events.repository.UserRepository;
import com.mvt.mvt_events.repository.SiteConfigurationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
        // TODO: Reimplementar usando Pagar.me API
        // Precisa ser reescrito para usar:
        // - pagarMeService.createOrder()
        // - SplitCalculator.calculatePagarmeSplit()
        // - Payment.setProviderPaymentId()
        
        throw new UnsupportedOperationException(
            "Payment creation temporarily disabled during Pagar.me migration. " +
            "Please implement createOrder with PagarMeService and SplitCalculator."
        );
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
            
            // Split do COURIER (87% padrão)
            if (delivery.getCourier() != null) {
                BigDecimal courierPercentage = splitCalculator.calculateCourierPercentage(config);
                BigDecimal courierAmount = splitCalculator.calculateCourierAmount(shippingFeeCents, config);
                
                PaymentReportResponse.SplitItem courierSplit = PaymentReportResponse.SplitItem.builder()
                        .recipientId(delivery.getCourier().getPagarmeRecipientId())
                        .recipientName(delivery.getCourier().getName())
                        .recipientRole("COURIER")
                        .amount(splitCalculator.toReais(courierAmount, 2))
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
                BigDecimal organizerAmount = splitCalculator.calculateOrganizerAmount(shippingFeeCents, config);
                
                PaymentReportResponse.SplitItem organizerSplit = PaymentReportResponse.SplitItem.builder()
                        .recipientId(organizer.getPagarmeRecipientId())
                        .recipientName(organizer.getName())
                        .recipientRole("ORGANIZER")
                        .amount(splitCalculator.toReais(organizerAmount, 2))
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
            // ATENÇÃO: Se não há organizer, plataforma recebe 8% + 5% = 13%
            BigDecimal platformPercentage = splitCalculator.calculatePlatformPercentage(config, hasOrganizer);
            BigDecimal platformAmount = splitCalculator.calculatePlatformAmount(shippingFeeCents, config, hasOrganizer);
            
            PaymentReportResponse.SplitItem platformSplit = PaymentReportResponse.SplitItem.builder()
                    .recipientId(config.getPagarmeRecipientId())
                    .recipientName("Plataforma Zapi10")
                    .recipientRole("PLATFORM")
                    .amount(splitCalculator.toReais(platformAmount, 2))
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
}
