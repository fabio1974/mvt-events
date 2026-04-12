package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.jpa.FoodOrder;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.service.FoodOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Food Orders", description = "Pedidos — Zapi-Food")
public class FoodOrderController {

    private final FoodOrderService orderService;

    public FoodOrderController(FoodOrderService orderService) {
        this.orderService = orderService;
    }

    // ================================================================
    // CUSTOMER — criar pedido e consultar
    // ================================================================

    @PostMapping
    @Operation(summary = "Criar pedido", description = "CUSTOMER cria pedido para um restaurante")
    public ResponseEntity<FoodOrder> create(Authentication authentication, @RequestBody CreateOrderRequest request) {
        User user = (User) authentication.getPrincipal();
        FoodOrder order = orderService.create(user.getId(), request.clientId, request.items, request.notes);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @GetMapping("/my")
    @Operation(summary = "Meus pedidos", description = "CUSTOMER: histórico de pedidos")
    public ResponseEntity<List<FoodOrder>> myOrders(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(orderService.findByCustomer(user.getId()));
    }

    // ================================================================
    // CLIENT (RESTAURANTE) — gerenciar pedidos
    // ================================================================

    @GetMapping("/restaurant")
    @Operation(summary = "Pedidos do meu restaurante", description = "CLIENT: todos os pedidos")
    public ResponseEntity<List<FoodOrder>> restaurantOrders(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(orderService.findByClient(user.getId()));
    }

    @GetMapping("/restaurant/active")
    @Operation(summary = "Pedidos ativos do restaurante", description = "CLIENT: pedidos em andamento")
    public ResponseEntity<List<FoodOrder>> restaurantActiveOrders(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(orderService.findActiveByClient(user.getId()));
    }

    @PatchMapping("/{id}/accept")
    @Operation(summary = "Aceitar pedido", description = "CLIENT: aceita pedido recebido")
    public ResponseEntity<FoodOrder> accept(Authentication authentication, @PathVariable Long id) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(orderService.accept(id, user.getId()));
    }

    @PatchMapping("/{id}/preparing")
    @Operation(summary = "Iniciar preparo", description = "CLIENT: pedido está sendo preparado")
    public ResponseEntity<FoodOrder> preparing(Authentication authentication, @PathVariable Long id) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(orderService.startPreparing(id, user.getId()));
    }

    @PatchMapping("/{id}/ready")
    @Operation(summary = "Marcar pronto", description = "CLIENT: pedido pronto para retirada. Cria Delivery automaticamente.")
    public ResponseEntity<FoodOrder> ready(Authentication authentication, @PathVariable Long id) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(orderService.markReady(id, user.getId()));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancelar pedido", description = "CLIENT ou CUSTOMER pode cancelar")
    public ResponseEntity<FoodOrder> cancel(Authentication authentication, @PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        User user = (User) authentication.getPrincipal();
        String reason = body != null ? body.getOrDefault("reason", "Cancelado pelo usuário") : "Cancelado pelo usuário";
        return ResponseEntity.ok(orderService.cancel(id, user.getId(), reason));
    }

    // ================================================================
    // GERAL
    // ================================================================

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe do pedido")
    public ResponseEntity<FoodOrder> getById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.findById(id));
    }

    // ================================================================
    // REQUEST DTO
    // ================================================================

    public static class CreateOrderRequest {
        public UUID clientId;
        public List<FoodOrderService.OrderItemRequest> items;
        public String notes;
    }
}
