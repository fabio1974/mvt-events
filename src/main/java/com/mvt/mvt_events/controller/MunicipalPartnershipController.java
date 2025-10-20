package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.dto.*;
import com.mvt.mvt_events.jpa.MunicipalPartnership;
import com.mvt.mvt_events.service.MunicipalPartnershipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Controller REST para MunicipalPartnership
 */
@RestController
@RequestMapping("/api/zapi10/partnerships")
@CrossOrigin(origins = "*")
@Tag(name = "Zapi10 - Partnerships", description = "Gerenciamento de parcerias municipais")
@SecurityRequirement(name = "bearerAuth")
public class MunicipalPartnershipController {

    @Autowired
    private MunicipalPartnershipService partnershipService;

    @PostMapping
    @Operation(summary = "Criar parceria municipal")
    public ResponseEntity<MunicipalPartnershipResponse> create(
            @RequestBody @Valid MunicipalPartnershipCreateRequest request) {

        MunicipalPartnership partnership = mapToEntity(request);
        MunicipalPartnership created = partnershipService.create(partnership);

        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(created));
    }

    @GetMapping
    @Operation(summary = "Listar parcerias com filtros")
    public Page<MunicipalPartnershipResponse> list(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean onlyValid,
            @RequestParam(required = false) String search,
            Pageable pageable) {

        MunicipalPartnership.PartnershipStatus partnershipStatus = status != null
                ? MunicipalPartnership.PartnershipStatus.valueOf(status)
                : null;

        Page<MunicipalPartnership> partnerships = partnershipService.findAll(
                city, state, partnershipStatus, onlyValid, search, pageable);

        return partnerships.map(this::mapToResponse);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar parceria por ID")
    public ResponseEntity<MunicipalPartnershipResponse> getById(@PathVariable Long id) {
        MunicipalPartnership partnership = partnershipService.findById(id);
        return ResponseEntity.ok(mapToResponse(partnership));
    }

    @GetMapping("/cnpj/{cnpj}")
    @Operation(summary = "Buscar parceria por CNPJ")
    public ResponseEntity<MunicipalPartnershipResponse> getByCnpj(@PathVariable String cnpj) {
        MunicipalPartnership partnership = partnershipService.findByCnpj(cnpj);
        return ResponseEntity.ok(mapToResponse(partnership));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Atualizar status da parceria")
    public ResponseEntity<MunicipalPartnershipResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {

        MunicipalPartnership.PartnershipStatus newStatus = MunicipalPartnership.PartnershipStatus.valueOf(status);

        MunicipalPartnership updated = partnershipService.updateStatus(id, newStatus);

        return ResponseEntity.ok(mapToResponse(updated));
    }

    @PostMapping("/{id}/renew")
    @Operation(summary = "Renovar parceria")
    public ResponseEntity<MunicipalPartnershipResponse> renew(
            @PathVariable Long id,
            @RequestParam String newEndDate) {

        LocalDate endDate = LocalDate.parse(newEndDate);
        MunicipalPartnership renewed = partnershipService.renew(id, endDate);

        return ResponseEntity.ok(mapToResponse(renewed));
    }

    @PostMapping("/{id}/suspend")
    @Operation(summary = "Suspender parceria")
    public ResponseEntity<MunicipalPartnershipResponse> suspend(
            @PathVariable Long id,
            @RequestParam String reason) {

        MunicipalPartnership suspended = partnershipService.suspend(id, reason);

        return ResponseEntity.ok(mapToResponse(suspended));
    }

    @PostMapping("/{id}/terminate")
    @Operation(summary = "Encerrar parceria")
    public ResponseEntity<MunicipalPartnershipResponse> terminate(@PathVariable Long id) {
        MunicipalPartnership terminated = partnershipService.terminate(id);
        return ResponseEntity.ok(mapToResponse(terminated));
    }

    private MunicipalPartnership mapToEntity(MunicipalPartnershipCreateRequest request) {
        MunicipalPartnership partnership = new MunicipalPartnership();
        partnership.setName(request.getName());
        partnership.setCity(request.getCity());
        partnership.setState(request.getState());
        partnership.setCnpj(request.getCnpj());
        partnership.setAgreementNumber(request.getAgreementNumber());
        partnership.setStartDate(request.getStartDate());
        partnership.setEndDate(request.getEndDate());
        // Nota: commissionDiscount não existe em MunicipalPartnership - remover se não
        // for necessário
        partnership.setContactName(request.getContactPerson());
        partnership.setContactEmail(request.getContactEmail());
        partnership.setContactPhone(request.getContactPhone());
        partnership.setNotes(request.getNotes());
        return partnership;
    }

    private MunicipalPartnershipResponse mapToResponse(MunicipalPartnership partnership) {
        boolean isValid = partnership.getStatus() == MunicipalPartnership.PartnershipStatus.ACTIVE &&
                LocalDate.now().isAfter(partnership.getStartDate()) &&
                (partnership.getEndDate() == null || LocalDate.now().isBefore(partnership.getEndDate()));

        return MunicipalPartnershipResponse.builder()
                .id(partnership.getId())
                .createdAt(partnership.getCreatedAt())
                .name(partnership.getName())
                .city(partnership.getCity())
                .state(partnership.getState())
                .cnpj(partnership.getCnpj())
                .agreementNumber(partnership.getAgreementNumber())
                .startDate(partnership.getStartDate())
                .endDate(partnership.getEndDate())
                .status(partnership.getStatus().name())
                .isValid(isValid)
                .discountPercentage(null) // Campo não existe em MunicipalPartnership
                .contactPerson(partnership.getContactName())
                .contactEmail(partnership.getContactEmail())
                .contactPhone(partnership.getContactPhone())
                .notes(partnership.getNotes())
                .build();
    }
}
