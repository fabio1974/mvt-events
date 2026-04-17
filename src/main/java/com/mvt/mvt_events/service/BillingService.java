package com.mvt.mvt_events.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mvt.mvt_events.jpa.*;
import com.mvt.mvt_events.payment.dto.OrderRequest;
import com.mvt.mvt_events.payment.dto.OrderResponse;
import com.mvt.mvt_events.payment.service.PagarMeService;
import com.mvt.mvt_events.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Serviço de billing para pagamentos recorrentes de serviços da plataforma.
 *
 * Gera faturas mensais (Payment + PIX) para subscriptions ativas.
 * Suporta pro-rata na primeira e última parcela.
 *
 * O valor total vai 100% para a plataforma (sem split com courier/organizer).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BillingService {

    private static final ZoneId TZ = ZoneId.of("America/Fortaleza");
    private static final int PIX_EXPIRES_SECONDS = 5 * 24 * 3600; // 5 dias

    private final PaymentRepository paymentRepository;
    private final ClientSubscriptionRepository subscriptionRepository;
    private final PlatformServiceRepository platformServiceRepository;
    private final SiteConfigurationRepository siteConfigurationRepository;
    private final PagarMeService pagarMeService;
    private final ObjectMapper objectMapper;

    // ============================================================================
    // ATIVAÇÃO DE SUBSCRIPTION
    // ============================================================================

    /**
     * Cria subscription e gera primeira fatura pro-rata.
     * Chamado automaticamente quando um serviço é ativado (ex: tableOrdersEnabled = true).
     */
    @Transactional
    public ClientSubscription activateSubscription(User client, String serviceCode, Integer billingDueDay) {
        PlatformService service = platformServiceRepository.findByCode(serviceCode)
                .orElseThrow(() -> new IllegalArgumentException("Serviço não encontrado: " + serviceCode));

        // Verificar se já existe subscription ativa
        var existing = subscriptionRepository.findByClientIdAndServiceCodeAndActiveTrue(client.getId(), serviceCode);
        if (existing.isPresent()) {
            log.info("📋 Subscription já ativa para client {} / serviço {}", client.getId(), serviceCode);
            return existing.get();
        }

        int dueDay = billingDueDay != null ? billingDueDay : 10; // default dia 10

        ClientSubscription subscription = ClientSubscription.builder()
                .client(client)
                .service(service)
                .monthlyPrice(service.getDefaultMonthlyPrice())
                .billingDueDay(dueDay)
                .startedAt(OffsetDateTime.now(TZ))
                .active(true)
                .build();

        subscription = subscriptionRepository.save(subscription);
        log.info("✅ Subscription criada: client={}, service={}, price={}, dueDay={}",
                client.getId(), serviceCode, subscription.getMonthlyPrice(), dueDay);

        // Gerar primeira fatura pro-rata
        generateFirstInvoice(subscription);

        return subscription;
    }

    // ============================================================================
    // DESATIVAÇÃO DE SUBSCRIPTION
    // ============================================================================

    /**
     * Cancela subscription, cancela fatura futura pendente e gera pro-rata final.
     */
    @Transactional
    public void deactivateSubscription(UUID clientId, String serviceCode) {
        ClientSubscription subscription = subscriptionRepository
                .findByClientIdAndServiceCodeAndActiveTrue(clientId, serviceCode)
                .orElse(null);

        if (subscription == null) {
            log.info("📋 Nenhuma subscription ativa para cancelar: client={}, service={}", clientId, serviceCode);
            return;
        }

        LocalDate today = LocalDate.now(TZ);

        // 1. Cancelar faturas PENDING futuras
        List<Payment> pendingPayments = paymentRepository.findBySubscriptionAndStatus(
                subscription, PaymentStatus.PENDING);

        for (Payment pending : pendingPayments) {
            if (pending.getDueDate() != null && pending.getDueDate().isAfter(today)) {
                pending.markAsCancelled();
                paymentRepository.save(pending);
                log.info("🚫 Fatura futura cancelada: payment={}, dueDate={}", pending.getId(), pending.getDueDate());
            }
        }

        // 2. Gerar última fatura pro-rata
        generateFinalInvoice(subscription);

        // 3. Cancelar subscription
        subscription.cancel();
        subscriptionRepository.save(subscription);
        log.info("❌ Subscription cancelada: id={}, client={}", subscription.getId(), clientId);
    }

    // ============================================================================
    // GERAÇÃO DE FATURAS
    // ============================================================================

    /**
     * Gera primeira fatura pro-rata: da ativação até o próximo vencimento.
     */
    private void generateFirstInvoice(ClientSubscription subscription) {
        LocalDate startDate = subscription.getStartedAt().atZoneSameInstant(TZ).toLocalDate();
        LocalDate nextDueDate = subscription.getNextDueDate(startDate);

        BigDecimal amount = subscription.calculateProrata(startDate, nextDueDate);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return;

        String refMonth = nextDueDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));

        createBillingPayment(subscription, startDate, nextDueDate, nextDueDate, refMonth, amount, true);
        log.info("📄 1ª fatura pro-rata: {} → {}, valor=R$ {}", startDate, nextDueDate, amount);
    }

    /**
     * Gera fatura mensal cheia (chamado pelo scheduler).
     */
    @Transactional
    public void generateMonthlyInvoice(ClientSubscription subscription) {
        LocalDate dueDate = LocalDate.now(TZ)
                .withDayOfMonth(Math.min(subscription.getBillingDueDay(),
                        LocalDate.now(TZ).lengthOfMonth()));

        // Período: do vencimento anterior até este vencimento
        LocalDate periodStart = dueDate.minusMonths(1);
        String refMonth = dueDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));

        // Verificar se já existe fatura para este mês
        if (subscriptionRepository.existsInvoiceForMonth(subscription.getId(), refMonth)) {
            log.info("📋 Fatura já existe para subscription={}, mês={}", subscription.getId(), refMonth);
            return;
        }

        BigDecimal amount = subscription.getMonthlyPrice();

        createBillingPayment(subscription, periodStart, dueDate, dueDate, refMonth, amount, false);
        log.info("📄 Fatura mensal: {} → {}, valor=R$ {}", periodStart, dueDate, amount);
    }

    /**
     * Gera última fatura pro-rata: do último vencimento até hoje.
     */
    private void generateFinalInvoice(ClientSubscription subscription) {
        LocalDate today = LocalDate.now(TZ);
        LocalDate lastDueDate = subscription.getNextDueDate(today.minusMonths(1));

        // Se o último vencimento é no futuro, recuar mais
        if (lastDueDate.isAfter(today)) {
            lastDueDate = lastDueDate.minusMonths(1);
        }

        BigDecimal amount = subscription.calculateProrata(lastDueDate, today);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return;

        String refMonth = today.format(DateTimeFormatter.ofPattern("yyyy-MM"));

        createBillingPayment(subscription, lastDueDate, today, today, refMonth, amount, true);
        log.info("📄 Fatura final pro-rata: {} → {}, valor=R$ {}", lastDueDate, today, amount);
    }

    // ============================================================================
    // CRIAÇÃO DO PAYMENT + PIX
    // ============================================================================

    /**
     * Cria o Payment local e gera PIX no Pagar.me.
     */
    private void createBillingPayment(
            ClientSubscription subscription,
            LocalDate periodStart,
            LocalDate periodEnd,
            LocalDate dueDate,
            String referenceMonth,
            BigDecimal amount,
            boolean prorata
    ) {
        User client = subscription.getClient();
        PlatformService service = subscription.getService();

        // Construir request para Pagar.me
        OrderRequest orderRequest = buildBillingPixRequest(client, service, amount, referenceMonth);

        try {
            // Chamar Pagar.me
            OrderResponse orderResponse = pagarMeService.createOrderWithFullResponse(orderRequest);
            log.info("✅ PIX billing criado no Pagar.me - Order ID: {}", orderResponse.getId());

            // Criar Payment
            Payment payment = new Payment();
            payment.setPaymentType(service.getCode());
            payment.setSubscription(subscription);
            payment.setBillingPeriodStart(periodStart);
            payment.setBillingPeriodEnd(periodEnd);
            payment.setDueDate(dueDate);
            payment.setReferenceMonth(referenceMonth);
            payment.setProrata(prorata);

            payment.setProviderPaymentId(orderResponse.getId());
            payment.setAmount(amount);
            payment.setCurrency(Currency.BRL);
            payment.setPaymentMethod(PaymentMethod.PIX);
            payment.setProvider(PaymentProvider.PAGARME);
            payment.setPayer(client);
            payment.setStatus(PaymentStatus.PENDING);
            payment.setNotes(service.getName() + " — " + referenceMonth + (prorata ? " (pro-rata)" : ""));

            // Extrair QR Code
            extractPixData(payment, orderResponse);

            // Salvar request/response para auditoria
            try {
                payment.setRequest(objectMapper.writeValueAsString(orderRequest));
                payment.setResponse(objectMapper.writeValueAsString(orderResponse));
            } catch (Exception e) {
                log.warn("⚠️ Erro ao serializar request/response: {}", e.getMessage());
            }

            paymentRepository.save(payment);
            log.info("💾 Payment billing salvo: id={}, amount=R$ {}, type={}, ref={}",
                    payment.getId(), amount, service.getCode(), referenceMonth);

        } catch (Exception e) {
            log.error("❌ Erro ao gerar PIX billing para subscription {}: {}", subscription.getId(), e.getMessage(), e);
        }
    }

    /**
     * Constrói OrderRequest PIX para billing (100% plataforma, sem split).
     */
    private OrderRequest buildBillingPixRequest(User client, PlatformService service, BigDecimal amount, String referenceMonth) {
        long amountCents = amount.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).longValue();

        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setClosed(true);

        // Item
        OrderRequest.ItemRequest item = new OrderRequest.ItemRequest();
        item.setCode("SVC-" + service.getCode());
        item.setDescription(service.getName() + " — " + referenceMonth);
        item.setAmount(amountCents);
        item.setQuantity(1L);
        orderRequest.setItems(List.of(item));

        // Customer
        OrderRequest.CustomerRequest customer = new OrderRequest.CustomerRequest();
        customer.setName(client.getName());
        customer.setEmail(client.getUsername());
        customer.setType("individual");
        if (client.getDocumentNumber() != null) {
            customer.setDocument(client.getDocumentNumber().replaceAll("[^0-9]", ""));
        }
        orderRequest.setCustomer(customer);

        // Pagamento PIX
        OrderRequest.PaymentRequest paymentReq = new OrderRequest.PaymentRequest();
        paymentReq.setPaymentMethod("pix");
        paymentReq.setAmount(amountCents);

        OrderRequest.PixRequest pix = new OrderRequest.PixRequest();
        pix.setExpiresIn(String.valueOf(PIX_EXPIRES_SECONDS));

        List<OrderRequest.AdditionalInfoRequest> additionalInfo = new ArrayList<>();
        OrderRequest.AdditionalInfoRequest info = new OrderRequest.AdditionalInfoRequest();
        info.setName("Serviço");
        info.setValue(service.getName() + " — Ref: " + referenceMonth);
        additionalInfo.add(info);
        pix.setAdditionalInformation(additionalInfo);

        paymentReq.setPix(pix);

        // Split 100% plataforma
        SiteConfiguration config = siteConfigurationRepository.findActiveConfiguration()
                .orElseThrow(() -> new IllegalStateException("Configuração do site não encontrada"));

        OrderRequest.SplitRequest platformSplit = new OrderRequest.SplitRequest();
        platformSplit.setRecipientId(config.getPagarmeRecipientId());
        platformSplit.setAmount((int) amountCents);
        platformSplit.setType("flat");

        OrderRequest.SplitOptionsRequest options = new OrderRequest.SplitOptionsRequest();
        options.setChargeProcessingFee(true);
        options.setChargeRemainderFee(true);
        options.setLiable(true);
        platformSplit.setOptions(options);

        paymentReq.setSplit(List.of(platformSplit));
        orderRequest.setPayments(List.of(paymentReq));

        return orderRequest;
    }

    /**
     * Extrai dados do PIX da resposta do Pagar.me (mesmo padrão do PaymentService).
     */
    private void extractPixData(Payment payment, OrderResponse orderResponse) {
        if (orderResponse.getCharges() != null && !orderResponse.getCharges().isEmpty()) {
            OrderResponse.Charge charge = orderResponse.getCharges().get(0);
            if (charge.getLastTransaction() != null) {
                OrderResponse.LastTransaction tx = charge.getLastTransaction();
                payment.setPixQrCode(tx.getQrCode());
                payment.setPixQrCodeUrl(tx.getQrCodeUrl());

                if (tx.getExpiresAt() != null) {
                    try {
                        OffsetDateTime expiresAt = OffsetDateTime.parse(tx.getExpiresAt())
                                .atZoneSameInstant(TZ).toOffsetDateTime();
                        payment.setExpiresAt(expiresAt);
                    } catch (Exception e) {
                        log.warn("⚠️ Erro ao parsear expiresAt: {}", e.getMessage());
                    }
                }
            }
        }
    }
}
