package com.mvt.mvt_events.service;

import com.mvt.mvt_events.dto.PaymentRequest;
import com.mvt.mvt_events.dto.PaymentResponse;
import com.mvt.mvt_events.payment.dto.*;
import com.mvt.mvt_events.payment.service.IuguService;
import com.mvt.mvt_events.jpa.*;
import com.mvt.mvt_events.repository.DeliveryRepository;
import com.mvt.mvt_events.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Serviço responsável pela criação e gestão de pagamentos PIX via Iugu
 * 
 * Funcionalidades principais:
 * 1. Criar fatura PIX com split automático (87% motoboy, 5% gestor, 8% plataforma)
 * 2. Validar dados antes de enviar ao Iugu
 * 3. Salvar informações localmente
 * 4. Processar webhooks de confirmação de pagamento
 * 
 * IMPORTANTE: Agora suporta MÚLTIPLAS DELIVERIES em um único pagamento!
 * Isso permite que o cliente pague várias entregas com um único QR Code PIX,
 * economizando taxas (R$ 0,59 vs múltiplos R$ 0,59) e melhorando a UX.
 * 
 * @see IuguService
 * @see PaymentRequest
 * @see PaymentResponse
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final IuguService iuguService;
    private final PaymentRepository paymentRepository;
    private final DeliveryRepository deliveryRepository;

    /**
     * Cria uma fatura PIX com split automático para MÚLTIPLAS DELIVERIES.
     * 
     * Fluxo:
     * 1. Valida request (deliveryIds, amount, etc)
     * 2. Busca todas as deliveries por IDs
     * 3. Valida TODAS as deliveries:
     *    - Status COMPLETED
     *    - Não têm payment PAID
     *    - Pertencem ao mesmo cliente (payer)
     * 4. Calcula split 87/5/8
     * 5. Cria fatura no Iugu com PIX
     * 6. Salva Payment local associado a TODAS as deliveries (N:M)
     * 
     * Split de valores:
     * - 87% para o motoboy (courier)
     * - 5% para o gestor da organização
     * - 8% para a plataforma
     * 
     * @param request Dados do pagamento (deliveryIds, amount, emails, contas Iugu)
     * @return PaymentResponse com QR Code PIX e dados da fatura
     * @throws IllegalArgumentException Se dados inválidos
     * @throws IllegalStateException Se entregas não encontradas ou inválidas
     */
    @Transactional
    public PaymentResponse createInvoiceWithSplit(PaymentRequest request) {
        log.info("📥 Criando fatura PIX com split - {} deliveries, Amount: R$ {}", 
                request.getDeliveryIds().size(), request.getAmount());

        // 1. Validar request
        request.validate();

        // 2. Buscar TODAS as deliveries
        List<Delivery> deliveries = deliveryRepository.findAllById(request.getDeliveryIds());
        
        if (deliveries.size() != request.getDeliveryIds().size()) {
            throw new IllegalArgumentException(
                    String.format("Algumas entregas não foram encontradas. Esperado: %d, Encontrado: %d",
                            request.getDeliveryIds().size(), deliveries.size()));
        }

        log.info("✅ {} deliveries encontradas: {}", deliveries.size(), 
                deliveries.stream().map(d -> d.getId().toString()).collect(Collectors.joining(", ")));

        // 3. Validar TODAS as deliveries (inclui verificação de pagamentos duplicados)
        validateDeliveriesForPayment(deliveries);

        // 4. Pegar o cliente (payer) da primeira delivery (todas devem ter o mesmo)
        Delivery firstDelivery = deliveries.get(0);
        User payer = firstDelivery.getClient();

        // 5. Calcular split
        BigDecimal motoboyAmount = request.getAmount().multiply(new BigDecimal("0.87"));
        BigDecimal managerAmount = request.getAmount().multiply(new BigDecimal("0.05"));
        BigDecimal platformAmount = request.getAmount().multiply(new BigDecimal("0.08"));

        log.info("💰 Split calculado - Motoboy: R$ {}, Gestor: R$ {}, Plataforma: R$ {}",
                motoboyAmount, managerAmount, platformAmount);

        // 6. Montar InvoiceRequest para Iugu
        InvoiceRequest invoiceRequest = new InvoiceRequest();
        invoiceRequest.setEmail(request.getClientEmail());
        invoiceRequest.setDueDate(LocalDateTime.now().plusHours(request.getExpirationHours()));
        invoiceRequest.setPayableWith("pix"); // Apenas PIX
        invoiceRequest.setEnsureWorkdayDueDate(false);

        // Item da fatura
        InvoiceItemRequest item = new InvoiceItemRequest();
        item.setDescription(request.getDescriptionOrDefault());
        item.setQuantity(1);
        item.setPriceCents((int) (request.getAmount().doubleValue() * 100));
        invoiceRequest.setItems(List.of(item));

        // Split rules
        List<SplitRequest> splits = new ArrayList<>();

        // Split do motoboy (87%)
        SplitRequest motoboyCharge = new SplitRequest();
        motoboyCharge.setReceiverId(request.getMotoboyAccountId());
        motoboyCharge.setCents((int) (motoboyAmount.doubleValue() * 100));
        motoboyCharge.setPercent(null); // Usa cents fixo
        splits.add(motoboyCharge);

        // Split do gestor (5%) - se informado
        if (request.getManagerAccountId() != null && !request.getManagerAccountId().isBlank()) {
            SplitRequest managerCharge = new SplitRequest();
            managerCharge.setReceiverId(request.getManagerAccountId());
            managerCharge.setCents((int) (managerAmount.doubleValue() * 100));
            managerCharge.setPercent(null);
            splits.add(managerCharge);
            log.info("👔 Gestor incluído no split: {}", request.getManagerAccountId());
        } else {
            log.info("⚠️ Gestor não informado - 5% irá para a plataforma");
        }

        // Os 8% da plataforma ficam na conta principal (não precisa adicionar no split)

        invoiceRequest.setSplits(splits);

        // 7. Criar fatura no Iugu
        log.info("🚀 Enviando fatura para Iugu...");
        InvoiceResponse iuguResponse;
        try {
            iuguResponse = iuguService.createInvoice(invoiceRequest);
            log.info("✅ Fatura criada no Iugu: {}", iuguResponse.id());
        } catch (Exception e) {
            log.error("❌ Erro ao criar fatura no Iugu", e);
            throw new RuntimeException("Erro ao criar fatura PIX: " + e.getMessage(), e);
        }

        // 8. Criar Payment local e associar com TODAS as deliveries (N:M)
        Payment payment = new Payment();
        
        // Adicionar TODAS as deliveries ao payment (relacionamento N:M)
        for (Delivery delivery : deliveries) {
            payment.addDelivery(delivery);
        }
        
        payment.setPayer(payer); // Cliente que solicitou as entregas
        payment.setAmount(request.getAmount());
        payment.setCurrency(com.mvt.mvt_events.jpa.Currency.BRL);
        payment.setPaymentMethod(PaymentMethod.PIX);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setProvider(com.mvt.mvt_events.jpa.PaymentProvider.IUGU);
        payment.setIuguInvoiceId(iuguResponse.id());
        payment.setPixQrCode(iuguResponse.pixQrCode());
        payment.setPixQrCodeUrl(iuguResponse.pixQrCodeUrl());
        payment.setExpiresAt(LocalDateTime.parse(iuguResponse.dueDate()));

        // Salvar split rules como JSON
        String splitRulesJson = String.format(
                "{\"motoboy\": {\"accountId\": \"%s\", \"amount\": %.2f, \"percent\": 87}, " +
                "\"manager\": {\"accountId\": \"%s\", \"amount\": %.2f, \"percent\": 5}, " +
                "\"platform\": {\"amount\": %.2f, \"percent\": 8}, " +
                "\"deliveries\": [%s]}",
                request.getMotoboyAccountId(),
                motoboyAmount.doubleValue(),
                request.getManagerAccountId() != null ? request.getManagerAccountId() : "N/A",
                managerAmount.doubleValue(),
                platformAmount.doubleValue(),
                deliveries.stream()
                    .map(d -> String.format("\"%s\"", d.getId().toString()))
                    .collect(Collectors.joining(", "))
        );
        payment.setSplitRules(splitRulesJson);

        Payment savedPayment = paymentRepository.save(payment);
        log.info("💾 Payment salvo localmente: ID {} com {} deliveries", 
                savedPayment.getId(), savedPayment.getDeliveriesCount());

        // 9. Retornar resposta
        PaymentResponse response = PaymentResponse.from(savedPayment, iuguResponse.secureUrl());
        
        log.info("📤 Fatura PIX criada com sucesso!");
        log.info("   ├─ Payment ID: {}", savedPayment.getId());
        log.info("   ├─ Iugu Invoice ID: {}", iuguResponse.id());
        log.info("   ├─ Amount: R$ {}", request.getAmount());
        log.info("   ├─ Deliveries: {}", savedPayment.getDeliveriesCount());
        log.info("   └─ Expires: {}", savedPayment.getExpiresAt());

        return response;
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
                String status = existingPayment.getStatus() == PaymentStatus.COMPLETED ? "PAGO" : "PENDENTE";
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
     * Processa confirmação de pagamento via webhook Iugu.
     * 
     * Este método é chamado quando o Iugu envia um webhook confirmando
     * que o pagamento foi realizado. Atualiza o status do Payment para COMPLETED.
     * 
     * @param invoiceId ID da fatura Iugu que foi paga
     */
    @Transactional
    public void processPaymentConfirmation(String invoiceId) {
        log.info("🔔 Processando confirmação de pagamento - Invoice: {}", invoiceId);

        Payment payment = paymentRepository.findByIuguInvoiceId(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Payment não encontrado para invoice: " + invoiceId));

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            log.warn("⚠️ Payment já estava COMPLETED: {}", payment.getId());
            return;
        }

        payment.markAsCompleted();
        paymentRepository.save(payment);

        log.info("✅ Payment {} marcado como COMPLETED ({} deliveries pagas)", 
                payment.getId(), payment.getDeliveriesCount());
    }
}
