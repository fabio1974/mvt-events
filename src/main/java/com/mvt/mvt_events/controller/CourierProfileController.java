package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.dto.*;
import com.mvt.mvt_events.jpa.CourierProfile;
import com.mvt.mvt_events.service.CourierProfileService;
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
 * Controller REST para CourierProfile
 */
@RestController
@RequestMapping("/api/zapi10/couriers")
@CrossOrigin(origins = "*")
@Tag(name = "Zapi10 - Couriers", description = "Gerenciamento de entregadores")
@SecurityRequirement(name = "bearerAuth")
public class CourierProfileController {

    @Autowired
    private CourierProfileService courierProfileService;

    @PostMapping
    @Operation(summary = "Criar perfil de courier")
    public ResponseEntity<CourierProfileResponse> create(
            @RequestBody @Valid CourierProfileCreateRequest request) {

        CourierProfile profile = new CourierProfile();
        profile.setVehicleType(CourierProfile.VehicleType.valueOf(request.getVehicleType()));
        profile.setVehiclePlate(request.getVehiclePlate());
        profile.setVehicleModel(request.getVehicleModel());
        profile.setVehicleColor(request.getVehicleYear() != null ? request.getVehicleYear().toString() : null);

        UUID userId = UUID.fromString(request.getUserId());
        CourierProfile created = courierProfileService.create(profile, userId);

        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(created));
    }

    @GetMapping
    @Operation(summary = "Listar couriers com filtros")
    public Page<CourierProfileResponse> list(
            @RequestParam(required = false) String admId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) BigDecimal minRating,
            @RequestParam(required = false) String search,
            Pageable pageable,
            Authentication authentication) {

        UUID admUuid = admId != null ? UUID.fromString(admId) : UUID.fromString(authentication.getName());
        CourierProfile.CourierStatus courierStatus = status != null ? CourierProfile.CourierStatus.valueOf(status)
                : null;

        Page<CourierProfile> couriers = courierProfileService.findAll(
                admUuid, courierStatus, minRating, search, pageable);

        return couriers.map(this::mapToResponse);
    }

    @GetMapping("/me")
    @Operation(summary = "Buscar perfil do courier autenticado")
    public ResponseEntity<CourierProfileResponse> getMyProfile(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        CourierProfile profile = courierProfileService.findByUserId(userId);
        return ResponseEntity.ok(mapToResponse(profile));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Buscar perfil de courier por user ID")
    public ResponseEntity<CourierProfileResponse> getByUserId(@PathVariable String userId) {
        UUID userUuid = UUID.fromString(userId);
        CourierProfile profile = courierProfileService.findByUserId(userUuid);
        return ResponseEntity.ok(mapToResponse(profile));
    }

    @PostMapping("/link-adm")
    @Operation(summary = "Vincular courier a um ADM")
    public ResponseEntity<?> linkToADM(
            @RequestParam String courierId,
            @RequestParam String admId,
            @RequestParam(defaultValue = "false") boolean isPrimary) {

        UUID courierUuid = UUID.fromString(courierId);
        UUID admUuid = UUID.fromString(admId);

        courierProfileService.linkToADM(courierUuid, admUuid, isPrimary);

        return ResponseEntity.ok().body("Courier vinculado ao ADM com sucesso");
    }

    @PutMapping("/status")
    @Operation(summary = "Atualizar status do courier autenticado")
    public ResponseEntity<CourierProfileResponse> updateStatus(
            @RequestParam String status,
            Authentication authentication) {

        UUID userId = UUID.fromString(authentication.getName());
        CourierProfile.CourierStatus newStatus = CourierProfile.CourierStatus.valueOf(status);

        CourierProfile updated = courierProfileService.updateStatus(userId, newStatus);

        return ResponseEntity.ok(mapToResponse(updated));
    }

    @GetMapping("/nearby")
    @Operation(summary = "Buscar couriers disponíveis próximos")
    public ResponseEntity<?> findNearby(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "10.0") Double radiusKm) {

        var couriers = courierProfileService.findAvailableNearby(latitude, longitude, radiusKm);

        return ResponseEntity.ok(couriers.stream().map(this::mapToResponse).toList());
    }

    private CourierProfileResponse mapToResponse(CourierProfile profile) {
        return CourierProfileResponse.builder()
                .id(profile.getId())
                .createdAt(profile.getCreatedAt())
                .userId(profile.getUser().getId().toString())
                .userName(profile.getUser().getName())
                .userEmail(profile.getUser().getUsername())
                .userPhone(profile.getUser().getPhone())
                .vehicleType(profile.getVehicleType().name())
                .vehiclePlate(profile.getVehiclePlate())
                .vehicleModel(profile.getVehicleModel())
                .vehicleYear(profile.getVehicleColor() != null
                        ? (profile.getVehicleColor().matches("\\d+") ? Integer.parseInt(profile.getVehicleColor())
                                : null)
                        : null)
                .rating(profile.getRating())
                .totalDeliveries(profile.getTotalDeliveries())
                .completedDeliveries(profile.getCompletedDeliveries())
                .cancelledDeliveries(profile.getCancelledDeliveries())
                .status(profile.getStatus().name())
                .lastLocationUpdate(profile.getLastLocationUpdate())
                .build();
    }
}
