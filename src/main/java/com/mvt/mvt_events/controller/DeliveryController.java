package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.dto.*;
import com.mvt.mvt_events.jpa.Delivery;
import com.mvt.mvt_events.service.DeliveryService;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Controller REST para Delivery - ENTIDADE CORE DO ZAPI10
 * Todas as operações filtram por ADM (tenant)
 */
@RestController
@RequestMapping("/api/zapi10/deliveries")
@CrossOrigin(origins = "*")
@Tag(name = "Zapi10 - Deliveries", description = "Gerenciamento de entregas")
@SecurityRequirement(name = "bearerAuth")
public class DeliveryController {

    @Autowired
    private DeliveryService deliveryService;

    @PostMapping
    @Operation(summary = "Criar nova delivery", description = "Requer role ADM. A delivery é criada com status PENDING.")
    public ResponseEntity<DeliveryResponse> create(
            @RequestBody @Valid DeliveryCreateRequest request,
            Authentication authentication) {

        // Obter ADM ID do usuário autenticado (simplificado - deve vir do
        // token/session)
        UUID admId = UUID.fromString(authentication.getName());

        Delivery delivery = mapToEntity(request);
        Delivery created = deliveryService.create(delivery, admId);

        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(created));
    }

    @GetMapping
    @Operation(summary = "Listar deliveries com filtros", description = "Filtra automaticamente por ADM (tenant)")
    public Page<DeliveryResponse> list(
            @RequestParam(required = false) String clientId,
            @RequestParam(required = false) String courierId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Pageable pageable,
            Authentication authentication) {

        UUID admId = UUID.fromString(authentication.getName());

        UUID clientUuid = clientId != null ? UUID.fromString(clientId) : null;
        UUID courierUuid = courierId != null ? UUID.fromString(courierId) : null;
        Delivery.DeliveryStatus deliveryStatus = status != null ? Delivery.DeliveryStatus.valueOf(status) : null;
        LocalDateTime start = startDate != null ? LocalDateTime.parse(startDate) : null;
        LocalDateTime end = endDate != null ? LocalDateTime.parse(endDate) : null;

        Page<Delivery> deliveries = deliveryService.findAll(
                admId, clientUuid, courierUuid, deliveryStatus, start, end, pageable);

        return deliveries.map(this::mapToResponse);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar delivery por ID", description = "Valida acesso por tenant")
    public ResponseEntity<DeliveryResponse> getById(
            @PathVariable Long id,
            Authentication authentication) {

        UUID admId = UUID.fromString(authentication.getName());
        Delivery delivery = deliveryService.findById(id, admId);

        return ResponseEntity.ok(mapToResponse(delivery));
    }

    @PostMapping("/{id}/assign")
    @Operation(summary = "Atribuir delivery a courier", description = "Status: PENDING → ACCEPTED")
    public ResponseEntity<DeliveryResponse> assign(
            @PathVariable Long id,
            @RequestBody @Valid DeliveryAssignRequest request,
            Authentication authentication) {

        UUID admId = UUID.fromString(authentication.getName());
        UUID courierId = UUID.fromString(request.getCourierId());

        Delivery delivery = deliveryService.assignToCourier(id, courierId, admId);

        return ResponseEntity.ok(mapToResponse(delivery));
    }

    @PostMapping("/{id}/pickup")
    @Operation(summary = "Confirmar coleta", description = "Courier confirma que coletou o item. Status: ACCEPTED → PICKED_UP")
    public ResponseEntity<DeliveryResponse> confirmPickup(
            @PathVariable Long id,
            Authentication authentication) {

        UUID courierId = UUID.fromString(authentication.getName());
        Delivery delivery = deliveryService.confirmPickup(id, courierId);

        return ResponseEntity.ok(mapToResponse(delivery));
    }

    @PostMapping("/{id}/transit")
    @Operation(summary = "Iniciar transporte", description = "Courier inicia o transporte. Status: PICKED_UP → IN_TRANSIT")
    public ResponseEntity<DeliveryResponse> startTransit(
            @PathVariable Long id,
            Authentication authentication) {

        UUID courierId = UUID.fromString(authentication.getName());
        Delivery delivery = deliveryService.startTransit(id, courierId);

        return ResponseEntity.ok(mapToResponse(delivery));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Completar delivery", description = "Courier confirma entrega. Status: IN_TRANSIT → COMPLETED")
    public ResponseEntity<DeliveryResponse> complete(
            @PathVariable Long id,
            Authentication authentication) {

        UUID courierId = UUID.fromString(authentication.getName());
        Delivery delivery = deliveryService.complete(id, courierId);

        return ResponseEntity.ok(mapToResponse(delivery));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancelar delivery", description = "ADM cancela a delivery")
    public ResponseEntity<DeliveryResponse> cancel(
            @PathVariable Long id,
            @RequestParam String reason,
            Authentication authentication) {

        UUID admId = UUID.fromString(authentication.getName());
        Delivery delivery = deliveryService.cancel(id, admId, reason);

        return ResponseEntity.ok(mapToResponse(delivery));
    }

    @GetMapping("/pending")
    @Operation(summary = "Listar deliveries pendentes de atribuição")
    public ResponseEntity<?> listPending(Authentication authentication) {
        UUID admId = UUID.fromString(authentication.getName());
        var deliveries = deliveryService.findPendingAssignment(admId);
        return ResponseEntity.ok(deliveries.stream().map(this::mapToResponse).toList());
    }

    @GetMapping("/courier/active")
    @Operation(summary = "Listar deliveries ativas do courier autenticado")
    public ResponseEntity<?> listCourierActive(Authentication authentication) {
        UUID courierId = UUID.fromString(authentication.getName());
        var deliveries = deliveryService.findActiveByCourier(courierId);
        return ResponseEntity.ok(deliveries.stream().map(this::mapToResponse).toList());
    }

    // ========================================================================
    // MAPPERS
    // ========================================================================

    private Delivery mapToEntity(DeliveryCreateRequest request) {
        Delivery delivery = new Delivery();
        // Mapear campos básicos
        delivery.setFromAddress(request.getFromAddress());
        delivery.setFromLatitude(request.getFromLatitude());
        delivery.setFromLongitude(request.getFromLongitude());

        delivery.setToAddress(request.getToAddress());
        delivery.setToLatitude(request.getToLatitude());
        delivery.setToLongitude(request.getToLongitude());

        delivery.setRecipientName(request.getRecipientName());
        delivery.setRecipientPhone(request.getRecipientPhone());
        // Nota: campo notes não existe na entidade Delivery

        // Client e Partnership serão setados no service
        return delivery;
    }

    private DeliveryResponse mapToResponse(Delivery delivery) {
        return DeliveryResponse.builder()
                .id(delivery.getId())
                .createdAt(delivery.getCreatedAt())
                .clientId(delivery.getClient() != null ? delivery.getClient().getId().toString() : null)
                .clientName(delivery.getClient() != null ? delivery.getClient().getName() : null)
                .courierId(delivery.getCourier() != null ? delivery.getCourier().getId().toString() : null)
                .courierName(delivery.getCourier() != null ? delivery.getCourier().getName() : null)
                .courierPhone(delivery.getCourier() != null ? delivery.getCourier().getPhone() : null)
                .admId(delivery.getAdm() != null ? delivery.getAdm().getId().toString() : null)
                .admName(delivery.getAdm() != null ? delivery.getAdm().getName() : null)
                .fromAddress(delivery.getFromAddress())
                .fromLatitude(delivery.getFromLatitude())
                .fromLongitude(delivery.getFromLongitude())
                .toAddress(delivery.getToAddress())
                .toLatitude(delivery.getToLatitude())
                .toLongitude(delivery.getToLongitude())
                .recipientName(delivery.getRecipientName())
                .recipientPhone(delivery.getRecipientPhone())
                .totalAmount(delivery.getTotalAmount())
                .status(delivery.getStatus().name())
                .pickedUpAt(delivery.getPickedUpAt())
                .completedAt(delivery.getCompletedAt())
                .partnershipId(delivery.getPartnership() != null ? delivery.getPartnership().getId() : null)
                .partnershipName(delivery.getPartnership() != null ? delivery.getPartnership().getName() : null)
                .build();
    }
}
