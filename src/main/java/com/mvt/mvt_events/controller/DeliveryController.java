package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.common.JwtUtil;
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
import java.util.List;
import java.util.UUID;

/**
 * Controller REST para Delivery - ENTIDADE CORE DO ZAPI10
 * Todas as operações filtram por ADM (tenant)
 */
@RestController
@RequestMapping("/api/deliveries")
@CrossOrigin(origins = "*")
@Tag(name = "Deliveries", description = "Gerenciamento de entregas")
@SecurityRequirement(name = "bearerAuth")
public class DeliveryController {

    @Autowired
    private DeliveryService deliveryService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private com.mvt.mvt_events.repository.UserRepository userRepository;

    @Autowired
    private com.mvt.mvt_events.repository.OrganizationRepository organizationRepository;

    @Autowired
    private com.mvt.mvt_events.repository.EmploymentContractRepository employmentContractRepository;

    @PostMapping
    @Operation(summary = "Criar nova delivery", description = "Requer autenticação. A delivery é criada com status PENDING.")
    public ResponseEntity<DeliveryResponse> create(
            @RequestBody @Valid DeliveryCreateRequest request,
            Authentication authentication) {

        UUID creatorId = getUserIdFromAuthentication(authentication);
        UUID clientId = UUID.fromString(request.getClient().getId());

        Delivery delivery = mapToEntity(request);
        Delivery created = deliveryService.create(delivery, creatorId, clientId);

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
            Authentication authentication,
            jakarta.servlet.http.HttpServletRequest request) {

        // Extrair dados do token JWT
        String token = extractTokenFromRequest(request);
        String role = jwtUtil.getRoleFromToken(token);

        UUID clientUuid = clientId != null ? UUID.fromString(clientId) : null;
        UUID courierUuid = courierId != null ? UUID.fromString(courierId) : null;

        Delivery.DeliveryStatus deliveryStatus = null;
        if (status != null) {
            try {
                deliveryStatus = Delivery.DeliveryStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Status inválido: " + status + ". Valores válidos: " +
                        java.util.Arrays.toString(Delivery.DeliveryStatus.values()));
            }
        }

        LocalDateTime start = startDate != null ? LocalDateTime.parse(startDate) : null;
        LocalDateTime end = endDate != null ? LocalDateTime.parse(endDate) : null;

        Page<Delivery> deliveries;

        if ("COURIER".equals(role)) {
            // Para COURIERs: buscar deliveries das organizações onde ele trabalha
            UUID courierUserId = UUID.fromString(jwtUtil.getUserIdFromToken(token));
            deliveries = findDeliveriesForCourier(courierUserId, clientUuid, courierUuid,
                    deliveryStatus, start, end, pageable);
        } else {
            // Para outros roles: usar organizationId do token
            Long organizationId = jwtUtil.getOrganizationIdFromToken(token);
            if (organizationId == null) {
                throw new RuntimeException("Token não contém organizationId");
            }
            deliveries = deliveryService.findAll(organizationId, clientUuid, courierUuid,
                    deliveryStatus, start, end, pageable);
        }

        return deliveries.map(this::mapToResponse);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar delivery por ID", description = "Valida acesso por tenant")
    public ResponseEntity<DeliveryResponse> getById(
            @PathVariable Long id,
            Authentication authentication,
            jakarta.servlet.http.HttpServletRequest request) {

        String token = extractTokenFromRequest(request);
        Long organizationId = jwtUtil.getOrganizationIdFromToken(token);
        if (organizationId == null) {
            throw new RuntimeException("Token não contém organizationId");
        }

        Delivery delivery = deliveryService.findById(id, organizationId);

        return ResponseEntity.ok(mapToResponse(delivery));
    }

    @PostMapping("/{id}/assign")
    @Operation(summary = "Atribuir delivery a courier", description = "Status: PENDING → ACCEPTED")
    public ResponseEntity<DeliveryResponse> assign(
            @PathVariable Long id,
            @RequestBody @Valid DeliveryAssignRequest request,
            Authentication authentication,
            jakarta.servlet.http.HttpServletRequest httpRequest) {

        // Extrair organizationId do token JWT
        String token = extractTokenFromRequest(httpRequest);
        Long organizationId = jwtUtil.getOrganizationIdFromToken(token);
        if (organizationId == null) {
            throw new RuntimeException("Token não contém organizationId");
        }

        UUID courierId = UUID.fromString(request.getCourierId());

        Delivery delivery = deliveryService.assignToCourier(id, courierId, organizationId);

        return ResponseEntity.ok(mapToResponse(delivery));
    }

