package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.jpa.FoodOrder;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.repository.FoodOrderRepository;
import com.mvt.mvt_events.service.FoodOrderPaymentService;
import com.mvt.mvt_events.service.FoodOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final FoodOrderRepository orderRepository;
    private final FoodOrderPaymentService paymentService;

    public FoodOrderController(FoodOrderService orderService, FoodOrderRepository orderRepository,
                               FoodOrderPaymentService paymentService) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.paymentService = paymentService;
    }

    // ================================================================
    // GENÉRICO — EntityCRUD usa GET paginado
    // ================================================================

    @GetMapping
    @Operation(summary = "Listar pedidos (paginado)", description = "Para EntityCRUD genérico. CLIENT vê seus pedidos, ADMIN vê todos. Suporta filtros: status, orderType, tableNumberField.")
    public ResponseEntity<Page<FoodOrder>> listPaged(
            Authentication authentication,
            Pageable pageable,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String orderType,
            @RequestParam(required = false) Integer tableNumberField) {
        // Força ordenação por createdAt DESC se nenhum sort foi especificado
        if (pageable.getSort().isUnsorted()) {
            pageable = org.springframework.data.domain.PageRequest.of(
                pageable.getPageNumber(), pageable.getPageSize(),
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        }
        User user = (User) authentication.getPrincipal();

        // Converte strings para enums
        FoodOrder.OrderStatus statusEnum = null;
        if (status != null && !status.isBlank()) {
            try { statusEnum = FoodOrder.OrderStatus.valueOf(status); } catch (Exception ignored) {}
        }
        FoodOrder.OrderType orderTypeEnum = null;
        if (orderType != null && !orderType.isBlank()) {
            try { orderTypeEnum = FoodOrder.OrderType.valueOf(orderType); } catch (Exception ignored) {}
        }

        UUID clientId = user.getRole() == User.Role.ADMIN ? null : user.getId();
        return ResponseEntity.ok(orderRepository.findWithFilters(clientId, statusEnum, orderTypeEnum, tableNumberField, pageable));
    }

    // ================================================================
    // CUSTOMER — criar pedido e consultar
    // ================================================================

    @PostMapping
    @Operation(summary = "Criar pedido", description = "CUSTOMER cria pedido para um restaurante. Fase 1: paymentTiming=AT_CHECKOUT gera QR PIX imediato; ON_DELIVERY só marca intenção.")
    public ResponseEntity<FoodOrder> create(Authentication authentication, @RequestBody CreateOrderRequest request) {
        User user = (User) authentication.getPrincipal();
        Double deliveryLat = request.deliveryAddress != null ? request.deliveryAddress.latitude : null;
        Double deliveryLng = request.deliveryAddress != null ? request.deliveryAddress.longitude : null;
        String deliveryAddr = request.deliveryAddress != null ? request.deliveryAddress.address : null;
        FoodOrder order = orderService.create(user.getId(), request.clientId, request.items, request.notes, deliveryAddr, deliveryLat, deliveryLng);

        // Pagamento (fase 1: apenas PIX)
        FoodOrder.PaymentTiming timing = parseTiming(request.paymentTiming);
        if (timing == FoodOrder.PaymentTiming.AT_CHECKOUT) {
            order = paymentService.createPixForCheckout(order);
        } else if (timing == FoodOrder.PaymentTiming.ON_DELIVERY) {
            order = paymentService.markPayOnDelivery(order);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    private FoodOrder.PaymentTiming parseTiming(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try { return FoodOrder.PaymentTiming.valueOf(raw.toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
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
    // WAITER — pedidos de mesa
    // ================================================================

    @GetMapping("/waiter/my-orders")
    @Operation(summary = "Pedidos ativos do garçom", description = "WAITER: pedidos ativos no estabelecimento")
    public ResponseEntity<List<FoodOrder>> waiterActiveOrders(
            Authentication authentication,
            @RequestParam UUID clientId) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(orderService.findActiveByWaiter(user.getId(), clientId));
    }

    @GetMapping("/tables-status")
    @Operation(summary = "Status dos pedidos por mesa", description = "Retorna mapa tableId → status do pedido ativo para todas as mesas de um client")
    public ResponseEntity<java.util.Map<Long, String>> tablesOrderStatus(@RequestParam java.util.UUID clientId) {
        java.util.Map<Long, String> result = orderService.getTablesOrderStatus(clientId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/by-table/{tableId}")
    @Operation(summary = "Pedidos de uma mesa", description = "Pedidos ativos de uma mesa específica")
    public ResponseEntity<List<FoodOrder>> ordersByTable(
            @PathVariable Long tableId,
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        if (activeOnly) {
            return ResponseEntity.ok(orderService.findActiveByTable(tableId));
        }
        return ResponseEntity.ok(orderService.findByTable(tableId));
    }

    @PostMapping("/table")
    @Operation(summary = "Criar pedido de mesa", description = "WAITER cria pedido vinculado a uma mesa")
    public ResponseEntity<FoodOrder> createTableOrder(Authentication authentication,
                                                       @RequestBody CreateTableOrderRequest request) {
        User user = (User) authentication.getPrincipal();
        if (user.getRole() != User.Role.WAITER && user.getRole() != User.Role.CLIENT && user.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("Apenas garçons, estabelecimentos ou admins podem criar pedidos de mesa");
        }
        FoodOrder order = orderService.createTableOrder(
                user.getId(), request.clientId, request.tableId, request.items, request.notes);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @PostMapping("/{orderId}/add-items")
    @Operation(summary = "Adicionar itens a pedido existente", description = "WAITER adiciona nova rodada de itens a um pedido de mesa aberto")
    public ResponseEntity<?> addItemsToOrder(
            @PathVariable Long orderId,
            @RequestBody AddItemsRequest request,
            Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            FoodOrder order = orderService.addItemsToOrder(orderId, user.getId(), request.items);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{orderId}/items/{itemId}")
    @Operation(summary = "Remover item do pedido", description = "WAITER remove um item do pedido (qualquer rodada)")
    public ResponseEntity<?> removeItemFromOrder(
            @PathVariable Long orderId,
            @PathVariable Long itemId,
            Authentication authentication) {
        try {
            FoodOrder order = orderService.removeItemFromOrder(orderId, itemId);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    public static class AddItemsRequest {
        public List<FoodOrderService.OrderItemRequest> items;
    }

    @PatchMapping("/{orderId}/close-table")
    @Operation(summary = "Fechar conta da mesa", description = "WAITER fecha a conta: marca COMPLETED + registra forma de pagamento")
    public ResponseEntity<?> closeTableOrder(
            @PathVariable Long orderId,
            @RequestBody CloseTableRequest request,
            Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            FoodOrder order = orderService.closeTableOrder(orderId, user.getId(), request.paymentMethod);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    public static class CloseTableRequest {
        public String paymentMethod; // CASH, PIX, CREDIT_CARD, DEBIT_CARD
    }

    @PatchMapping("/{orderId}/confirm-payment")
    @Operation(summary = "Confirmar pagamento", description = "Confirma pagamento: AWAITING_PAYMENT → COMPLETED")
    public ResponseEntity<?> confirmPayment(
            @PathVariable Long orderId,
            @RequestBody(required = false) CloseTableRequest request) {
        try {
            String pm = request != null ? request.paymentMethod : null;
            FoodOrder order = orderService.confirmPayment(orderId, pm);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    // ================================================================
    // COMANDAS (split de conta)
    // ================================================================

    @GetMapping("/{orderId}/commands")
    @Operation(summary = "Listar comandas do pedido")
    public ResponseEntity<?> listCommands(@PathVariable Long orderId) {
        try {
            return ResponseEntity.ok(orderService.listCommands(orderId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{orderId}/commands")
    @Operation(summary = "Criar nova comanda", description = "Cria comanda (opcionalmente com nome) no pedido de mesa")
    public ResponseEntity<?> createCommand(@PathVariable Long orderId, @RequestBody(required = false) CommandRequest req) {
        try {
            String name = req != null ? req.name : null;
            return ResponseEntity.ok(orderService.createCommand(orderId, name));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{orderId}/commands/{commandId}")
    @Operation(summary = "Renomear comanda")
    public ResponseEntity<?> renameCommand(@PathVariable Long orderId, @PathVariable Long commandId,
                                           @RequestBody CommandRequest req) {
        try {
            return ResponseEntity.ok(orderService.renameCommand(orderId, commandId, req.name));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{orderId}/commands/{commandId}")
    @Operation(summary = "Remover comanda", description = "Só permite se a comanda não tem itens")
    public ResponseEntity<?> deleteCommand(@PathVariable Long orderId, @PathVariable Long commandId) {
        try {
            orderService.deleteCommand(orderId, commandId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{orderId}/bill-breakdown")
    @Operation(summary = "Breakdown da conta por comanda")
    public ResponseEntity<?> getBillBreakdown(@PathVariable Long orderId) {
        try {
            return ResponseEntity.ok(orderService.getBillBreakdown(orderId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{orderId}/items/{itemId}/command")
    @Operation(summary = "Mover item entre comandas", description = "commandId null = Mesa (compartilhado)")
    public ResponseEntity<?> moveItemToCommand(@PathVariable Long orderId, @PathVariable Long itemId,
                                               @RequestBody MoveItemRequest req) {
        try {
            FoodOrder order = orderService.moveItemToCommand(orderId, itemId, req.commandId);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{orderId}/items/{itemId}/packaged")
    @Operation(summary = "Marcar/desmarcar item como empacotado pra viagem")
    public ResponseEntity<?> setItemPackaged(@PathVariable Long orderId, @PathVariable Long itemId,
                                             @RequestBody PackagedRequest req) {
        try {
            orderService.setItemPackaged(orderId, itemId, req.packaged);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    public static class PackagedRequest {
        public boolean packaged;
    }

    @PatchMapping("/{orderId}/close-partial")
    @Operation(summary = "Fechar comanda ou Mesa parcialmente",
               description = "commandId null = fechar Mesa (itens compartilhados); se todas pagas, auto-COMPLETED")
    public ResponseEntity<?> closePartial(@PathVariable Long orderId, @RequestBody ClosePartialRequest req) {
        try {
            FoodOrder order = orderService.closePartial(orderId, req.commandId, req.paymentMethod);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{orderId}/auto-complete")
    @Operation(summary = "Tentar completar pedido automaticamente",
               description = "Marca COMPLETED se tudo com valor já foi pago; comandas vazias não bloqueiam")
    public ResponseEntity<?> autoComplete(@PathVariable Long orderId) {
        try {
            FoodOrder order = orderService.autoComplete(orderId);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    public static class CommandRequest {
        public String name;
    }

    public static class MoveItemRequest {
        public Long commandId; // null = Mesa (compartilhado)
    }

    public static class ClosePartialRequest {
        public Long commandId; // null = Mesa
        public String paymentMethod; // CASH, PIX, CREDIT_CARD, DEBIT_CARD, NOT_INFORMED
    }

    // ================================================================
    // REQUEST DTO
    // ================================================================

    public static class CreateOrderRequest {
        public UUID clientId;
        public List<FoodOrderService.OrderItemRequest> items;
        public String notes;
        public DeliveryAddress deliveryAddress;
        /** Fase 1 Zapi-Food: AT_CHECKOUT (gera PIX agora) ou ON_DELIVERY (paga na entrega). null = sem pagamento. */
        public String paymentTiming;
    }

    public static class CreateTableOrderRequest {
        public UUID clientId;
        public Long tableId;
        public List<FoodOrderService.OrderItemRequest> items;
        public String notes;
    }

    public static class DeliveryAddress {
        public String address;
        public Double latitude;
        public Double longitude;
    }
}
