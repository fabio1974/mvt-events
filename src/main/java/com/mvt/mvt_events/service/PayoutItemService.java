package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.Payment;
import com.mvt.mvt_events.jpa.PayoutItem;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.repository.PayoutItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service para gerenciar PayoutItems.
 * Cada PayoutItem representa um repasse individual para um beneficiário.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PayoutItemService {

    private final PayoutItemRepository payoutItemRepository;

    // ============================================================================
    // CREATE PAYOUT ITEMS
    // ============================================================================

    /**
     * Cria um item de repasse para um beneficiário
     */
    @Transactional
    public PayoutItem createPayoutItem(
            Payment payment,
            User beneficiary,
            BigDecimal amount,
            PayoutItem.ValueType valueType) {

        PayoutItem item = new PayoutItem();
        item.setPayment(payment);
        item.setBeneficiary(beneficiary);
        item.setItemValue(amount);
        item.setValueType(valueType);
        item.setStatus(PayoutItem.PayoutStatus.PENDING);

        PayoutItem saved = payoutItemRepository.save(item);
        log.info("Created payout item {} for beneficiary {} with value {} ({})",
                saved.getId(), beneficiary.getId(), amount, valueType);

        return saved;
    }

    // ============================================================================
    // FIND PAYOUT ITEMS
    // ============================================================================

    /**
     * Busca items de repasse por beneficiário
     */
    @Transactional(readOnly = true)
    public List<PayoutItem> findByBeneficiary(UUID beneficiaryId) {
        return payoutItemRepository.findByBeneficiaryIdOrderByCreatedAtDesc(beneficiaryId);
    }

    /**
     * Busca items de repasse pendentes por beneficiário
     */
    @Transactional(readOnly = true)
    public List<PayoutItem> findPendingByBeneficiary(UUID beneficiaryId) {
        return payoutItemRepository.findPendingByBeneficiaryId(beneficiaryId);
    }

    /**
     * Busca items de repasse por status
     */
    @Transactional(readOnly = true)
    public List<PayoutItem> findByStatus(PayoutItem.PayoutStatus status) {
        return payoutItemRepository.findByStatusOrderByCreatedAtAsc(status);
    }

    /**
     * Busca items de repasse por beneficiário e status
     */
    @Transactional(readOnly = true)
    public List<PayoutItem> findByBeneficiaryAndStatus(UUID beneficiaryId, PayoutItem.PayoutStatus status) {
        return payoutItemRepository.findByBeneficiaryIdAndStatusOrderByCreatedAtDesc(beneficiaryId, status);
    }

    /**
     * Busca items de repasse por tipo de valor e status
     */
    @Transactional(readOnly = true)
    public List<PayoutItem> findByValueTypeAndStatus(
            PayoutItem.ValueType valueType,
            PayoutItem.PayoutStatus status) {
        return payoutItemRepository.findByValueTypeAndStatus(valueType, status);
    }

    // ============================================================================
    // UPDATE PAYOUT ITEM STATUS
    // ============================================================================

    /**
     * Marca um item de repasse como pago
     */
    @Transactional
    public PayoutItem markAsPaid(Long itemId, String reference, PayoutItem.PaymentMethod method) {
        PayoutItem item = payoutItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("PayoutItem não encontrado: " + itemId));

        item.markAsPaid(reference, method);
        PayoutItem saved = payoutItemRepository.save(item);

        log.info("Marked payout item {} as PAID with reference {} via {}",
                itemId, reference, method);

        return saved;
    }

    /**
     * Marca um item de repasse como falho
     */
    @Transactional
    public PayoutItem markAsFailed(Long itemId, String reason) {
        PayoutItem item = payoutItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("PayoutItem não encontrado: " + itemId));

        item.markAsFailed(reason);
        PayoutItem saved = payoutItemRepository.save(item);

        log.info("Marked payout item {} as FAILED: {}", itemId, reason);

        return saved;
    }

    /**
     * Cancela um item de repasse
     */
    @Transactional
    public PayoutItem cancel(Long itemId, String reason) {
        PayoutItem item = payoutItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("PayoutItem não encontrado: " + itemId));

        item.cancel(reason);
        PayoutItem saved = payoutItemRepository.save(item);

        log.info("Cancelled payout item {}: {}", itemId, reason);

        return saved;
    }

    /**
     * Processa múltiplos items de repasse
     */
    @Transactional
    public void processPayoutItems(List<Long> itemIds, String reference, PayoutItem.PaymentMethod method) {
        for (Long itemId : itemIds) {
            try {
                markAsPaid(itemId, reference, method);
            } catch (Exception e) {
                log.error("Error processing payout item {}: {}", itemId, e.getMessage());
                markAsFailed(itemId, e.getMessage());
            }
        }
    }

    // ============================================================================
    // STATISTICS
    // ============================================================================

    /**
     * Calcula total pago para um beneficiário
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalPaid(UUID beneficiaryId) {
        Double sum = payoutItemRepository.sumPaidAmountByBeneficiaryId(beneficiaryId);
        return sum != null ? BigDecimal.valueOf(sum) : BigDecimal.ZERO;
    }

    /**
     * Calcula total pendente para um beneficiário
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalPending(UUID beneficiaryId) {
        Double sum = payoutItemRepository.sumPendingAmountByBeneficiaryId(beneficiaryId);
        return sum != null ? BigDecimal.valueOf(sum) : BigDecimal.ZERO;
    }
}
