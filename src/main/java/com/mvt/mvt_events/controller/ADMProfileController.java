package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.dto.*;
import com.mvt.mvt_events.jpa.ADMProfile;
import com.mvt.mvt_events.service.ADMProfileService;
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

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Controller REST para ADMProfile
 */
@RestController
@RequestMapping("/api/zapi10/adm")
@CrossOrigin(origins = "*")
@Tag(name = "Zapi10 - ADM", description = "Gerenciamento de perfis ADM")
@SecurityRequirement(name = "bearerAuth")
public class ADMProfileController {

    @Autowired
    private ADMProfileService admProfileService;

    @PostMapping
    @Operation(summary = "Criar perfil ADM")
    public ResponseEntity<ADMProfileResponse> create(
            @RequestBody @Valid ADMProfileCreateRequest request) {

        ADMProfile profile = new ADMProfile();
        profile.setRegion(request.getRegion());
        profile.setCommissionPercentage(request.getCommissionPercentage());

        UUID userId = UUID.fromString(request.getUserId());

        ADMProfile created = admProfileService.create(profile, userId);

        // Vincular parceria se fornecida
        if (request.getPartnershipId() != null) {
            created = admProfileService.linkToPartnership(userId, request.getPartnershipId());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(created));
    }

    @GetMapping
    @Operation(summary = "Listar ADMs com filtros")
    public Page<ADMProfileResponse> list(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            Pageable pageable) {

        ADMProfile.ADMStatus admStatus = status != null ? ADMProfile.ADMStatus.valueOf(status) : null;

        Page<ADMProfile> adms = admProfileService.findAll(region, admStatus, search, pageable);
        return adms.map(this::mapToResponse);
    }

    @GetMapping("/me")
    @Operation(summary = "Buscar perfil do ADM autenticado")
    public ResponseEntity<ADMProfileResponse> getMyProfile(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        ADMProfile profile = admProfileService.findByUserId(userId);
        return ResponseEntity.ok(mapToResponse(profile));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Buscar perfil ADM por user ID")
    public ResponseEntity<ADMProfileResponse> getByUserId(@PathVariable String userId) {
        UUID userUuid = UUID.fromString(userId);
        ADMProfile profile = admProfileService.findByUserId(userUuid);
        return ResponseEntity.ok(mapToResponse(profile));
    }

    @GetMapping("/region/{region}")
    @Operation(summary = "Buscar ADMs por região")
    public ResponseEntity<?> getByRegion(@PathVariable String region) {
        var adms = admProfileService.findByRegion(region);
        return ResponseEntity.ok(adms.stream().map(this::mapToResponse).toList());
    }

    @PutMapping("/commission")
    @Operation(summary = "Atualizar percentual de comissão do ADM autenticado")
    public ResponseEntity<ADMProfileResponse> updateCommission(
            @RequestParam BigDecimal percentage,
            Authentication authentication) {

        UUID userId = UUID.fromString(authentication.getName());
        ADMProfile updated = admProfileService.updateCommission(userId, percentage);
        return ResponseEntity.ok(mapToResponse(updated));
    }

    @PutMapping("/status")
    @Operation(summary = "Atualizar status do ADM autenticado")
    public ResponseEntity<ADMProfileResponse> updateStatus(
            @RequestParam String status,
            Authentication authentication) {

        UUID userId = UUID.fromString(authentication.getName());
        ADMProfile.ADMStatus newStatus = ADMProfile.ADMStatus.valueOf(status);
        ADMProfile updated = admProfileService.updateStatus(userId, newStatus);
        return ResponseEntity.ok(mapToResponse(updated));
    }

    @PostMapping("/link-partnership")
    @Operation(summary = "Vincular ADM autenticado a uma parceria municipal")
    public ResponseEntity<ADMProfileResponse> linkToPartnership(
            @RequestParam Long partnershipId,
            Authentication authentication) {

        UUID userId = UUID.fromString(authentication.getName());
        ADMProfile updated = admProfileService.linkToPartnership(userId, partnershipId);
        return ResponseEntity.ok(mapToResponse(updated));
    }

    private ADMProfileResponse mapToResponse(ADMProfile profile) {
        return ADMProfileResponse.builder()
                .id(profile.getId())
                .createdAt(profile.getCreatedAt())
                .userId(profile.getUser().getId().toString())
                .userName(profile.getUser().getName())
                .userPhone(profile.getUser().getPhone())
                .region(profile.getRegion())
                .commissionPercentage(profile.getCommissionPercentage())
                .totalCommission(profile.getTotalCommission()) // Campo correto
                .totalDeliveriesManaged(profile.getTotalDeliveriesManaged())
                .activeDeliveriesCount(0) // Campo calculado - adicionar lógica se necessário
                .status(profile.getStatus().name())
                .partnershipId(profile.getPartnership() != null ? profile.getPartnership().getId() : null)
                .partnershipName(profile.getPartnership() != null ? profile.getPartnership().getName() : null)
                .partnershipCity(profile.getPartnership() != null ? profile.getPartnership().getCity() : null)
                .build();
    }
}
