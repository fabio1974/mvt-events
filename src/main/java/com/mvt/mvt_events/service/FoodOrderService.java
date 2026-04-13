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
    private final DeliveryStopRepository deliveryStopRepository;
    private final PushNotificationService pushNotificationService;
    private final SiteConfigurationService siteConfigurationService;

    public FoodOrderService(FoodOrderRepository orderRepository, ProductRepository productRepository,
                            UserRepository userRepository, StoreProfileRepository storeProfileRepository,
                            DeliveryService deliveryService, DeliveryStopRepository deliveryStopRepository,
                            PushNotificationService pushNotificationService,
                            SiteConfigurationService siteConfigurationService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.storeProfileRepository = storeProfileRepository;
        this.deliveryStopRepository = deliveryStopRepository;
        this.deliveryService = deliveryService;
        this.pushNotificationService = pushNotificationService;
        this.siteConfigurationService = siteConfigurationService;
    }

    // ================================================================
    // CRIAR PEDIDO (CUSTOMER)
    // ================================================================

    public FoodOrder create(UUID customerId, UUID clientId, List<OrderItemRequest> items, String notes,
                            String deliveryAddress, Double deliveryLat, Double deliveryLng) {
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

        // Coordenadas de entrega: usa endereço informado ou GPS do customer
        Double destLat = deliveryLat != null ? deliveryLat : customer.getGpsLatitude();
        Double destLng = deliveryLng != null ? deliveryLng : customer.getGpsLongitude();
        String destAddr = deliveryAddress != null ? deliveryAddress : customer.getName();

        // Validar distância mínima entre destino e restaurante (200m)
        if (destLat != null && destLng != null
                && client.getGpsLatitude() != null && client.getGpsLongitude() != null) {
            double distMeters = haversineKm(destLat, destLng,
                    client.getGpsLatitude(), client.getGpsLongitude()) * 1000;
            if (distMeters < 200) {
                throw new RuntimeException("O endereço de entrega está muito próximo do restaurante (" + Math.round(distMeters) + "m). Escolha outro endereço.");
            }
        } else if (destLat == null || destLng == null) {
            throw new RuntimeException("Informe o endereço de entrega para fazer pedidos.");
        }

        // Montar itens e calcular subtotal
        FoodOrder order = new FoodOrder();
        order.setCustomer(customer);
        order.setClient(client);
        order.setNotes(notes);
        order.setStatus(FoodOrder.OrderStatus.PLACED);
        order.setDeliveryAddress(destAddr);
        order.setDeliveryLatitude(destLat);
        order.setDeliveryLongitude(destLng);

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

        // Calcular taxa de entrega (baseado na distância destino ↔ restaurante)
        BigDecimal deliveryFee = calculateDeliveryFee(destLat, destLng, client);

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

    private static final int GROUPING_WINDOW_MINUTES = 10;

    public FoodOrder markReady(Long orderId, UUID clientId) {
        FoodOrder order = findAndValidateClient(orderId, clientId);
        if (order.getStatus() != FoodOrder.OrderStatus.ACCEPTED && order.getStatus() != FoodOrder.OrderStatus.PREPARING) {
            throw new RuntimeException("Pedido não está em preparo (status atual: " + order.getStatus() + ")");
        }

        order.setStatus(FoodOrder.OrderStatus.READY);
        order.setReadyAt(OffsetDateTime.now(ZONE));
        FoodOrder saved = orderRepository.save(order);

        try {
            // Tentar agrupar com Delivery existente (PENDING ou ACCEPTED, últimos 5 min)
            Delivery groupedDelivery = tryGroupWithExistingDelivery(saved);

            if (groupedDelivery != null) {
                saved.setDelivery(groupedDelivery);
                saved.setStatus(FoodOrder.OrderStatus.DELIVERING);
                saved = orderRepository.save(saved);
                log.info("📦 Pedido #{} agrupado na Delivery #{} (multi-stop)", orderId, groupedDelivery.getId());
            } else {
                // Criar nova Delivery
                Delivery delivery = createDeliveryFromOrder(saved);
                saved.setDelivery(delivery);
                saved.setStatus(FoodOrder.OrderStatus.DELIVERING);
                saved = orderRepository.save(saved);
                log.info("🚀 Pedido #{} pronto → Delivery #{} criada", orderId, delivery.getId());
            }
        } catch (Exception e) {
            log.error("❌ Falha ao criar/agrupar delivery para pedido #{}: {}", orderId, e.getMessage());
        }

        notifyCustomer(saved, "🏍️ Pedido saindo", "Seu pedido #" + orderId + " está saindo para entrega");

        return saved;
    }

    private static final double COURIER_NEAR_STORE_METERS = 200;

    /**
     * Tenta agrupar o pedido com uma Delivery existente do mesmo restaurante.
     *
     * Regras de agrupamento:
     * - PENDING: agrupa sempre (courier ainda não aceitou)
     * - ACCEPTED: agrupa se courier está a menos de 200m do restaurante
     *   e chegou há menos de 5 minutos (ainda está coletando)
     * - IN_TRANSIT: nunca agrupa (courier já saiu)
     */
    private Delivery tryGroupWithExistingDelivery(FoodOrder order) {
        OffsetDateTime since = OffsetDateTime.now(ZONE).minusMinutes(GROUPING_WINDOW_MINUTES);
        List<FoodOrder> recentOrders = orderRepository.findRecentDeliveringByClient(
                order.getClient().getId(), since);

        if (recentOrders.isEmpty()) return null;

        User restaurant = order.getClient();
        Double storeLat = restaurant.getGpsLatitude();
        Double storeLng = restaurant.getGpsLongitude();

        for (FoodOrder recentOrder : recentOrders) {
            Delivery candidate = recentOrder.getDelivery();
            if (candidate == null) continue;

            // Recarregar delivery
            candidate = deliveryService.findById(candidate.getId(), null);

            if (candidate.getStatus() == Delivery.DeliveryStatus.PENDING) {
                // PENDING: sempre agrupável
                return addStopToDelivery(candidate, order);
            }

            if (candidate.getStatus() == Delivery.DeliveryStatus.ACCEPTED) {
                // ACCEPTED: só se courier está perto do restaurante
                User courier = candidate.getCourier();
                if (courier == null) continue;

                if (storeLat != null && storeLng != null
                        && courier.getGpsLatitude() != null && courier.getGpsLongitude() != null) {
                    double distMeters = haversineKm(storeLat, storeLng,
                            courier.getGpsLatitude(), courier.getGpsLongitude()) * 1000;
                    if (distMeters <= COURIER_NEAR_STORE_METERS) {
                        log.info("📦 Courier está a {}m do restaurante — agrupando pedido #{}",
                                Math.round(distMeters), order.getId());
                        return addStopToDelivery(candidate, order);
                    } else {
                        log.info("🚫 Courier está a {}m do restaurante (> {}m) — não agrupa pedido #{}",
                                Math.round(distMeters), COURIER_NEAR_STORE_METERS, order.getId());
                    }
                }
            }
            // IN_TRANSIT: nunca agrupa
        }

        return null;
    }

    private Delivery addStopToDelivery(Delivery delivery, FoodOrder order) {
        User customer = order.getCustomer();
        Double destLat = order.getDeliveryLatitude() != null ? order.getDeliveryLatitude() : customer.getGpsLatitude();
        Double destLng = order.getDeliveryLongitude() != null ? order.getDeliveryLongitude() : customer.getGpsLongitude();
        String destAddr = order.getDeliveryAddress() != null ? order.getDeliveryAddress() : customer.getName();
        int nextOrder = delivery.getStops() != null ? delivery.getStops().size() + 1 : 2;

        DeliveryStop newStop = DeliveryStop.builder()
                .delivery(delivery)
                .stopOrder(nextOrder)
                .address(destAddr)
                .latitude(destLat)
                .longitude(destLng)
                .recipientName(customer.getName())
                .recipientPhone(customer.getPhoneDdd() != null && customer.getPhoneNumber() != null
                        ? customer.getPhoneDdd() + customer.getPhoneNumber() : null)
                .itemDescription("Pedido #" + order.getId())
                .status(DeliveryStop.StopStatus.PENDING)
                .build();

        deliveryStopRepository.save(newStop);

        log.info("📦 Stop #{} adicionado à Delivery #{} — pedido #{} agrupado",
                nextOrder, delivery.getId(), order.getId());

        return delivery;
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

        // Usa coordenadas de entrega da Order (selecionadas pelo customer)
        Double destLat = order.getDeliveryLatitude() != null ? order.getDeliveryLatitude() : customer.getGpsLatitude();
        Double destLng = order.getDeliveryLongitude() != null ? order.getDeliveryLongitude() : customer.getGpsLongitude();
        String destAddr = order.getDeliveryAddress() != null ? order.getDeliveryAddress() : customer.getName();

        Delivery delivery = new Delivery();
        delivery.setFromAddress(client.getName());
        delivery.setFromLatitude(client.getGpsLatitude());
        delivery.setFromLongitude(client.getGpsLongitude());
        delivery.setToAddress(destAddr);
        delivery.setToLatitude(destLat);
        delivery.setToLongitude(destLng);
        delivery.setRecipientName(customer.getName());
        delivery.setRecipientPhone(customer.getPhoneDdd() != null && customer.getPhoneNumber() != null
                ? customer.getPhoneDdd() + customer.getPhoneNumber() : null);
        delivery.setItemDescription("Pedido #" + order.getId());
        delivery.setTotalAmount(order.getSubtotal());
        delivery.setShippingFee(order.getDeliveryFee());
        delivery.setDeliveryType(Delivery.DeliveryType.DELIVERY);

        // Calcular distância
        if (client.getGpsLatitude() != null && client.getGpsLongitude() != null
                && destLat != null && destLng != null) {
            double distKm = haversineKm(client.getGpsLatitude(), client.getGpsLongitude(),
                    destLat, destLng);
            delivery.setDistanceKm(BigDecimal.valueOf(distKm).setScale(2, RoundingMode.HALF_UP));
        }

        return deliveryService.create(delivery, client.getId(), client.getId());
    }

    private BigDecimal calculateDeliveryFee(Double destLat, Double destLng, User client) {
        if (destLat == null || destLng == null
                || client.getGpsLatitude() == null || client.getGpsLongitude() == null) {
            SiteConfiguration config = siteConfigurationService.getActiveConfiguration();
            return config.getMinimumShippingFee();
        }

        double distKm = haversineKm(destLat, destLng,
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
