package com.mvt.mvt_events.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mvt.mvt_events.jpa.Currency;
import com.mvt.mvt_events.jpa.FoodOrder;
import com.mvt.mvt_events.jpa.Organization;
import com.mvt.mvt_events.jpa.Payment;
import com.mvt.mvt_events.jpa.PaymentMethod;
import com.mvt.mvt_events.jpa.PaymentProvider;
import com.mvt.mvt_events.jpa.PaymentStatus;
import com.mvt.mvt_events.jpa.SiteConfiguration;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.payment.dto.OrderRequest;
import com.mvt.mvt_events.payment.dto.OrderResponse;
import com.mvt.mvt_events.payment.service.PagarMeService;
import com.mvt.mvt_events.repository.FoodOrderRepository;
import com.mvt.mvt_events.repository.OrganizationRepository;
import com.mvt.mvt_events.repository.PaymentRepository;
import com.mvt.mvt_events.repository.SiteConfigurationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Pagamento de FoodOrder (Zapi-Food) — fase 1: apenas PIX.
 *
 * Checkout: cria order no pagar.me com split de 3 recipients:
 *  - Estabelecimento (client): 87% × food
 *  - Organizer (gerente do client, se houver): 5% × total
 *  - Plataforma: o resto (8% food + 95% delivery; ou 13% food + 95% delivery sem organizer)
 *
 * Posteriormente (quando courier aceita delivery) o saldo do frete retido na
 * plataforma deve ser transferido 87% para o courier via {@link PagarMeService#transferToRecipient}
 * — disparado pelo CourierTransferService/hook no DeliveryService.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class FoodOrderPaymentService {

    private static final int PIX_EXPIRATION_SECONDS = 30 * 60; // 30 min

    private final PagarMeService pagarMeService;
    private final FoodOrderSplitCalculator splitCalculator;
    private final FoodOrderRepository foodOrderRepository;
    private final SiteConfigurationRepository siteConfigRepository;
    private final OrganizationRepository organizationRepository;
    private final PaymentRepository paymentRepository;
    private final ObjectMapper objectMapper;

    /**
     * Cria a cobrança PIX no checkout para um FoodOrder. Popula no próprio FoodOrder
     * os campos pagarmeOrderId, pixQrCode, pixQrCodeUrl, customerPaymentStatus=PENDING.
     *
     * Pré-requisito: o client (restaurante) precisa ter pagarmeRecipientId.
     * @throws RuntimeException se o client não tem recipient pagar.me.
     */
    public FoodOrder createPixForCheckout(FoodOrder order) {
        if (order.getCustomer() == null || order.getClient() == null) {
            throw new RuntimeException("FoodOrder sem customer ou client");
        }
        if (order.getTotal() == null || order.getTotal().signum() <= 0) {
            throw new RuntimeException("FoodOrder sem valor total");
        }

        SiteConfiguration config = siteConfigRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("SiteConfiguration não configurado"));

        User client = order.getClient();
        if (client.getPagarmeRecipientId() == null || client.getPagarmeRecipientId().isBlank()) {
            throw new RuntimeException(
                    "Estabelecimento " + client.getId() + " não tem pagarmeRecipientId cadastrado — " +
                    "cliente precisa completar onboarding no pagar.me antes de receber pedidos Zapi-Food");
        }

        User customer = order.getCustomer();
        if (customer.getPhoneDdd() == null || customer.getPhoneDdd().isBlank()
                || customer.getPhoneNumber() == null || customer.getPhoneNumber().isBlank()) {
            throw new RuntimeException(
                    "Customer " + customer.getId() + " sem telefone cadastrado — " +
                    "pagar.me exige phone para gerar PIX. Atualize o perfil em \"Meus Dados\".");
        }

        // Organizer e courier são INDETERMINADOS no checkout (dependem de qual courier aceita
        // a delivery, que rola horas depois). Por isso o split no Pagar.me é sempre 2-way
        // (estabelecimento + plataforma); os 5% do organizer e 87% do frete saem via
        // pagarme_transfers PENDING quando o courier aceita (CourierTransferService).
        User organizer = resolveOrganizer(client);
        boolean hasOrganizer = false;

        BigDecimal foodCents = splitCalculator.toCents(order.getSubtotal());
        BigDecimal deliveryCents = splitCalculator.toCents(
                order.getDeliveryFee() != null ? order.getDeliveryFee() : BigDecimal.ZERO);

        FoodOrderSplitCalculator.CheckoutSplit split =
                splitCalculator.calculateCheckoutSplit(foodCents, deliveryCents, config, hasOrganizer);

        log.info("🍔 [FoodOrder #{}] Split checkout: client={}¢ organizer={}¢ platform={}¢ total={}¢",
                order.getId(), split.getClientAmountCents(), split.getOrganizerAmountCents(),
                split.getPlatformAmountCents(), split.getTotalCents());

        OrderRequest request = buildOrderRequest(order, client, organizer, config, split, hasOrganizer);
        OrderResponse response = pagarMeService.createOrderWithFullResponse(request);

        applyPaymentInfo(order, response);
        FoodOrder saved = foodOrderRepository.save(order);

        // Persiste Payment para auditoria/reconciliação financeira.
        // Sem isso, requests/responses do Pagar.me só ficam nos logs do Render (~7 dias).
        persistPayment(saved, request, response);

        return saved;
    }

    /**
     * Cria registro {@link Payment} associado ao FoodOrder pago via PIX no checkout.
     * Inclui request/response JSON para auditoria, QR Code e expiresAt.
     */
    private void persistPayment(FoodOrder order, OrderRequest request, OrderResponse response) {
        try {
            Payment payment = new Payment();
            payment.setPaymentType("ZAPI_FOOD");
            payment.setProvider(PaymentProvider.PAGARME);
            payment.setProviderPaymentId(response.getId());
            payment.setPayer(order.getCustomer());
            payment.setAmount(order.getTotal());
            payment.setCurrency(Currency.BRL);
            payment.setPaymentMethod(PaymentMethod.PIX);
            payment.setStatus(PaymentStatus.PENDING);
            payment.setNotes("Zapi-Food #" + order.getId());

            // QR Code + expiresAt do PIX
            extractPixData(payment, response);

            // Idempotência simples no transaction_id: usa a transação do Pagar.me se houver,
            // senão usa providerPaymentId pra garantir uniqueness em re-runs improváveis.
            String txId = extractTransactionId(response);
            payment.setTransactionId(txId != null ? txId : response.getId());

            // Auditoria — JSON do pedido + resposta
            try {
                payment.setRequest(objectMapper.writeValueAsString(request));
                payment.setResponse(objectMapper.writeValueAsString(response));
            } catch (Exception e) {
                log.warn("⚠️ Falha ao serializar request/response do FoodOrder #{}: {}",
                        order.getId(), e.getMessage());
            }

            paymentRepository.save(payment);
            log.info("💾 Payment FoodOrder salvo: id={} pagarmeOrderId={} amount=R$ {} status={}",
                    payment.getId(), response.getId(), payment.getAmount(), payment.getStatus());
        } catch (Exception e) {
            // Não derruba o checkout — pagar.me já criou a order com sucesso, FoodOrder já salvo.
            // Mas alerta loud porque é problema de auditoria/reconciliação.
            log.error("❌ Falha ao persistir Payment do FoodOrder #{} (pagamento já criado no Pagar.me): {}",
                    order.getId(), e.getMessage(), e);
        }
    }

    private String extractTransactionId(OrderResponse response) {
        if (response.getCharges() != null && !response.getCharges().isEmpty()) {
            var charge = response.getCharges().get(0);
            if (charge.getLastTransaction() != null && charge.getLastTransaction().getId() != null) {
                return charge.getLastTransaction().getId();
            }
        }
        return null;
    }

    private void extractPixData(Payment payment, OrderResponse response) {
        try {
            if (response.getCharges() != null && !response.getCharges().isEmpty()) {
                var charge = response.getCharges().get(0);
                if (charge.getLastTransaction() != null) {
                    var tx = charge.getLastTransaction();
                    payment.setPixQrCode(tx.getQrCode());
                    payment.setPixQrCodeUrl(tx.getQrCodeUrl());
                    if (tx.getExpiresAt() != null) {
                        try {
                            payment.setExpiresAt(OffsetDateTime.parse(tx.getExpiresAt())
                                    .atZoneSameInstant(ZoneId.of("America/Fortaleza"))
                                    .toOffsetDateTime());
                        } catch (Exception ignore) { /* parse pode falhar — não é crítico */ }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ Não foi possível extrair dados PIX da resposta pagar.me: {}", e.getMessage());
        }
    }

    /**
     * Só marca a intenção (PIX na entrega) sem chamar pagar.me.
     */
    public FoodOrder markPayOnDelivery(FoodOrder order) {
        order.setCustomerPaymentMethod(com.mvt.mvt_events.jpa.PaymentMethod.PIX);
        order.setPaymentTiming(FoodOrder.PaymentTiming.ON_DELIVERY);
        order.setCustomerPaymentStatus(FoodOrder.CustomerPaymentStatus.PENDING);
        return foodOrderRepository.save(order);
    }

    private User resolveOrganizer(User client) {
        // Organization onde este CLIENT é owner → pega o dono (ORGANIZER) de outro lugar não é direto.
        // Aqui: se o client pertence a alguma organização (via employmentContract/organization), retornar o owner.
        // Simplificado: procura Organization cujo owner == client (se client é ORGANIZER) — no fluxo normal,
        // o organizer é quem é dono. Para este ponto, sem organizer = plataforma absorve 5%.
        Organization org = organizationRepository.findByOwner(client).orElse(null);
        return org != null ? org.getOwner() : null;
    }

    private OrderRequest buildOrderRequest(FoodOrder order, User client, User organizer,
                                           SiteConfiguration config,
                                           FoodOrderSplitCalculator.CheckoutSplit split,
                                           boolean hasOrganizer) {
        long totalCents = split.getTotalCents().longValue();
        User customer = order.getCustomer();

        OrderRequest.PhonesRequest phones = OrderRequest.PhonesRequest.builder()
                .mobilePhone(OrderRequest.PhoneRequest.builder()
                        .countryCode("55")
                        .areaCode(customer.getPhoneDdd())
                        .number(customer.getPhoneNumber())
                        .build())
                .build();

        List<OrderRequest.SplitRequest> splits = new ArrayList<>();

        // Restaurante (estabelecimento) — 87% da comida
        splits.add(OrderRequest.SplitRequest.builder()
                .amount(split.getClientAmountCents().intValue())
                .type("flat")
                .recipientId(client.getPagarmeRecipientId())
                .options(OrderRequest.SplitOptionsRequest.builder()
                        // Mesmo padrão de SplitCalculator (corridas): plataforma absorve
                        // chargebacks (liable=false), taxa de processamento (charge_processing_fee=false)
                        // e arredondamento (charge_remainder_fee=false). Estabelecimento recebe limpo.
                        .liable(false)
                        .chargeProcessingFee(false)
                        .chargeRemainderFee(false)
                        .build())
                .build());

        // Organizer — 5% do total (se houver)
        if (hasOrganizer && split.getOrganizerAmountCents().signum() > 0) {
            splits.add(OrderRequest.SplitRequest.builder()
                    .amount(split.getOrganizerAmountCents().intValue())
                    .type("flat")
                    .recipientId(organizer.getPagarmeRecipientId())
                    .options(OrderRequest.SplitOptionsRequest.builder()
                            .liable(false)
                            .chargeProcessingFee(false)
                            .chargeRemainderFee(false)
                            .build())
                    .build());
        }

        // Plataforma — resto (absorve arredondamento + retem 95% do frete até courier aceitar)
        if (config.getPagarmeRecipientId() != null && !config.getPagarmeRecipientId().isBlank()
                && split.getPlatformAmountCents().signum() > 0) {
            splits.add(OrderRequest.SplitRequest.builder()
                    .amount(split.getPlatformAmountCents().intValue())
                    .type("flat")
                    .recipientId(config.getPagarmeRecipientId())
                    .options(OrderRequest.SplitOptionsRequest.builder()
                            .liable(true)
                            .chargeProcessingFee(true)
                            .chargeRemainderFee(true)
                            .build())
                    .build());
        }

        String description = "Zapi-Food #" + order.getId();

        return OrderRequest.builder()
                .closed(true)
                .items(List.of(OrderRequest.ItemRequest.builder()
                        .amount(totalCents)
                        .description(description)
                        .quantity(1L)
                        .code(String.valueOf(order.getId()))
                        .build()))
                .customer(OrderRequest.CustomerRequest.builder()
                        .name(customer.getName())
                        .email(customer.getUsername())
                        .document(customer.getDocumentNumber())
                        .type("individual")
                        .phones(phones)
                        .build())
                .payments(List.of(OrderRequest.PaymentRequest.builder()
                        .paymentMethod("pix")
                        .amount(totalCents)
                        .pix(OrderRequest.PixRequest.builder()
                                .expiresIn(String.valueOf(PIX_EXPIRATION_SECONDS))
                                .build())
                        .split(splits)
                        .build()))
                .build();
    }

    private void applyPaymentInfo(FoodOrder order, OrderResponse response) {
        order.setCustomerPaymentMethod(com.mvt.mvt_events.jpa.PaymentMethod.PIX);
        order.setPaymentTiming(FoodOrder.PaymentTiming.AT_CHECKOUT);
        order.setCustomerPaymentStatus(FoodOrder.CustomerPaymentStatus.PENDING);
        order.setPagarmeOrderId(response.getId());
        order.setPixExpiresAt(OffsetDateTime.now(ZoneId.of("America/Fortaleza"))
                .plusSeconds(PIX_EXPIRATION_SECONDS));
        // Extrair dados do PIX se houver charges/transactions no response
        try {
            if (response.getCharges() != null && !response.getCharges().isEmpty()) {
                var charge = response.getCharges().get(0);
                if (charge.getLastTransaction() != null) {
                    order.setPixQrCode(charge.getLastTransaction().getQrCode());
                    order.setPixQrCodeUrl(charge.getLastTransaction().getQrCodeUrl());
                }
            }
        } catch (Exception e) {
            log.warn("Não foi possível extrair QR code da resposta pagar.me", e);
        }
    }
}
