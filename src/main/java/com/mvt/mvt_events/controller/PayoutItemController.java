package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.jpa.PayoutItem;
import com.mvt.mvt_events.service.PayoutItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Controller para gerenciar PayoutItems (items de repasse individual).
 * Cada PayoutItem representa um repasse específico para um beneficiário.
 */
@RestController
@RequestMapping("/api/payout-items")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payout Items", description = "Endpoints para gerenciar items de repasse individual")
public class PayoutItemController {

    private final PayoutItemService payoutItemService;

    // ============================================================================
    // GET PAYOUT ITEMS
    // ============================================================================

    @GetMapping("/beneficiary/{beneficiaryId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADM', 'COURIER')")
    @Operation(summary = "Busca items de repasse por beneficiário")
    public ResponseEntity<List<PayoutItem>> getByBeneficiary(@PathVariable UUID beneficiaryId) {
        log.info("Fetching payout items for beneficiary: {}", beneficiaryId);
        List<PayoutItem> items = payoutItemService.findByBeneficiary(beneficiaryId);
        return ResponseEntity.ok(items);
    }

    @GetMapping("/beneficiary/{beneficiaryId}/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADM', 'COURIER')")
    @Operation(summary = "Busca items de repasse pendentes por beneficiário")
    public ResponseEntity<List<PayoutItem>> getPendingByBeneficiary(@PathVariable UUID beneficiaryId) {
        log.info("Fetching pending payout items for beneficiary: {}", beneficiaryId);
        List<PayoutItem> items = payoutItemService.findPendingByBeneficiary(beneficiaryId);
        return ResponseEntity.ok(items);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADM')")
    @Operation(summary = "Busca items de repasse por status")
    public ResponseEntity<List<PayoutItem>> getByStatus(@PathVariable PayoutItem.PayoutStatus status) {
        log.info("Fetching payout items with status: {}", status);
        List<PayoutItem> items = payoutItemService.findByStatus(status);
        return ResponseEntity.ok(items);
    }

    @GetMapping("/beneficiary/{beneficiaryId}/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADM', 'COURIER')")
    @Operation(summary = "Busca items de repasse por beneficiário e status")
    public ResponseEntity<List<PayoutItem>> getByBeneficiaryAndStatus(
            @PathVariable UUID beneficiaryId,
            @PathVariable PayoutItem.PayoutStatus status) {
        log.info("Fetching payout items for beneficiary {} with status {}", beneficiaryId, status);
        List<PayoutItem> items = payoutItemService.findByBeneficiaryAndStatus(beneficiaryId, status);
        return ResponseEntity.ok(items);
    }

    // ============================================================================
    // UPDATE PAYOUT ITEM STATUS
    // ============================================================================

    @PutMapping("/{itemId}/mark-paid")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADM')")
    @Operation(summary = "Marca item de repasse como pago")
    public ResponseEntity<PayoutItem> markAsPaid(
            @PathVariable Long itemId,
            @RequestParam String reference,
            @RequestParam PayoutItem.PaymentMethod method) {
        log.info("Marking payout item {} as paid with reference {} via {}", itemId, reference, method);
        PayoutItem item = payoutItemService.markAsPaid(itemId, reference, method);
        return ResponseEntity.ok(item);
    }

    @PutMapping("/{itemId}/mark-failed")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADM')")
    @Operation(summary = "Marca item de repasse como falho")
    public ResponseEntity<PayoutItem> markAsFailed(
            @PathVariable Long itemId,
            @RequestParam String reason) {
        log.info("Marking payout item {} as failed: {}", itemId, reason);
        PayoutItem item = payoutItemService.markAsFailed(itemId, reason);
        return ResponseEntity.ok(item);
    }

    @PutMapping("/{itemId}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADM')")
    @Operation(summary = "Cancela item de repasse")
    public ResponseEntity<PayoutItem> cancel(
            @PathVariable Long itemId,
            @RequestParam String reason) {
        log.info("Cancelling payout item {}: {}", itemId, reason);
        PayoutItem item = payoutItemService.cancel(itemId, reason);
        return ResponseEntity.ok(item);
    }

    @PutMapping("/process-batch")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Processa múltiplos items de repasse")
    public ResponseEntity<String> processPayoutItems(
            @RequestParam List<Long> itemIds,
            @RequestParam String reference,
            @RequestParam PayoutItem.PaymentMethod method) {
        log.info("Processing {} payout items with reference {} via {}", itemIds.size(), reference, method);
        payoutItemService.processPayoutItems(itemIds, reference, method);
        return ResponseEntity.ok("Processados " + itemIds.size() + " items de repasse");
    }

    // ============================================================================
    // STATISTICS
    // ============================================================================

    @GetMapping("/beneficiary/{beneficiaryId}/total-paid")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADM', 'COURIER')")
    @Operation(summary = "Calcula total pago para um beneficiário")
    public ResponseEntity<BigDecimal> getTotalPaid(@PathVariable UUID beneficiaryId) {
        log.info("Calculating total paid for beneficiary: {}", beneficiaryId);
        BigDecimal total = payoutItemService.calculateTotalPaid(beneficiaryId);
        return ResponseEntity.ok(total);
    }

    @GetMapping("/beneficiary/{beneficiaryId}/total-pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADM', 'COURIER')")
    @Operation(summary = "Calcula total pendente para um beneficiário")
    public ResponseEntity<BigDecimal> getTotalPending(@PathVariable UUID beneficiaryId) {
        log.info("Calculating total pending for beneficiary: {}", beneficiaryId);
        BigDecimal total = payoutItemService.calculateTotalPending(beneficiaryId);
        return ResponseEntity.ok(total);
    }
}