    @PostMapping("/{id}/pickup")
    @Operation(summary = "Confirmar coleta", description = "Courier confirma que coletou o item. Status: ACCEPTED → PICKED_UP")
    public ResponseEntity<DeliveryResponse> confirmPickup(
            @PathVariable Long id,
            Authentication authentication) {

        UUID courierId = getUserIdFromAuthentication(authentication);
        Delivery delivery = deliveryService.confirmPickup(id, courierId);

        return ResponseEntity.ok(mapToResponse(delivery));
    }

    @PostMapping("/{id}/transit")
    @Operation(summary = "Iniciar transporte", description = "Courier inicia o transporte. Status: PICKED_UP → IN_TRANSIT")
    public ResponseEntity<DeliveryResponse> startTransit(
            @PathVariable Long id,
            Authentication authentication) {

        UUID courierId = getUserIdFromAuthentication(authentication);
        Delivery delivery = deliveryService.startTransit(id, courierId);

        return ResponseEntity.ok(mapToResponse(delivery));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Completar delivery", description = "Courier confirma entrega. Status: IN_TRANSIT → COMPLETED")
    public ResponseEntity<DeliveryResponse> complete(
            @PathVariable Long id,
            Authentication authentication) {

        UUID courierId = getUserIdFromAuthentication(authentication);
        Delivery delivery = deliveryService.complete(id, courierId);

        return ResponseEntity.ok(mapToResponse(delivery));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancelar delivery", description = "ADM cancela a delivery")
    public ResponseEntity<DeliveryResponse> cancel(
            @PathVariable Long id,
            @RequestParam String reason,
            Authentication authentication,
            jakarta.servlet.http.HttpServletRequest httpRequest) {

        // Extrair organizationId do token JWT
        String token = extractTokenFromRequest(httpRequest);
        Long organizationId = jwtUtil.getOrganizationIdFromToken(token);
        if (organizationId == null) {
            throw new RuntimeException("Token não contém organizationId");
        }

        Delivery delivery = deliveryService.cancel(id, organizationId, reason);

        return ResponseEntity.ok(mapToResponse(delivery));
    }

    @GetMapping("/pending")
    @Operation(summary = "Listar deliveries pendentes de atribuição")
    public ResponseEntity<?> listPending(Authentication authentication,
            jakarta.servlet.http.HttpServletRequest httpRequest) {

        // Extrair organizationId do token JWT
        String token = extractTokenFromRequest(httpRequest);
        Long organizationId = jwtUtil.getOrganizationIdFromToken(token);
        if (organizationId == null) {
            throw new RuntimeException("Token não contém organizationId");
        }

        var deliveries = deliveryService.findPendingAssignment(organizationId);
        return ResponseEntity.ok(deliveries.stream().map(this::mapToResponse).toList());
    }

    @GetMapping("/courier/active")
    @Operation(summary = "Listar deliveries ativas do courier autenticado")
    public ResponseEntity<?> listCourierActive(Authentication authentication) {
        UUID courierId = getUserIdFromAuthentication(authentication);
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
        delivery.setTotalAmount(request.getTotalAmount());
        delivery.setItemDescription(request.getItemDescription());

        // Client e Partnership serão setados no service
        return delivery;
    }

    private DeliveryResponse mapToResponse(Delivery delivery) {
        return DeliveryResponse.builder()
                .id(delivery.getId())
                .createdAt(delivery.getCreatedAt())
                // Client (objeto aninhado)
                .client(delivery.getClient() != null ? DeliveryResponse.UserDTO.builder()
                        .id(delivery.getClient().getId().toString())
                        .name(delivery.getClient().getName())
                        .phone(delivery.getClient().getPhone())
                        .build() : null)
                // Courier (objeto aninhado)
                .courier(delivery.getCourier() != null ? DeliveryResponse.UserDTO.builder()
                        .id(delivery.getCourier().getId().toString())
                        .name(delivery.getCourier().getName())
                        .phone(delivery.getCourier().getPhone())
                        .build() : null)
                // Organização vem do cliente agora
                .organization(delivery.getClient() != null && delivery.getClient().getOrganization() != null
                        ? DeliveryResponse.OrganizationDTO.builder()
                                .id(delivery.getClient().getOrganization().getId())
                                .name(delivery.getClient().getOrganization().getName())
                                .build()
                        : null)
                // Addresses
                .fromAddress(delivery.getFromAddress())
                .fromLatitude(delivery.getFromLatitude())
                .fromLongitude(delivery.getFromLongitude())
                .toAddress(delivery.getToAddress())
                .toLatitude(delivery.getToLatitude())
                .toLongitude(delivery.getToLongitude())
                // Recipient
                .recipientName(delivery.getRecipientName())
                .recipientPhone(delivery.getRecipientPhone())
                // Amount & Status
                .totalAmount(delivery.getTotalAmount())
                .status(delivery.getStatus().name())
                .pickedUpAt(delivery.getPickedUpAt())
                .completedAt(delivery.getCompletedAt())
                // Partnership (objeto aninhado)
                .partnership(delivery.getPartnership() != null ? DeliveryResponse.PartnershipDTO.builder()
                        .id(delivery.getPartnership().getId())
                        .name(delivery.getPartnership().getName())
                        .build() : null)
                .build();
    }

    /**
     * Helper para extrair userId do JWT token via email
     */
    private UUID getUserIdFromAuthentication(Authentication authentication) {
        String email = authentication.getName();
        com.mvt.mvt_events.jpa.User user = userRepository.findByUsername(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + email));
        return user.getId();
    }

    /**
     * Helper para extrair organizationId do JWT token
     */
    private UUID getOrganizationIdFromRequest(jakarta.servlet.http.HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            Long organizationId = jwtUtil.getOrganizationIdFromToken(token);
            if (organizationId == null) {
                throw new RuntimeException("Token não contém organizationId");
            }
            // Buscar a organização pelo ID para obter o UUID
            return findOrganizationUuidById(organizationId);
        }
        throw new RuntimeException("Token de autorização não encontrado");
    }

