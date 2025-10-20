package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.dto.*;
import com.mvt.mvt_events.jpa.UnifiedPayout;
import com.mvt.mvt_events.service.UnifiedPayoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller REST para UnifiedPayout
 */
@RestController
@RequestMapping("/api/zapi10/payouts")
@CrossOrigin(origins = "*")
@Tag(name = "Zapi10 - Payouts", description = "Gerenciamento de repasses periódicos")
@SecurityRequirement(name = "bearerAuth")
public class UnifiedPayoutController {

    @Autowired
    private UnifiedPayoutService payoutService;

    @PostMapping
    @Operation(summary = "Criar payout manual")
    public ResponseEntity<UnifiedPayoutResponse> create(
            @RequestBody @Valid UnifiedPayoutCreateRequest request) {

        UnifiedPayout payout = new UnifiedPayout();
        payout.setPeriod(request.getPeriod());
        payout.setBeneficiaryType(UnifiedPayout.BeneficiaryType.valueOf(request.getBeneficiaryType()));
        payout.setTotalAmount(request.getTotalAmount());
        payout.setNotes(request.getNotes());

        if (request.getPaymentMethod() != null) {
            payout.setPaymentMethod(UnifiedPayout.PayoutMethod.valueOf(request.getPaymentMethod()));
        }

        UUID beneficiaryId = UUID.fromString(request.getBeneficiaryId());
        UnifiedPayout created = payoutService.create(payout, beneficiaryId);

        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(created));
    }

    @GetMapping
    @Operation(summary = "Listar payouts com filtros")
    public Page<UnifiedPayoutResponse> list(
            @RequestParam(required = false) String beneficiaryId,
            @RequestParam(required = false) String beneficiaryType,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String status,
            Pageable pageable) {

        UUID beneficiaryUuid = beneficiaryId != null ? UUID.fromString(beneficiaryId) : null;
        UnifiedPayout.BeneficiaryType type = beneficiaryType != null
                ? UnifiedPayout.BeneficiaryType.valueOf(beneficiaryType)
                : null;
        UnifiedPayout.PayoutStatus payoutStatus = status != null ? UnifiedPayout.PayoutStatus.valueOf(status) : null;

        Page<UnifiedPayout> payouts = payoutService.findAll(
                beneficiaryUuid, type, period, payoutStatus, pageable);

        return payouts.map(this::mapToResponse);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar payout por ID")
    public ResponseEntity<UnifiedPayoutResponse> getById(@PathVariable Long id) {
        UnifiedPayout payout = payoutService.findById(id);
        return ResponseEntity.ok(mapToResponse(payout));
    }

    @GetMapping("/me")
    @Operation(summary = "Buscar payouts do usuário autenticado")
    public ResponseEntity<?> getMyPayouts(Authentication authentication) {
        UUID beneficiaryId = UUID.fromString(authentication.getName());
        var payouts = payoutService.findByBeneficiary(beneficiaryId);
        return ResponseEntity.ok(payouts.stream().map(this::mapToResponse).toList());
    }

    @GetMapping("/period/{period}/pending")
    @Operation(summary = "Buscar payouts pendentes de um período")
    public ResponseEntity<?> getPendingByPeriod(@PathVariable String period) {
        var payouts = payoutService.findPendingByPeriod(period);
        return ResponseEntity.ok(payouts.stream().map(this::mapToResponse).toList());
    }

    @PostMapping("/{id}/process")
    @Operation(summary = "Processar payout", description = "Marca como em processamento. Status: PENDING → PROCESSING")
    public ResponseEntity<UnifiedPayoutResponse> process(
            @PathVariable Long id,
            @RequestParam String paymentMethod,
            @RequestParam(required = false) String paymentReference) {

        UnifiedPayout.PayoutMethod method = UnifiedPayout.PayoutMethod.valueOf(paymentMethod);
        UnifiedPayout processed = payoutService.processPayout(id, method, paymentReference);

        return ResponseEntity.ok(mapToResponse(processed));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Completar payout", description = "Marca como pago. Status: PROCESSING → COMPLETED")
    public ResponseEntity<UnifiedPayoutResponse> complete(@PathVariable Long id) {
        UnifiedPayout completed = payoutService.completePayout(id);
        return ResponseEntity.ok(mapToResponse(completed));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancelar payout")
    public ResponseEntity<UnifiedPayoutResponse> cancel(
            @PathVariable Long id,
            @RequestParam String reason) {

        UnifiedPayout cancelled = payoutService.cancelPayout(id, reason);
        return ResponseEntity.ok(mapToResponse(cancelled));
    }

    @GetMapping("/period/{period}/total")
    @Operation(summary = "Total pago em um período")
    public ResponseEntity<?> getTotalByPeriod(@PathVariable String period) {
        Double total = payoutService.getTotalPaidByPeriod(period);
        return ResponseEntity.ok().body(java.util.Map.of("period", period, "totalPaid", total));
    }

    private UnifiedPayoutResponse mapToResponse(UnifiedPayout payout) {
        return UnifiedPayoutResponse.builder()
                .id(payout.getId())
                .createdAt(payout.getCreatedAt())
                .beneficiaryId(payout.getBeneficiary().getId().toString())
                .beneficiaryName(payout.getBeneficiary().getName())
                .beneficiaryType(payout.getBeneficiaryType().name())
                .period(payout.getPeriod())
                .startDate(payout.getStartDate())
                .endDate(payout.getEndDate())
                .totalAmount(payout.getTotalAmount())
                .itemCount(payout.getItemCount())
                .status(payout.getStatus().name())
                .paidAt(payout.getPaidAt())
                .paymentMethod(payout.getPaymentMethod() != null ? payout.getPaymentMethod().name() : null)
                .paymentReference(payout.getPaymentReference())
                .notes(payout.getNotes())
                .build();
    }
}
