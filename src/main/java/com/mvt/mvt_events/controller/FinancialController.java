package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.jpa.Transfer;
import com.mvt.mvt_events.service.FinancialService;
import com.mvt.mvt_events.service.PaymentGatewayService;
import com.mvt.mvt_events.service.TransferSchedulingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;


@RestController
@RequestMapping("/api/financial")
@RequiredArgsConstructor
public class FinancialController {

    private final FinancialService financialService;
    private final TransferSchedulingService transferSchedulingService;
    private final PaymentGatewayService paymentGatewayService;

    /**
     * Get financial summary for an event
     */
    @GetMapping("/events/{eventId}/summary")
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getEventFinancialSummary(@PathVariable Long eventId) {
        Map<String, Object> summary = financialService.getEventFinancialSummary(eventId);
        return ResponseEntity.ok(summary);
    }

    /**
     * Create a manual transfer for an event
     */
    @PostMapping("/events/{eventId}/transfers")
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('ADMIN')")
    public ResponseEntity<Transfer> createManualTransfer(
            @PathVariable Long eventId,
            @RequestBody CreateTransferRequest request) {

        Transfer transfer = financialService.createTransfer(
                eventId,
                request.amount(),
                Transfer.TransferType.MANUAL);

        return ResponseEntity.ok(transfer);
    }

    /**
     * Get transfer statistics
     */
    @GetMapping("/transfers/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TransferSchedulingService.TransferStats> getTransferStats() {
        TransferSchedulingService.TransferStats stats = transferSchedulingService.getTransferStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Get available transfer methods
     */
    @GetMapping("/transfer-methods")
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('ADMIN')")
    public ResponseEntity<java.util.List<Transfer.TransferMethod>> getAvailableTransferMethods() {
        java.util.List<Transfer.TransferMethod> methods = paymentGatewayService.getAvailableTransferMethods();
        return ResponseEntity.ok(methods);
    }

    /**
     * Calculate transfer fee
     */
    @GetMapping("/transfer-fee")
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> calculateTransferFee(
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) Transfer.TransferMethod method) {

        BigDecimal fee = method != null ? paymentGatewayService.getTransferFee(amount, method)
                : paymentGatewayService.getTransferFee(amount);

        String processingTime = method != null ? paymentGatewayService.getTransferProcessingTime(method)
                : "Depends on method";

        Map<String, Object> response = Map.of(
                "amount", amount,
                "fee", fee,
                "netAmount", amount.subtract(fee),
                "method", method != null ? method : "PIX (default)",
                "processingTime", processingTime);

        return ResponseEntity.ok(response);
    }

    /**
     * Process refund for a payment
     */
    @PostMapping("/payments/{paymentId}/refund")
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> processRefund(
            @PathVariable Long paymentId,
            @RequestBody RefundRequest request) {

        // This would need to be implemented with proper payment lookup
        // For now, returning a placeholder response
        Map<String, String> response = Map.of(
                "message", "Refund functionality not yet implemented",
                "paymentId", paymentId.toString(),
                "amount", request.amount().toString());

        return ResponseEntity.ok(response);
    }

    /**
     * Trigger manual transfer processing (admin only)
     */
    @PostMapping("/transfers/process")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> triggerTransferProcessing() {
        try {
            transferSchedulingService.processAutomaticTransfers();
            transferSchedulingService.processPendingTransfers();

            Map<String, String> response = Map.of(
                    "message", "Transfer processing triggered successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = Map.of(
                    "message", "Error triggering transfer processing",
                    "error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Request DTOs
    public record CreateTransferRequest(
            BigDecimal amount,
            Transfer.TransferMethod transferMethod,
            String destinationKey,
            String destinationBank,
            String destinationAgency,
            String destinationAccount) {
    }

    public record RefundRequest(
            BigDecimal amount,
            String reason) {
    }
}