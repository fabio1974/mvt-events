package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.*;
import com.mvt.mvt_events.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Serviço de pedidos — módulo Zapi-Food.
 * Gerencia o ciclo de vida: PLACED → ACCEPTED → PREPARING → READY → DELIVERING → COMPLETED
 * Quando o restaurante marca READY, cria automaticamente uma Delivery.
 */
@Service
@Transactional
public class FoodOrderService {

    private static final Logger log = LoggerFactory.getLogger(FoodOrderService.class);
    private static final ZoneId ZONE = ZoneId.of("America/Fortaleza");

    private final FoodOrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final StoreProfileRepository storeProfileRepository;
    private final DeliveryService deliveryService;
    private final PushNotificationService pushNotificationService;
    private final SiteConfigurationService siteConfigurationService;

    public FoodOrderService(FoodOrderRepository orderRepository, ProductRepository productRepository,
                            UserRepository userRepository, StoreProfileRepository storeProfileRepository,
                            DeliveryService deliveryService, PushNotificationService pushNotificationService,
                            SiteConfigurationService siteConfigurationService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.storeProfileRepository = storeProfileRepository;
        this.deliveryService = deliveryService;
        this.pushNotificationService = pushNotificationService;
        this.siteConfigurationService = siteConfigurationService;
    }

    // ================================================================
    // CRIAR PEDIDO (CUSTOMER)
    // ================================================================

    public FoodOrder create(UUID customerId, UUID clientId, List<OrderItemRequest> items, String notes) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        if (customer.getRole() != User.Role.CUSTOMER && customer.getRole() != User.Role.CLIENT) {
            throw new RuntimeException("Apenas CUSTOMERs ou CLIENTs podem fazer pedidos");
        }

        User client = userRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Restaurante não encontrado"));
        if (client.getRole() != User.Role.CLIENT) {
            throw new RuntimeException("Destinatário do pedido deve ser um CLIENT (restaurante)");
        }

        // Verificar se loja está aberta
        StoreProfile store = storeProfileRepository.findByUserId(clientId).orElse(null);
        if (store != null && !store.getIsOpen()) {
            throw new RuntimeException("Este restaurante está fechado no momento");
        }

        // Montar itens e calcular subtotal
        FoodOrder order = new FoodOrder();
        order.setCustomer(customer);
        order.setClient(client);
        order.setNotes(notes);
        order.setStatus(FoodOrder.OrderStatus.PLACED);

        BigDecimal subtotal = BigDecimal.ZERO;
        int maxPrepTime = 0;

        for (OrderItemRequest itemReq : items) {
            Product product = productRepository.findById(itemReq.productId)
                    .orElseThrow(() -> new RuntimeException("Produto não encontrado: " + itemReq.productId));

            if (!product.getClient().getId().equals(clientId)) {
                throw new RuntimeException("Produto " + product.getName() + " não pertence a este restaurante");
            }
            if (!product.getAvailable()) {
                throw new RuntimeException("Produto " + product.getName() + " não está disponível no momento");
            }

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setQuantity(itemReq.quantity);
            item.setUnitPrice(product.getPrice()); // snapshot do preço
            item.setNotes(itemReq.notes);
            order.getItems().add(item);

            subtotal = subtotal.add(product.getPrice().multiply(BigDecimal.valueOf(itemReq.quantity)));

            if (product.getPreparationTimeMinutes() != null && product.getPreparationTimeMinutes() > maxPrepTime) {
                maxPrepTime = product.getPreparationTimeMinutes();
            }
        }

        // Verificar pedido mínimo
        if (store != null && store.getMinOrder() != null && subtotal.compareTo(store.getMinOrder()) < 0) {
            throw new RuntimeException("Pedido mínimo é R$ " + store.getMinOrder().setScale(2, RoundingMode.HALF_UP));
        }

        // Calcular taxa de entrega (baseado na distância customer ↔ restaurante)
        BigDecimal deliveryFee = calculateDeliveryFee(customer, client);

        order.setSubtotal(subtotal.setScale(2, RoundingMode.HALF_UP));
        order.setDeliveryFee(deliveryFee);
        order.setTotal(subtotal.add(deliveryFee).setScale(2, RoundingMode.HALF_UP));
        order.setEstimatedPreparationMinutes(maxPrepTime > 0 ? maxPrepTime : null);

        FoodOrder saved = orderRepository.save(order);

        // Notificar restaurante
        try {
            pushNotificationService.sendNotificationToUser(
                    client.getId(),
                    "🍽️ Novo pedido #" + saved.getId(),
                    "Pedido de R$ " + saved.getTotal() + " — toque para ver",
                    null
            );
        } catch (Exception e) {
            log.warn("Falha ao notificar restaurante sobre pedido #{}: {}", saved.getId(), e.getMessage());
        }

