package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.jpa.FoodOrder;
import com.mvt.mvt_events.jpa.RestaurantTable;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.repository.FoodOrderRepository;
import com.mvt.mvt_events.service.RestaurantTableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/tables")
@Tag(name = "Mesas", description = "Gerenciamento de mesas do estabelecimento")
public class RestaurantTableController {

    private final RestaurantTableService tableService;
    private final FoodOrderRepository foodOrderRepository;

    public RestaurantTableController(RestaurantTableService tableService, FoodOrderRepository foodOrderRepository) {
        this.tableService = tableService;
        this.foodOrderRepository = foodOrderRepository;
    }

    @GetMapping
    @Operation(summary = "Listar mesas do estabelecimento")
    public List<RestaurantTable> list(
            @RequestParam(required = false) UUID clientId,
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly,
            Authentication authentication) {
        UUID resolvedClientId = resolveClientId(clientId, authentication);
        return activeOnly
                ? tableService.findActiveByClient(resolvedClientId)
                : tableService.findByClient(resolvedClientId);
    }

    @PostMapping
    @Operation(summary = "Criar mesa")
    public ResponseEntity<RestaurantTable> create(
            @RequestBody CreateTableRequest request,
            Authentication authentication) {
        UUID clientId = resolveClientId(request.clientId, authentication);
        RestaurantTable table = tableService.create(clientId, request.number, request.seats);
        return ResponseEntity.status(HttpStatus.CREATED).body(table);
    }

    @PostMapping("/batch")
    @Operation(summary = "Criar mesas em lote (de N a M)")
    public ResponseEntity<List<RestaurantTable>> createBatch(
            @RequestBody CreateBatchRequest request,
            Authentication authentication) {
        UUID clientId = resolveClientId(request.clientId, authentication);
        List<RestaurantTable> tables = tableService.createBatch(clientId, request.from, request.to, request.seats);
        return ResponseEntity.status(HttpStatus.CREATED).body(tables);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar mesa")
    public RestaurantTable update(
            @PathVariable Long id,
            @RequestBody UpdateTableRequest request,
            Authentication authentication) {
        UUID clientId = resolveClientId(null, authentication);
        return tableService.update(id, clientId, request.seats, request.active, request.status);
    }

    @GetMapping("/order-status")
    @Operation(summary = "Status dos pedidos ativos por mesa")
    public Map<Long, Map<String, Object>> orderStatusByTable(Authentication authentication) {
        UUID clientId = resolveClientId(null, authentication);
        List<FoodOrder> activeOrders = foodOrderRepository.findActiveTableOrders(clientId);
        Map<Long, Map<String, Object>> result = new HashMap<>();
        for (FoodOrder order : activeOrders) {
            if (order.getTable() != null) {
                result.putIfAbsent(order.getTable().getId(), Map.of(
                    "status", order.getStatus().name(),
                    "orderId", order.getId()
                ));
            }
        }
        return result;
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Alterar status da mesa", description = "WAITER/CLIENT: trocar status (AVAILABLE, RESERVED, OCCUPIED, UNAVAILABLE)")
    public RestaurantTable changeStatus(
            @PathVariable Long id,
            @RequestBody ChangeStatusRequest request,
            Authentication authentication) {
        // Busca o clientId da própria mesa (WAITER não precisa informar)
        RestaurantTable table = tableService.findById(id);
        UUID clientId = table.getClient().getId();
        return tableService.changeStatus(id, clientId, request.status);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover mesa")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            Authentication authentication) {
        UUID clientId = resolveClientId(null, authentication);
        tableService.delete(id, clientId);
        return ResponseEntity.noContent().build();
    }

    private UUID resolveClientId(UUID clientId, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        if (user.getRole() == User.Role.CLIENT) {
            return user.getId(); // CLIENT sempre usa seu próprio ID
        }
        if (user.getRole() == User.Role.ADMIN && clientId != null) {
            return clientId;
        }
        if (user.getRole() == User.Role.WAITER && clientId != null) {
            return clientId; // WAITER precisa informar qual estabelecimento
        }
        throw new RuntimeException("clientId é obrigatório para esta role");
    }

    @Data
    public static class CreateTableRequest {
        private UUID clientId;
        private Integer number;
        private Integer seats;
    }

    @Data
    public static class CreateBatchRequest {
        private UUID clientId;
        private int from;
        private int to;
        private Integer seats;
    }

    @Data
    public static class UpdateTableRequest {
        private Integer seats;
        private Boolean active;
        private String status;
    }

    @Data
    public static class ChangeStatusRequest {
        private String status; // AVAILABLE, RESERVED, OCCUPIED, UNAVAILABLE
    }
}
