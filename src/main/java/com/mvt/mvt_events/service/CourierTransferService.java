package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.*;
import com.mvt.mvt_events.payment.pixout.PixOutProvider;
import com.mvt.mvt_events.payment.service.PagarMeService;
import com.mvt.mvt_events.repository.PagarmeTransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Registra a transferência da plataforma → courier (87% do frete) quando um
 * courier aceita uma delivery de Zapi-Food já paga pelo customer no checkout.
 *
 * ⚠️ Fase 1: só registra em PagarmeTransfer status=PENDING (escrituração interna).
 * A execução real (via pagar.me withdrawal ou TED) fica pra fase posterior.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CourierTransferService {

    private final FoodOrderSplitCalculator splitCalculator;
    private final PagarMeService pagarMeService;
    private final PagarmeTransferRepository transferRepository;
    private final PixOutProvider pixOutProvider;

    /**
     * Registra transfer para courier se a delivery vem de FoodOrder paga no checkout.
     * Idempotente: não cria duplicados para a mesma delivery.
     */
    public void registerForCourierAccept(Delivery delivery) {
        if (delivery == null || delivery.getCourier() == null) return;

        FoodOrder foodOrder = delivery.getOrder();
        if (foodOrder == null) {
            log.debug("⏭️  Delivery #{} sem FoodOrder — transfer não aplicável", delivery.getId());
            return;
        }

        // Só processa pedidos pagos no checkout (PIX agora)
        if (foodOrder.getPaymentTiming() != FoodOrder.PaymentTiming.AT_CHECKOUT) {
            log.debug("⏭️  FoodOrder #{} com timing {} — transfer não aplicável",
                    foodOrder.getId(), foodOrder.getPaymentTiming());
            return;
        }
        if (foodOrder.getCustomerPaymentStatus() != FoodOrder.CustomerPaymentStatus.PAID) {
            log.debug("⏭️  FoodOrder #{} ainda não paga — transfer adiado",
                    foodOrder.getId());
            return;
        }

        BigDecimal deliveryFee = foodOrder.getDeliveryFee();
        if (deliveryFee == null || deliveryFee.signum() <= 0) {
            log.debug("⏭️  FoodOrder #{} sem delivery fee — nada a transferir", foodOrder.getId());
            return;
        }

        // Idempotência
        boolean exists = transferRepository.findByFoodOrderId(foodOrder.getId()).stream()
                .anyMatch(t -> t.getDeliveryId() != null && t.getDeliveryId().equals(delivery.getId()));
        if (exists) {
            log.debug("⏭️  Transfer já registrado para FoodOrder #{} + Delivery #{}",
                    foodOrder.getId(), delivery.getId());
            return;
        }

        User courier = delivery.getCourier();
        BigDecimal deliveryCents = splitCalculator.toCents(deliveryFee);
        long amountCents = splitCalculator.calculateCourierTransferAmount(deliveryCents).longValueExact();

        PagarmeTransfer transfer = PagarmeTransfer.builder()
                .foodOrder(foodOrder)
                .deliveryId(delivery.getId())
                .recipient(courier)
                .recipientPagarmeId(courier.getPagarmeRecipientId() != null ? courier.getPagarmeRecipientId() : "")
                .amountCents(amountCents)
                .status(PagarmeTransfer.Status.PENDING)
                .createdAt(OffsetDateTime.now())
                .build();
        transferRepository.save(transfer);

        // Modo semi-automático: o transfer fica PENDING até admin disparar via UI
        // ("Dívidas com Couriers" → botão Enviar PIX ou Marcar Pago).
        log.info("📋 [Admin to-do] Transfer PENDING criado — FoodOrder #{} Delivery #{} Courier #{} amount={}¢ pixKey={}",
                foodOrder.getId(), delivery.getId(), courier.getId(), amountCents,
                courier.getPixKey() != null ? "✓" : "❌ não cadastrada");
    }

    /**
     * Tenta executar um transfer via PIX out. Atualiza o status da entidade.
     * Usado pelo scheduler pra reprocessar PENDINGs.
     */
    public void executeTransfer(PagarmeTransfer transfer, User courier) {
        String externalId = "transfer-" + transfer.getId();
        PixOutProvider.PixOutResult result = pixOutProvider.send(courier, transfer.getAmountCents(), externalId);

        switch (result.status()) {
            case SUCCEEDED -> {
                transfer.setStatus(PagarmeTransfer.Status.SUCCEEDED);
                transfer.setPagarmeTransferId(result.providerTransactionId());
                transfer.setExecutedAt(OffsetDateTime.now());
            }
            case PENDING -> {
                transfer.setPagarmeTransferId(result.providerTransactionId());
                // status continua PENDING; webhook depois confirma
            }
            case FAILED -> {
                transfer.setStatus(PagarmeTransfer.Status.FAILED);
                transfer.setErrorMessage(result.errorMessage());
                transfer.setExecutedAt(OffsetDateTime.now());
            }
        }
        transferRepository.save(transfer);
        log.info("💸 Transfer #{} → {} (FoodOrder #{}, Delivery #{}, amount={}¢)",
                transfer.getId(), transfer.getStatus(), transfer.getFoodOrder().getId(),
                transfer.getDeliveryId(), transfer.getAmountCents());
    }
}