        log.info("🍽️ Pedido #{} criado: {} → {}, R$ {}", saved.getId(), customer.getName(), client.getName(), saved.getTotal());
        return saved;
    }

    // ================================================================
    // AÇÕES DO RESTAURANTE (CLIENT)
    // ================================================================

    public FoodOrder accept(Long orderId, UUID clientId) {
        FoodOrder order = findAndValidateClient(orderId, clientId);
        validateStatus(order, FoodOrder.OrderStatus.PLACED);

        order.setStatus(FoodOrder.OrderStatus.ACCEPTED);
        order.setAcceptedAt(OffsetDateTime.now(ZONE));

        notifyCustomer(order, "✅ Pedido aceito", "O restaurante aceitou seu pedido #" + orderId);

        log.info("✅ Pedido #{} aceito pelo restaurante", orderId);
        return orderRepository.save(order);
    }

    public FoodOrder startPreparing(Long orderId, UUID clientId) {
        FoodOrder order = findAndValidateClient(orderId, clientId);
        validateStatus(order, FoodOrder.OrderStatus.ACCEPTED);

        order.setStatus(FoodOrder.OrderStatus.PREPARING);
        order.setPreparingAt(OffsetDateTime.now(ZONE));

        notifyCustomer(order, "👨‍🍳 Pedido em preparo", "Seu pedido #" + orderId + " está sendo preparado");

        log.info("👨‍🍳 Pedido #{} em preparo", orderId);
        return orderRepository.save(order);
    }

    public FoodOrder markReady(Long orderId, UUID clientId) {
        FoodOrder order = findAndValidateClient(orderId, clientId);
        if (order.getStatus() != FoodOrder.OrderStatus.ACCEPTED && order.getStatus() != FoodOrder.OrderStatus.PREPARING) {
            throw new RuntimeException("Pedido não está em preparo (status atual: " + order.getStatus() + ")");
        }

        order.setStatus(FoodOrder.OrderStatus.READY);
        order.setReadyAt(OffsetDateTime.now(ZONE));
        FoodOrder saved = orderRepository.save(order);

        // Criar Delivery automaticamente
        try {
            Delivery delivery = createDeliveryFromOrder(saved);
            saved.setDelivery(delivery);
            saved.setStatus(FoodOrder.OrderStatus.DELIVERING);
            saved = orderRepository.save(saved);
            log.info("🚀 Pedido #{} pronto → Delivery #{} criada", orderId, delivery.getId());
        } catch (Exception e) {
            log.error("❌ Falha ao criar delivery para pedido #{}: {}", orderId, e.getMessage());
        }

        notifyCustomer(saved, "🏍️ Pedido saindo", "Seu pedido #" + orderId + " está saindo para entrega");

        return saved;
    }

    public FoodOrder cancel(Long orderId, UUID userId, String reason) {
        FoodOrder order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        // Cliente ou restaurante pode cancelar
        boolean isCustomer = order.getCustomer().getId().equals(userId);
        boolean isClient = order.getClient().getId().equals(userId);
        if (!isCustomer && !isClient) {
            throw new RuntimeException("Você não tem permissão para cancelar este pedido");
        }

        if (order.getStatus() == FoodOrder.OrderStatus.COMPLETED) {
            throw new RuntimeException("Não é possível cancelar pedido já entregue");
        }
        if (order.getStatus() == FoodOrder.OrderStatus.CANCELLED) {
            throw new RuntimeException("Pedido já está cancelado");
        }

        order.setStatus(FoodOrder.OrderStatus.CANCELLED);
        order.setCancelledAt(OffsetDateTime.now(ZONE));
        order.setCancellationReason(reason);

        // Cancelar delivery associada se existir
        if (order.getDelivery() != null) {
            try {
                deliveryService.cancel(order.getDelivery().getId(), null, "Pedido cancelado: " + reason);
            } catch (Exception e) {
                log.warn("Falha ao cancelar delivery do pedido #{}: {}", orderId, e.getMessage());
            }
        }

        // Notificar a outra parte
        if (isCustomer) {
            notifyClient(order, "❌ Pedido cancelado", "O cliente cancelou o pedido #" + orderId);
        } else {
            notifyCustomer(order, "❌ Pedido cancelado", "O restaurante cancelou seu pedido #" + orderId + ": " + reason);
        }

        log.info("❌ Pedido #{} cancelado por {}: {}", orderId, isCustomer ? "customer" : "restaurante", reason);
        return orderRepository.save(order);
    }

    public FoodOrder complete(Long orderId) {
        FoodOrder order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        if (order.getStatus() == FoodOrder.OrderStatus.COMPLETED) {
            return order; // idempotente
        }

        order.setStatus(FoodOrder.OrderStatus.COMPLETED);
        order.setCompletedAt(OffsetDateTime.now(ZONE));

        notifyCustomer(order, "✅ Pedido entregue", "Seu pedido #" + orderId + " foi entregue. Bom apetite!");

        log.info("✅ Pedido #{} entregue com sucesso", orderId);
        return orderRepository.save(order);
    }

    // ================================================================
    // CONSULTAS
    // ================================================================

    public FoodOrder findById(Long id) {
        return orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
    }

    public List<FoodOrder> findByCustomer(UUID customerId) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    public List<FoodOrder> findByClient(UUID clientId) {
        return orderRepository.findByClientIdOrderByCreatedAtDesc(clientId);
    }

    public List<FoodOrder> findActiveByClient(UUID clientId) {
        return orderRepository.findByClientIdAndStatusInOrderByCreatedAtDesc(clientId,
                List.of(FoodOrder.OrderStatus.PLACED, FoodOrder.OrderStatus.ACCEPTED,
                        FoodOrder.OrderStatus.PREPARING, FoodOrder.OrderStatus.READY,
                        FoodOrder.OrderStatus.DELIVERING));
    }

    // ================================================================
    // HELPERS
    // ================================================================

    private Delivery createDeliveryFromOrder(FoodOrder order) {
        User client = order.getClient();
        User customer = order.getCustomer();

        Delivery delivery = new Delivery();
        delivery.setFromAddress(client.getName()); // endereço do restaurante
        delivery.setFromLatitude(client.getGpsLatitude());
        delivery.setFromLongitude(client.getGpsLongitude());
        delivery.setToAddress(customer.getName()); // endereço do customer
        delivery.setToLatitude(customer.getGpsLatitude());
        delivery.setToLongitude(customer.getGpsLongitude());
        delivery.setRecipientName(customer.getName());
        delivery.setRecipientPhone(customer.getPhoneDdd() != null && customer.getPhoneNumber() != null
                ? customer.getPhoneDdd() + customer.getPhoneNumber() : null);
        delivery.setItemDescription("Pedido #" + order.getId());
        delivery.setTotalAmount(order.getSubtotal());
        delivery.setShippingFee(order.getDeliveryFee());
        delivery.setDeliveryType(Delivery.DeliveryType.DELIVERY);

        // Calcular distância
        if (client.getGpsLatitude() != null && client.getGpsLongitude() != null
                && customer.getGpsLatitude() != null && customer.getGpsLongitude() != null) {
            double distKm = haversineKm(client.getGpsLatitude(), client.getGpsLongitude(),
                    customer.getGpsLatitude(), customer.getGpsLongitude());
            delivery.setDistanceKm(BigDecimal.valueOf(distKm).setScale(2, RoundingMode.HALF_UP));
        }

        return deliveryService.create(delivery, client.getId(), client.getId());
    }

    private BigDecimal calculateDeliveryFee(User customer, User client) {
        if (customer.getGpsLatitude() == null || customer.getGpsLongitude() == null
                || client.getGpsLatitude() == null || client.getGpsLongitude() == null) {
            // Sem coordenadas → taxa mínima
            SiteConfiguration config = siteConfigurationService.getActiveConfiguration();
            return config.getMinimumShippingFee();
        }

        double distKm = haversineKm(customer.getGpsLatitude(), customer.getGpsLongitude(),
                client.getGpsLatitude(), client.getGpsLongitude());

        SiteConfiguration config = siteConfigurationService.getActiveConfiguration();
        BigDecimal fee = BigDecimal.valueOf(distKm).multiply(config.getPricePerKm());
        if (fee.compareTo(config.getMinimumShippingFee()) < 0) {
            fee = config.getMinimumShippingFee();
        }
        return fee.setScale(2, RoundingMode.HALF_UP);
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private FoodOrder findAndValidateClient(Long orderId, UUID clientId) {
        FoodOrder order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
        if (!order.getClient().getId().equals(clientId)) {
            throw new RuntimeException("Este pedido não pertence ao seu restaurante");
        }
        return order;
    }

    private void validateStatus(FoodOrder order, FoodOrder.OrderStatus expected) {
        if (order.getStatus() != expected) {
            throw new RuntimeException("Status inválido. Esperado: " + expected + ", atual: " + order.getStatus());
        }
    }

    private void notifyCustomer(FoodOrder order, String title, String body) {
        try {
            pushNotificationService.sendNotificationToUser(order.getCustomer().getId(), title, body, null);
        } catch (Exception e) {
            log.warn("Falha ao notificar customer: {}", e.getMessage());
        }
    }

    private void notifyClient(FoodOrder order, String title, String body) {
        try {
            pushNotificationService.sendNotificationToUser(order.getClient().getId(), title, body, null);
        } catch (Exception e) {
            log.warn("Falha ao notificar restaurante: {}", e.getMessage());
        }
    }

    // ================================================================
    // REQUEST DTO (inner class simples)
    // ================================================================

    public static class OrderItemRequest {
        public Long productId;
        public int quantity;
        public String notes;
    }
}