    /**
     * Helper para extrair token do request
     */
    private String extractTokenFromRequest(jakarta.servlet.http.HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new RuntimeException("Token de autorização não encontrado");
    }

    /**
     * Busca deliveries para um COURIER baseado nas organizações onde ele trabalha
     */
    private Page<Delivery> findDeliveriesForCourier(UUID courierUserId, UUID clientId, UUID courierId,
            Delivery.DeliveryStatus status, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {

        // Buscar contratos ativos do courier para saber as organizações
        List<Object[]> contractData = employmentContractRepository.findContractDataByCourierId(courierUserId);

        if (contractData.isEmpty()) {
            // Se courier não tem contratos, retornar página vazia
            return Page.empty(pageable);
        }

        // Extrair apenas contratos ativos
        List<Long> organizationIds = contractData.stream()
                .filter(data -> (Boolean) data[3]) // is_active = true
                .map(data -> (Long) data[0]) // organization_id
                .toList();

        if (organizationIds.isEmpty()) {
            return Page.empty(pageable);
        }

        // Buscar deliveries de todas as organizações do courier
        return deliveryService.findAllByOrganizationIds(organizationIds, clientId, courierId,
                status, startDate, endDate, pageable);
    }

    /**
     * Helper para buscar UUID da organização pelo ID Long
     */
    private UUID findOrganizationUuidById(Long organizationId) {
        try {
            com.mvt.mvt_events.jpa.Organization org = organizationRepository.findById(organizationId)
                    .orElseThrow(() -> new RuntimeException("Organização não encontrada: " + organizationId));
            // Como Organization usa Long ID, não podemos retornar UUID diretamente
            // Vamos retornar null por enquanto e reformular a lógica
            throw new RuntimeException("Incompatibilidade de tipos: Organization usa Long ID, não UUID");
        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar organização: " + e.getMessage());
        }
    }
}
