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

        BigDecimal deliveryCents = splitCalculator.toCents(deliveryFee);
        BigDecimal foodCents = splitCalculator.toCents(
                foodOrder.getSubtotal() != null ? foodOrder.getSubtotal() : BigDecimal.ZERO);
        BigDecimal totalCents = foodCents.add(deliveryCents);

        // Transfer 1: 87% do frete → courier
        registerTransferIfMissing(
                foodOrder, delivery, delivery.getCourier(),
                splitCalculator.calculateCourierTransferAmount(deliveryCents).longValueExact(),
                "Courier");

        // Transfer 2: 5% do total → organizer (gerente da organização do courier).
        // Indeterminado no checkout (dependia de qual courier aceitaria), por isso
        // só agora a gente sabe qual organizer recebe o repasse.
        User organizer = delivery.getOrganizer();
        if (organizer != null) {
            registerTransferIfMissing(
                    foodOrder, delivery, organizer,
                    splitCalculator.calculateOrganizerTransferAmount(totalCents).longValueExact(),
                    "Organizer");
        } else {
            log.debug("⏭️  Delivery #{} sem organizer — sem transfer 5% (plataforma absorve)",
                    delivery.getId());
        }
    }

    /**
     * Cria PagarmeTransfer PENDING para um destinatário específico se ainda não existir
     * (idempotente por foodOrder + delivery + recipient). Retorna o transfer criado ou null
     * se já existia ou amountCents <= 0.
     */
    private PagarmeTransfer registerTransferIfMissing(FoodOrder foodOrder, Delivery delivery,
                                                       User recipient, long amountCents,
                                                       String roleLabel) {
        if (recipient == null || amountCents <= 0) return null;

        boolean exists = transferRepository.findByFoodOrderId(foodOrder.getId()).stream()
                .anyMatch(t -> t.getDeliveryId() != null
                        && t.getDeliveryId().equals(delivery.getId())
                        && t.getRecipient() != null
                        && recipient.getId().equals(t.getRecipient().getId()));
        if (exists) {
            log.debug("⏭️  Transfer {} já registrado para FoodOrder #{} + Delivery #{} + recipient #{}",
                    roleLabel, foodOrder.getId(), delivery.getId(), recipient.getId());
            return null;
        }

        PagarmeTransfer transfer = PagarmeTransfer.builder()
                .foodOrder(foodOrder)
                .deliveryId(delivery.getId())
                .recipient(recipient)
                .recipientPagarmeId(recipient.getPagarmeRecipientId() != null ? recipient.getPagarmeRecipientId() : "")
                .amountCents(amountCents)
                .status(PagarmeTransfer.Status.PENDING)
                .createdAt(OffsetDateTime.now())
                .build();
        transferRepository.save(transfer);

        log.info("📋 [Admin to-do] Transfer PENDING criado — FoodOrder #{} Delivery #{} {} #{} amount={}¢ pixKey={}",
                foodOrder.getId(), delivery.getId(), roleLabel, recipient.getId(), amountCents,
                recipient.getPixKey() != null ? "✓" : "❌ não cadastrada");
        return transfer;
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
