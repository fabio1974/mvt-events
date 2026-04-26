package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.*;
import com.mvt.mvt_events.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço de pedidos — módulo Zapi-Food.
 * Gerencia o ciclo de vida: PLACED → ACCEPTED → PREPARING → READY → DELIVERING → COMPLETED
 * Quando o restaurante marca READY, cria automaticamente uma Delivery.
 */
@Service
@Transactional
public class FoodOrderService implements DeliveryStatusCallback {

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
    private final GoogleDirectionsService googleDirectionsService;
    private final RestaurantTableRepository restaurantTableRepository;
    private final com.mvt.mvt_events.repository.OrderCommandRepository orderCommandRepository;
    private final ClientWaiterRepository clientWaiterRepository;

    public FoodOrderService(FoodOrderRepository orderRepository, ProductRepository productRepository,
                            UserRepository userRepository, StoreProfileRepository storeProfileRepository,
                            @Lazy DeliveryService deliveryService, DeliveryStopRepository deliveryStopRepository,
                            PushNotificationService pushNotificationService,
                            SiteConfigurationService siteConfigurationService,
                            GoogleDirectionsService googleDirectionsService,
                            RestaurantTableRepository restaurantTableRepository,
                            com.mvt.mvt_events.repository.OrderCommandRepository orderCommandRepository,
                            ClientWaiterRepository clientWaiterRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.storeProfileRepository = storeProfileRepository;
        this.deliveryStopRepository = deliveryStopRepository;
        this.deliveryService = deliveryService;
        this.pushNotificationService = pushNotificationService;
        this.siteConfigurationService = siteConfigurationService;
        this.googleDirectionsService = googleDirectionsService;
        this.restaurantTableRepository = restaurantTableRepository;
        this.orderCommandRepository = orderCommandRepository;
        this.clientWaiterRepository = clientWaiterRepository;
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

        if (!client.getEnabled() || !client.hasBankAccount()) {
            throw new RuntimeException("Este estabelecimento ainda não está habilitado na plataforma");
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

        // Valida coordenadas de entrega
        if (destLat == null || destLng == null) {
            throw new RuntimeException("Informe o endereço de entrega para fazer pedidos.");
        }

        // Coordenadas do estabelecimento: prefere endereço fixo (Address), cai pro GPS só se não houver
        Address storeAddress = client.getAddress();
        Double storeLat = storeAddress != null && storeAddress.getLatitude() != null
                ? storeAddress.getLatitude() : client.getGpsLatitude();
        Double storeLng = storeAddress != null && storeAddress.getLongitude() != null
                ? storeAddress.getLongitude() : client.getGpsLongitude();

        // Validar distância mínima entre destino e restaurante usando rota do Google.
        // O limite vem de SiteConfiguration (default 50m). Zero desliga a validação.
        int minDistMeters = siteConfigurationService.getActiveConfiguration()
                .getMinOrderDistanceMeters();
        if (minDistMeters > 0 && storeLat != null && storeLng != null) {
            int distMeters = googleDirectionsService.getDistanceMeters(
                    destLat, destLng, storeLat, storeLng);
            if (distMeters < 0) {
                // Fallback: haversine em linha reta se Google Directions indisponível
                distMeters = (int) Math.round(
                        haversineKm(destLat, destLng, storeLat, storeLng) * 1000);
                log.info("📍 Directions indisponível, validando distância via haversine: {}m", distMeters);
            }
            if (distMeters < minDistMeters) {
                throw new RuntimeException("O endereço de entrega está muito próximo do restaurante ("
                        + distMeters + "m). Escolha outro endereço.");
            }
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
            item.setUnitPrice(product.priceFor(order.getOrderType())); // snapshot do preço
            item.setNotes(itemReq.notes);
            item.setObservation(itemReq.observation);
            BigDecimal addonTotal = attachAddons(item, itemReq.addons, clientId, order.getOrderType());
            order.getItems().add(item);

            subtotal = subtotal.add(product.priceFor(order.getOrderType()).multiply(BigDecimal.valueOf(itemReq.quantity)));
            subtotal = subtotal.add(addonTotal);

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
    // CRIAR PEDIDO DE MESA (WAITER)
    // ================================================================

    public FoodOrder createTableOrder(UUID waiterId, UUID clientId, Long tableId,
                                      List<OrderItemRequest> items, String notes) {
        User author = userRepository.findById(waiterId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        if (author.getRole() != User.Role.WAITER && author.getRole() != User.Role.CLIENT) {
            throw new RuntimeException("Apenas garçons ou o próprio cliente podem criar pedidos de mesa");
        }

        // Se o autor é CLIENT, ele é o próprio restaurante
        User client;
        if (author.getRole() == User.Role.CLIENT) {
            client = author;
        } else {
            client = userRepository.findById(clientId)
                    .orElseThrow(() -> new RuntimeException("Restaurante não encontrado"));
            if (client.getRole() != User.Role.CLIENT) {
                throw new RuntimeException("Destinatário do pedido deve ser um CLIENT (restaurante)");
            }
        }

        // Verificar se módulo de mesas está habilitado
        StoreProfile store = storeProfileRepository.findByUserId(clientId).orElse(null);
        if (store == null || !store.getTableOrdersEnabled()) {
            throw new RuntimeException("Módulo de mesas não está habilitado para este estabelecimento");
        }

        // Verificar mesa
        RestaurantTable table = null;
        if (tableId != null) {
            table = restaurantTableRepository.findById(tableId)
                    .orElseThrow(() -> new RuntimeException("Mesa não encontrada"));
            if (!table.getClient().getId().equals(clientId)) {
                throw new RuntimeException("Mesa não pertence a este estabelecimento");
            }
            if (!table.getActive()) {
                throw new RuntimeException("Mesa #" + table.getNumber() + " está desativada");
            }
            // Auto-transição: mesa fica OCCUPIED apenas quando há itens no pedido.
            // Pedido vazio pode existir (garçom criou comanda ainda sem itens) sem ocupar a mesa.
            if (items != null && !items.isEmpty()) {
                table.setStatus(RestaurantTable.TableStatus.OCCUPIED);
                restaurantTableRepository.save(table);
            }
        }

        // Montar itens e calcular subtotal
        FoodOrder order = new FoodOrder();
        order.setCustomer(author); // autor do pedido (garçom ou cliente)
        order.setClient(client);
        if (author.getRole() == User.Role.WAITER) {
            order.setWaiter(author);
        }
        order.setTable(table);
        if (table != null) {
            order.setTableNumberField(table.getNumber());
        }
        order.setOrderType(FoodOrder.OrderType.TABLE);
        order.setNotes(notes);
        // Mesa: garçom é o próprio restaurante, então pedido já nasce aceito e vai direto pra PREPARING.
        // Grava acceptedAt e preparingAt no mesmo instante pra timeline ficar completa.
        OffsetDateTime nowCreate = OffsetDateTime.now(ZONE);
        order.setStatus(FoodOrder.OrderStatus.PREPARING);
        order.setAcceptedAt(nowCreate);
        order.setPreparingAt(nowCreate);

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
            item.setUnitPrice(product.priceFor(order.getOrderType()));
            item.setNotes(itemReq.notes);
            item.setObservation(itemReq.observation);
            if (itemReq.commandId != null) {
                item.setCommand(orderCommandRepository.findById(itemReq.commandId)
                        .orElseThrow(() -> new RuntimeException("Comanda não encontrada: " + itemReq.commandId)));
            }
            BigDecimal addonTotal = attachAddons(item, itemReq.addons, clientId, order.getOrderType());
            order.getItems().add(item);

            subtotal = subtotal.add(product.priceFor(order.getOrderType()).multiply(BigDecimal.valueOf(itemReq.quantity)));
            subtotal = subtotal.add(addonTotal);

            if (product.getPreparationTimeMinutes() != null && product.getPreparationTimeMinutes() > maxPrepTime) {
                maxPrepTime = product.getPreparationTimeMinutes();
            }
        }

        order.setSubtotal(subtotal.setScale(2, RoundingMode.HALF_UP));
        order.setDeliveryFee(BigDecimal.ZERO); // sem taxa de entrega em pedido de mesa
        order.setTotal(subtotal.setScale(2, RoundingMode.HALF_UP));
        order.setEstimatedPreparationMinutes(maxPrepTime > 0 ? maxPrepTime : null);

        FoodOrder saved = orderRepository.save(order);

        log.info("🍽️ Pedido de mesa #{} criado: {} ({}) → mesa {} do {}, R$ {}",
                saved.getId(), author.getName(), author.getRole(),
                tableId, client.getName(), saved.getTotal());
        return saved;
    }

    // ================================================================
    // ADICIONAR ITENS A PEDIDO EXISTENTE (nova rodada)
    // ================================================================

    public FoodOrder addItemsToOrder(Long orderId, UUID waiterId, List<OrderItemRequest> newItems) {
        FoodOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        // Validar que o pedido não está finalizado
        if (order.getStatus() == FoodOrder.OrderStatus.COMPLETED || order.getStatus() == FoodOrder.OrderStatus.CANCELLED) {
            throw new RuntimeException("Pedido já está " + order.getStatus().name());
        }

        UUID clientId = order.getClient().getId();

        // Calcular próxima rodada
        int nextRound = order.getItems().stream()
                .mapToInt(OrderItem::getRound)
                .max()
                .orElse(0) + 1;

        BigDecimal addedSubtotal = BigDecimal.ZERO;

        for (OrderItemRequest itemReq : newItems) {
            Product product = productRepository.findById(itemReq.productId)
                    .orElseThrow(() -> new RuntimeException("Produto não encontrado: " + itemReq.productId));

            if (!product.getClient().getId().equals(clientId)) {
                throw new RuntimeException("Produto " + product.getName() + " não pertence a este restaurante");
            }

            // Dedup por (produto, comanda): cerveja do Pedro não funde com cerveja do Iran.
            // A partir da fase 2, itens com customização (observation ou addons) NUNCA fazem dedup —
            // cada item precisa manter identidade própria pra preservar os adicionais dele.
            // Dedup só funde com itens existentes que também não tenham customização.
            Long reqCommandId = itemReq.commandId;
            boolean canDedup = !itemReq.hasCustomization();
            Optional<OrderItem> existingItem = canDedup
                    ? order.getItems().stream()
                            .filter(i -> i.getProduct().getId().equals(product.getId()))
                            .filter(i -> java.util.Objects.equals(i.getCommandId(), reqCommandId))
                            .filter(i -> (i.getObservation() == null || i.getObservation().isBlank())
                                    && (i.getAddons() == null || i.getAddons().isEmpty()))
                            .findFirst()
                    : Optional.empty();

            if (existingItem.isPresent()) {
                existingItem.get().setQuantity(existingItem.get().getQuantity() + itemReq.quantity);
                addedSubtotal = addedSubtotal.add(product.priceFor(order.getOrderType()).multiply(BigDecimal.valueOf(itemReq.quantity)));
            } else {
                OrderItem item = new OrderItem();
                item.setOrder(order);
                item.setProduct(product);
                item.setQuantity(itemReq.quantity);
                item.setUnitPrice(product.priceFor(order.getOrderType()));
                item.setNotes(itemReq.notes);
                item.setObservation(itemReq.observation);
                item.setRound(nextRound);
                item.setSentAt(OffsetDateTime.now());
                if (reqCommandId != null) {
                    item.setCommand(orderCommandRepository.findById(reqCommandId)
                            .orElseThrow(() -> new RuntimeException("Comanda não encontrada: " + reqCommandId)));
                }
                BigDecimal addonTotal = attachAddons(item, itemReq.addons, clientId, order.getOrderType());
                order.getItems().add(item);

                addedSubtotal = addedSubtotal.add(product.priceFor(order.getOrderType()).multiply(BigDecimal.valueOf(itemReq.quantity)));
                addedSubtotal = addedSubtotal.add(addonTotal);
            }
        }

        // Atualizar totais
        order.setSubtotal(order.getSubtotal().add(addedSubtotal).setScale(2, RoundingMode.HALF_UP));
        order.setTotal(order.getSubtotal().add(order.getDeliveryFee()).setScale(2, RoundingMode.HALF_UP));

        // Se o pedido já foi aceito/preparando/pronto, volta para PREPARING
        if (order.getStatus() != FoodOrder.OrderStatus.PLACED) {
            order.setStatus(FoodOrder.OrderStatus.PREPARING);
            order.setPreparingAt(OffsetDateTime.now());
        }

        // Marcar mesa como OCCUPIED se o pedido ganhou itens (pedido vazio não ocupava a mesa)
        if (order.getTable() != null && !order.getItems().isEmpty()
                && order.getTable().getStatus() != RestaurantTable.TableStatus.OCCUPIED) {
            RestaurantTable t = order.getTable();
            t.setStatus(RestaurantTable.TableStatus.OCCUPIED);
            restaurantTableRepository.save(t);
        }

        FoodOrder saved = orderRepository.save(order);

        log.info("🍽️ Pedido #{} — rodada {} adicionada: +{} itens, novo total R$ {}",
                orderId, nextRound, newItems.size(), saved.getTotal());
        return saved;
    }

    // ================================================================
    // REMOVER ITEM DO PEDIDO
    // ================================================================

    public FoodOrder removeItemFromOrder(Long orderId, Long itemId, UUID requesterId) {
        FoodOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        if (order.getStatus() == FoodOrder.OrderStatus.COMPLETED || order.getStatus() == FoodOrder.OrderStatus.CANCELLED) {
            throw new RuntimeException("Pedido já está " + order.getStatus().name());
        }

        // Guarda: se o solicitante é WAITER, exige permissão canCancelItem no link com o CLIENT do pedido
        if (requesterId != null && order.getClient() != null) {
            User requester = userRepository.findById(requesterId).orElse(null);
            if (requester != null && requester.getRole() == User.Role.WAITER) {
                ClientWaiter link = clientWaiterRepository
                        .findByClientAndWaiter(order.getClient(), requester)
                        .orElseThrow(() -> new RuntimeException("Garçom não vinculado a este estabelecimento"));
                if (!link.isCanCancelItem()) {
                    throw new RuntimeException("Garçom não tem permissão para cancelar itens deste estabelecimento");
                }
            }
        }

        OrderItem item = order.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Item não encontrado no pedido"));

        BigDecimal unitPrice = item.getUnitPrice();
        BigDecimal delta = unitPrice; // sempre subtrai 1x unitPrice

        if (item.getQuantity() > 1) {
            // Decrementa quantidade; addons permanecem no item (quantity dos addons é absoluta)
            item.setQuantity(item.getQuantity() - 1);
        } else {
            // Última unidade — remove o item inteiro e abate os addons do subtotal também
            delta = delta.add(sumAddons(item));
            order.getItems().remove(item);
        }

        // Recalcular totais
        order.setSubtotal(order.getSubtotal().subtract(delta).setScale(2, RoundingMode.HALF_UP));
        order.setTotal(order.getSubtotal().add(order.getDeliveryFee()).setScale(2, RoundingMode.HALF_UP));

        // Se ficou sem itens, cancela o pedido e libera a mesa
        if (order.getItems().isEmpty()) {
            order.setStatus(FoodOrder.OrderStatus.CANCELLED);
            order.setCancellationReason("Todos os itens removidos");
            order.setCancelledAt(OffsetDateTime.now());
            if (order.getTable() != null) {
                order.getTable().setStatus(RestaurantTable.TableStatus.AVAILABLE);
                restaurantTableRepository.save(order.getTable());
            }
        }

        log.info("🗑️ 1x item #{} removido do pedido #{}, novo total R$ {}", itemId, orderId, order.getTotal());
        return orderRepository.save(order);
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

        if (saved.getOrderType() == FoodOrder.OrderType.TABLE) {
            // Pedido de mesa: não cria delivery — notifica garçom para servir
            if (saved.getWaiter() != null) {
                notifyWaiter(saved, "📦 Pedido pronto!",
                        "Mesa " + (saved.getTable() != null ? "#" + saved.getTable().getNumber() : "") +
                        " — pedido #" + orderId + " pronto para servir");
            }
            log.info("📦 Pedido de mesa #{} pronto (sem delivery)", orderId);
        } else {
            // Pedido de delivery: criar ou agrupar delivery
            try {
                Delivery groupedDelivery = tryGroupWithExistingDelivery(saved);

                if (groupedDelivery != null) {
                    groupedDelivery.setOrder(saved);
                    if (groupedDelivery.getStatus() == Delivery.DeliveryStatus.ACCEPTED
                            || groupedDelivery.getStatus() == Delivery.DeliveryStatus.IN_TRANSIT) {
                        saved.setStatus(FoodOrder.OrderStatus.DELIVERING);
                        saved = orderRepository.save(saved);
                    }
                    log.info("📦 Pedido #{} agrupado na Delivery #{} (multi-stop)", orderId, groupedDelivery.getId());
                } else {
                    Delivery delivery = createDeliveryFromOrder(saved);
                    log.info("🚀 Pedido #{} pronto → Delivery #{} criada (PENDING, aguardando courier)", orderId, delivery.getId());
                }
            } catch (Exception e) {
                log.error("❌ Falha ao criar/agrupar delivery para pedido #{}: {}", orderId, e.getMessage());
            }

            notifyCustomer(saved, "📦 Pedido pronto", "Seu pedido #" + orderId + " está pronto e aguardando entregador");
        }

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
            Delivery candidate = recentOrder.getActiveDelivery();
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

    /**
     * Fecha a conta de uma mesa: marca COMPLETED + registra forma de pagamento.
     */
    public FoodOrder closeTableOrder(Long orderId, UUID waiterId, String paymentMethodStr) {
        FoodOrder order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        if (order.getOrderType() != FoodOrder.OrderType.TABLE) {
            throw new RuntimeException("Só é possível fechar conta de pedidos de mesa");
        }

        if (order.getStatus() == FoodOrder.OrderStatus.COMPLETED) {
            throw new RuntimeException("Conta já foi fechada");
        }
        if (order.getStatus() == FoodOrder.OrderStatus.CANCELLED) {
            throw new RuntimeException("Pedido está cancelado");
        }

        // Registrar forma de pagamento
        if (paymentMethodStr != null && !paymentMethodStr.isEmpty()) {
            try {
                order.setTablePaymentMethod(PaymentMethod.valueOf(paymentMethodStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Forma de pagamento inválida: " + paymentMethodStr);
            }
        }

        order.setStatus(FoodOrder.OrderStatus.AWAITING_PAYMENT);
        order.setCompletedAt(OffsetDateTime.now(ZONE));

        return orderRepository.save(order);
    }

    /**
     * Confirma pagamento: AWAITING_PAYMENT → COMPLETED.
     * Se o pedido ainda não está em AWAITING_PAYMENT, marca direto como COMPLETED (pagou antes de fechar).
     */
    public FoodOrder confirmPayment(Long orderId, String paymentMethodStr) {
        FoodOrder order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        if (order.getStatus() == FoodOrder.OrderStatus.COMPLETED) {
            throw new RuntimeException("Pagamento já foi confirmado");
        }
        if (order.getStatus() == FoodOrder.OrderStatus.CANCELLED) {
            throw new RuntimeException("Pedido está cancelado");
        }

        // Registrar forma de pagamento (se não foi informada no fechar conta)
        if (paymentMethodStr != null && !paymentMethodStr.isEmpty()) {
            try {
                order.setTablePaymentMethod(PaymentMethod.valueOf(paymentMethodStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Forma de pagamento inválida: " + paymentMethodStr);
            }
        }

        order.setStatus(FoodOrder.OrderStatus.COMPLETED);
        order.setPaidAt(OffsetDateTime.now(ZONE));
        if (order.getCompletedAt() == null) {
            order.setCompletedAt(OffsetDateTime.now(ZONE));
        }

        // Auto-transição: mesa volta a AVAILABLE e desvincula do pedido
        if (order.getTable() != null) {
            RestaurantTable table = order.getTable();
            table.setStatus(RestaurantTable.TableStatus.AVAILABLE);
            restaurantTableRepository.save(table);
            order.setTable(null); // libera FK, tableNumberField mantém o histórico
        }

        return orderRepository.save(order);
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

        // Cancelar delivery ativa associada se existir
        Delivery activeDelivery = order.getActiveDelivery();
        if (activeDelivery != null) {
            try {
                deliveryService.cancel(activeDelivery.getId(), null, "Pedido cancelado: " + reason);
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

        // Liberar mesa se for pedido de mesa
        if (order.getTable() != null) {
            RestaurantTable tbl = order.getTable();
            tbl.setStatus(RestaurantTable.TableStatus.AVAILABLE);
            restaurantTableRepository.save(tbl);
            order.setTable(null);
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

        // Liberar mesa se for pedido de mesa
        if (order.getTable() != null) {
            RestaurantTable tbl = order.getTable();
            tbl.setStatus(RestaurantTable.TableStatus.AVAILABLE);
            restaurantTableRepository.save(tbl);
            order.setTable(null);
        }

        notifyCustomer(order, "✅ Pedido entregue", "Seu pedido #" + orderId + " foi entregue. Bom apetite!");

        log.info("✅ Pedido #{} entregue com sucesso", orderId);
        return orderRepository.save(order);
    }

    // ================================================================
    // CONSULTAS
    // ================================================================

    public FoodOrder findById(Long id) {
        FoodOrder order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        // Força inicialização da collection `deliveries` enquanto a transação ainda está aberta.
        // Sem isso, o getter @JsonGetter("activeDelivery") explode com LazyInitializationException
        // na hora de serializar, e o try/catch dele engole o erro retornando null —
        // resultando na timeline vazia ("Entregador a caminho", "Coletado", "Em trânsito") no app.
        // Não dá pra usar LEFT JOIN FETCH na query (MultipleBagFetchException com items + deliveries).
        if (order.getDeliveries() != null) {
            order.getDeliveries().size();
            order.getDeliveries().forEach(d -> {
                if (d.getCourier() != null) d.getCourier().getName();
            });
        }
        return order;
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

    /** Pedidos ativos do garçom em um estabelecimento */
    public List<FoodOrder> findActiveByWaiter(UUID waiterId, UUID clientId) {
        return orderRepository.findActiveByWaiterAndClient(waiterId, clientId);
    }

    /** Pedidos ativos de uma mesa */
    public List<FoodOrder> findActiveByTable(Long tableId) {
        List<FoodOrder> orders = orderRepository.findActiveByTable(tableId);
        orders.forEach(this::initAddons);
        return orders;
    }

    /** Todos os pedidos de uma mesa */
    public List<FoodOrder> findByTable(Long tableId) {
        List<FoodOrder> orders = orderRepository.findByTableId(tableId);
        orders.forEach(this::initAddons);
        return orders;
    }

    /**
     * Força inicialização lazy dos `addons` de cada OrderItem dentro da transação do service.
     * Sem isso, com open-in-view=false a serialização Jackson falha/retorna vazio.
     * Não usamos JOIN FETCH em addons pra evitar duplicação de OrderItems com múltiplos addons
     * (DISTINCT + JOIN FETCH + multiple bags não resolve bem com Hibernate).
     */
    private void initAddons(FoodOrder order) {
        if (order == null || order.getItems() == null) return;
        for (OrderItem item : order.getItems()) {
            if (item.getAddons() == null) continue;
            item.getAddons().size(); // força init do Set
            // Também força init do Product de cada addon (senão @JsonGetter productName falha no lazy)
            for (OrderItemAddon a : item.getAddons()) {
                if (a.getProduct() != null) a.getProduct().getName();
            }
        }
    }

    /** Mapa tableId → status do pedido ativo para todas as mesas de um client */
    @Transactional(readOnly = true)
    public java.util.Map<Long, String> getTablesOrderStatus(java.util.UUID clientId) {
        List<FoodOrder> activeOrders = orderRepository.findActiveByClientId(clientId);
        java.util.Map<Long, String> result = new java.util.HashMap<>();
        for (FoodOrder order : activeOrders) {
            if (order.getTable() != null) {
                result.put(order.getTable().getId(), order.getStatus().name());
            }
        }
        return result;
    }

    // ================================================================
    // HELPERS
    // ================================================================

    private Delivery createDeliveryFromOrder(FoodOrder order) {
        User client = order.getClient();
        User customer = order.getCustomer();

        // Origem: endereço cadastrado do restaurante (fallback para GPS/nome)
        Address clientAddr = client.getAddress();
        String fromAddress = clientAddr != null ? clientAddr.getFullAddress() : client.getName();
        Double fromLat = clientAddr != null && clientAddr.getLatitude() != null
                ? clientAddr.getLatitude() : client.getGpsLatitude();
        Double fromLng = clientAddr != null && clientAddr.getLongitude() != null
                ? clientAddr.getLongitude() : client.getGpsLongitude();

        // Destino: coordenadas de entrega da Order (selecionadas pelo customer)
        Double destLat = order.getDeliveryLatitude() != null ? order.getDeliveryLatitude() : customer.getGpsLatitude();
        Double destLng = order.getDeliveryLongitude() != null ? order.getDeliveryLongitude() : customer.getGpsLongitude();
        String destAddr = order.getDeliveryAddress() != null ? order.getDeliveryAddress() : customer.getName();

        // Dados do destinatário
        String recipientName = customer.getName();
        String recipientPhone = customer.getPhoneDdd() != null && customer.getPhoneNumber() != null
                ? customer.getPhoneDdd() + customer.getPhoneNumber() : null;
        // Descrição dos itens: "2x Tex Bacon, 1x Heineken, 1x Panqueca de Frango"
        String itemDesc = order.getItems().stream()
                .map(i -> i.getQuantity() + "x " + (i.getProduct() != null ? i.getProduct().getName() : "Item"))
                .reduce((a, b) -> a + ", " + b)
                .map(desc -> "Pedido #" + order.getId() + " — " + desc)
                .orElse("Pedido #" + order.getId());

        Delivery delivery = new Delivery();
        delivery.setFromAddress(fromAddress);
        delivery.setFromLatitude(fromLat);
        delivery.setFromLongitude(fromLng);
        delivery.setToAddress(destAddr);
        delivery.setToLatitude(destLat);
        delivery.setToLongitude(destLng);
        delivery.setRecipientName(recipientName);
        delivery.setRecipientPhone(recipientPhone);
        delivery.setItemDescription(itemDesc);
        delivery.setTotalAmount(order.getSubtotal());
        // shippingFee NÃO é pré-setado — DeliveryService.create() calcula a partir do distanceKm
        // (Google Directions), derivando shippingFee, estimatedShippingFee e estimatedDistanceKm
        delivery.setDeliveryType(Delivery.DeliveryType.DELIVERY);
        delivery.setPreferredVehicleType(Delivery.PreferredVehicleType.MOTORCYCLE);

        // Calcular rota e distância via Google Directions (mesma fonte que o CRUD)
        if (fromLat != null && fromLng != null && destLat != null && destLng != null) {
            List<double[]> route = googleDirectionsService.getRoute(fromLat, fromLng, destLat, destLng, null);
            if (!route.isEmpty()) {
                // Distância real pela rota (soma dos segmentos)
                double totalKm = 0;
                for (int i = 1; i < route.size(); i++) {
                    totalKm += haversineKm(route.get(i - 1)[0], route.get(i - 1)[1],
                            route.get(i)[0], route.get(i)[1]);
                }
                delivery.setDistanceKm(BigDecimal.valueOf(totalKm).setScale(2, RoundingMode.HALF_UP));
                // Converter para formato esperado pelo DeliveryService.create()
                List<List<Double>> coords = route.stream()
                        .map(p -> List.of(p[0], p[1]))
                        .toList();
                delivery.setPlannedRouteCoordinates(coords);
                log.info("📍 Rota Google calculada para pedido: {} km, {} pontos",
                        String.format("%.2f", totalKm), route.size());
            } else {
                // Fallback: haversine
                double distKm = haversineKm(fromLat, fromLng, destLat, destLng);
                delivery.setDistanceKm(BigDecimal.valueOf(distKm).setScale(2, RoundingMode.HALF_UP));
                log.info("📍 Rota Google indisponível, usando haversine: {} km", String.format("%.2f", distKm));
            }
        }

        // Criar DeliveryStop (alinhado com fluxo CRUD que sempre cria pelo menos 1 stop)
        DeliveryStop stop = DeliveryStop.builder()
                .delivery(delivery)
                .stopOrder(1)
                .address(destAddr)
                .latitude(destLat)
                .longitude(destLng)
                .recipientName(recipientName)
                .recipientPhone(recipientPhone)
                .itemDescription(itemDesc)
                .status(DeliveryStop.StopStatus.PENDING)
                .build();
        delivery.setStops(new java.util.ArrayList<>(List.of(stop)));

        // Vincular delivery ao pedido (FK no lado da delivery)
        delivery.setOrder(order);

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

    private void notifyWaiter(FoodOrder order, String title, String body) {
        try {
            if (order.getWaiter() != null) {
                pushNotificationService.sendNotificationToUser(order.getWaiter().getId(), title, body, null);
            }
        } catch (Exception e) {
            log.warn("Falha ao notificar garçom: {}", e.getMessage());
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
    // CALLBACK: sincroniza status do pedido quando a delivery muda
    // ================================================================

    @Override
    public void onDeliveryStatusChanged(Delivery delivery) {
        orderRepository.findByDeliveryId(delivery.getId()).ifPresent(order -> {
            FoodOrder.OrderStatus newStatus = mapDeliveryStatusToOrderStatus(delivery.getStatus());
            if (newStatus != null && order.getStatus() != newStatus) {
                FoodOrder.OrderStatus oldStatus = order.getStatus();
                order.setStatus(newStatus);

                // Sincronizar timestamps
                if (newStatus == FoodOrder.OrderStatus.COMPLETED) {
                    order.setCompletedAt(delivery.getCompletedAt());
                } else if (newStatus == FoodOrder.OrderStatus.CANCELLED) {
                    order.setCancelledAt(delivery.getCancelledAt());
                    order.setCancellationReason(delivery.getCancellationReason());
                }

                orderRepository.save(order);
                log.info("🔄 Pedido #{} status sincronizado: {} → {} (delivery #{} → {})",
                        order.getId(), oldStatus, newStatus, delivery.getId(), delivery.getStatus());
            }
        });
    }

    private FoodOrder.OrderStatus mapDeliveryStatusToOrderStatus(Delivery.DeliveryStatus deliveryStatus) {
        return switch (deliveryStatus) {
            case PENDING -> FoodOrder.OrderStatus.READY;
            case WAITING_PAYMENT -> null;                       // sem mudança no pedido
            case ACCEPTED, IN_TRANSIT -> FoodOrder.OrderStatus.DELIVERING;
            case COMPLETED -> FoodOrder.OrderStatus.COMPLETED;
            case CANCELLED -> FoodOrder.OrderStatus.READY;     // delivery cancelada = pedido volta pra READY
        };
    }

    // ================================================================
    // COMANDAS (split de conta por pessoa dentro do pedido de mesa)
    // ================================================================

    @Transactional
    public com.mvt.mvt_events.jpa.OrderCommand createCommand(Long orderId, String name) {
        FoodOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        if (order.getOrderType() != FoodOrder.OrderType.TABLE) {
            throw new RuntimeException("Comandas só existem em pedidos de mesa");
        }
        if (order.getStatus() == FoodOrder.OrderStatus.COMPLETED || order.getStatus() == FoodOrder.OrderStatus.CANCELLED) {
            throw new RuntimeException("Pedido já está " + order.getStatus().name());
        }

        int nextDisplay = orderCommandRepository.findMaxDisplayNumberByOrderId(orderId) + 1;

        com.mvt.mvt_events.jpa.OrderCommand cmd = com.mvt.mvt_events.jpa.OrderCommand.builder()
                .order(order)
                .displayNumber(nextDisplay)
                .name(name != null && !name.isBlank() ? name.trim() : null)
                .build();

        return orderCommandRepository.save(cmd);
    }

    @Transactional
    public com.mvt.mvt_events.jpa.OrderCommand renameCommand(Long orderId, Long commandId, String name) {
        com.mvt.mvt_events.jpa.OrderCommand cmd = orderCommandRepository.findByIdAndOrderId(commandId, orderId)
                .orElseThrow(() -> new RuntimeException("Comanda não encontrada"));
        cmd.setName(name != null && !name.isBlank() ? name.trim() : null);
        return orderCommandRepository.save(cmd);
    }

    @Transactional
    public void deleteCommand(Long orderId, Long commandId) {
        com.mvt.mvt_events.jpa.OrderCommand cmd = orderCommandRepository.findByIdAndOrderId(commandId, orderId)
                .orElseThrow(() -> new RuntimeException("Comanda não encontrada"));

        FoodOrder order = cmd.getOrder();
        boolean hasItems = order.getItems().stream()
                .anyMatch(i -> cmd.getId().equals(i.getCommandId()));
        if (hasItems) {
            throw new RuntimeException("Não é possível remover comanda com itens. Mova os itens primeiro.");
        }

        orderCommandRepository.delete(cmd);
    }

    public java.util.List<com.mvt.mvt_events.jpa.OrderCommand> listCommands(Long orderId) {
        return orderCommandRepository.findByOrderIdOrderByDisplayNumberAsc(orderId);
    }

    /**
     * Move um item entre comandas (ou para Mesa = null).
     * Se já existe item com mesmo produto na comanda destino, soma quantidades
     * (preserva dedup por product+command).
     */
    @Transactional
    public FoodOrder moveItemToCommand(Long orderId, Long itemId, Long targetCommandId) {
        FoodOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        if (order.getStatus() == FoodOrder.OrderStatus.COMPLETED || order.getStatus() == FoodOrder.OrderStatus.CANCELLED) {
            throw new RuntimeException("Pedido já está " + order.getStatus().name());
        }

        OrderItem item = order.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Item não encontrado"));

        // Valida comanda destino (se não for null = Mesa)
        com.mvt.mvt_events.jpa.OrderCommand target = null;
        if (targetCommandId != null) {
            target = orderCommandRepository.findByIdAndOrderId(targetCommandId, orderId)
                    .orElseThrow(() -> new RuntimeException("Comanda destino não encontrada"));
        }

        // No-op se já está na comanda destino
        Long currentCmdId = item.getCommandId();
        if (java.util.Objects.equals(currentCmdId, targetCommandId)) {
            return order;
        }

        // Merge: se existe item do mesmo produto na comanda destino, soma e deleta o origem
        Long productId = item.getProduct().getId();
        java.util.Optional<OrderItem> existingOnTarget = order.getItems().stream()
                .filter(i -> !i.getId().equals(itemId))
                .filter(i -> i.getProduct().getId().equals(productId))
                .filter(i -> java.util.Objects.equals(i.getCommandId(), targetCommandId))
                .findFirst();

        if (existingOnTarget.isPresent()) {
            existingOnTarget.get().setQuantity(existingOnTarget.get().getQuantity() + item.getQuantity());
            order.getItems().remove(item);
        } else {
            item.setCommand(target);
        }

        return orderRepository.save(order);
    }

    @Transactional
    public void setItemPackaged(Long orderId, Long itemId, boolean packaged) {
        FoodOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
        OrderItem item = order.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Item não encontrado"));
        item.setPackaged(packaged);
        orderRepository.save(order);
    }

    // ================================================================
    // BILL BREAKDOWN (split de conta por comanda)
    // ================================================================

    public BillBreakdown getBillBreakdown(Long orderId) {
        FoodOrder order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        java.util.List<com.mvt.mvt_events.jpa.OrderCommand> commands =
                orderCommandRepository.findByOrderIdOrderByDisplayNumberAsc(orderId);

        // Mesa = items compartilhados (commandId null). Tratada como comanda própria.
        java.util.List<OrderItem> mesaItems = order.getItems().stream()
                .filter(i -> i.getCommandId() == null)
                .collect(java.util.stream.Collectors.toList());
        BigDecimal mesaSubtotal = mesaItems.stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        java.util.List<CommandBreakdown> perCommand = new java.util.ArrayList<>();
        for (com.mvt.mvt_events.jpa.OrderCommand cmd : commands) {
            java.util.List<OrderItem> items = order.getItems().stream()
                    .filter(it -> cmd.getId().equals(it.getCommandId()))
                    .collect(java.util.stream.Collectors.toList());
            BigDecimal subtotal = items.stream()
                    .map(it -> it.getUnitPrice().multiply(BigDecimal.valueOf(it.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);
            perCommand.add(new CommandBreakdown(
                    cmd.getId(), cmd.getDisplayNumber(), cmd.getName(),
                    items, subtotal,
                    cmd.getStatus() != null ? cmd.getStatus().name() : "OPEN",
                    cmd.getPaymentMethod() != null ? cmd.getPaymentMethod().name() : null,
                    cmd.getPaidAt()));
        }

        MesaBreakdown mesa = new MesaBreakdown(
                mesaItems, mesaSubtotal,
                order.getMesaStatus() != null ? order.getMesaStatus().name() : "OPEN",
                order.getMesaPaymentMethod() != null ? order.getMesaPaymentMethod().name() : null,
                order.getMesaPaidAt());

        return new BillBreakdown(perCommand, mesa, order.getTotal());
    }

    public static class BillBreakdown {
        public java.util.List<CommandBreakdown> commands;
        public MesaBreakdown mesa;
        public BigDecimal grandTotal;

        public BillBreakdown(java.util.List<CommandBreakdown> commands, MesaBreakdown mesa, BigDecimal grandTotal) {
            this.commands = commands;
            this.mesa = mesa;
            this.grandTotal = grandTotal;
        }
    }

    public static class MesaBreakdown {
        public java.util.List<OrderItem> items;
        public BigDecimal subtotal;
        public String status;
        public String paymentMethod;
        public OffsetDateTime paidAt;

        public MesaBreakdown(java.util.List<OrderItem> items, BigDecimal subtotal,
                             String status, String paymentMethod, OffsetDateTime paidAt) {
            this.items = items;
            this.subtotal = subtotal;
            this.status = status;
            this.paymentMethod = paymentMethod;
            this.paidAt = paidAt;
        }
    }

    public static class CommandBreakdown {
        public Long id;
        public Integer displayNumber;
        public String name;
        public java.util.List<OrderItem> items;
        public BigDecimal subtotal;
        public String status;
        public String paymentMethod;
        public OffsetDateTime paidAt;

        public CommandBreakdown(Long id, Integer displayNumber, String name, java.util.List<OrderItem> items,
                                BigDecimal subtotal, String status, String paymentMethod, OffsetDateTime paidAt) {
            this.id = id;
            this.displayNumber = displayNumber;
            this.name = name;
            this.items = items;
            this.subtotal = subtotal;
            this.status = status;
            this.paymentMethod = paymentMethod;
            this.paidAt = paidAt;
        }
    }

    // ================================================================
    // FECHAR COMANDA / MESA (pagamento parcial)
    // ================================================================

    /**
     * Fecha uma comanda (commandId != null) ou a Mesa (commandId == null).
     * Se após o fechamento tudo estiver pago, marca o pedido como COMPLETED e libera a mesa.
     */
    @Transactional
    public FoodOrder closePartial(Long orderId, Long commandId, String paymentMethodStr) {
        FoodOrder order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        if (order.getStatus() == FoodOrder.OrderStatus.COMPLETED || order.getStatus() == FoodOrder.OrderStatus.CANCELLED) {
            throw new RuntimeException("Pedido já está " + order.getStatus().name());
        }

        PaymentMethod pm = null;
        if (paymentMethodStr != null && !paymentMethodStr.isBlank()) {
            try { pm = PaymentMethod.valueOf(paymentMethodStr); }
            catch (IllegalArgumentException e) { throw new RuntimeException("Forma de pagamento inválida: " + paymentMethodStr); }
        }

        OffsetDateTime now = OffsetDateTime.now(ZONE);

        if (commandId == null) {
            // Fechar Mesa
            if (order.getMesaStatus() == FoodOrder.MesaStatus.PAID) {
                throw new RuntimeException("Mesa já foi paga");
            }
            boolean hasMesaItems = order.getItems().stream().anyMatch(i -> i.getCommandId() == null);
            if (!hasMesaItems) {
                throw new RuntimeException("Mesa não tem itens");
            }
            order.setMesaStatus(FoodOrder.MesaStatus.PAID);
            order.setMesaPaymentMethod(pm);
            order.setMesaPaidAt(now);
        } else {
            com.mvt.mvt_events.jpa.OrderCommand cmd = orderCommandRepository.findByIdAndOrderId(commandId, orderId)
                    .orElseThrow(() -> new RuntimeException("Comanda não encontrada"));
            if (cmd.getStatus() == com.mvt.mvt_events.jpa.OrderCommand.PaymentStatus.PAID) {
                throw new RuntimeException("Comanda já foi paga");
            }
            boolean hasItems = order.getItems().stream()
                    .anyMatch(i -> commandId.equals(i.getCommandId()));
            if (!hasItems) {
                throw new RuntimeException("Comanda não tem itens");
            }
            cmd.setStatus(com.mvt.mvt_events.jpa.OrderCommand.PaymentStatus.PAID);
            cmd.setPaymentMethod(pm);
            cmd.setPaidAt(now);
            orderCommandRepository.save(cmd);
        }

        tryAutoComplete(order, now);
        return orderRepository.save(order);
    }

    /**
     * Marca o pedido como COMPLETED se tudo que tinha valor já foi pago.
     * Comandas sem itens não bloqueiam: são tratadas como "done".
     */
    private void tryAutoComplete(FoodOrder order, OffsetDateTime now) {
        java.util.List<com.mvt.mvt_events.jpa.OrderCommand> allCmds =
                orderCommandRepository.findByOrderIdOrderByDisplayNumberAsc(order.getId());
        boolean mesaDone = order.getMesaStatus() == FoodOrder.MesaStatus.PAID
                || order.getItems().stream().noneMatch(i -> i.getCommandId() == null);
        boolean allCmdsDone = allCmds.stream().allMatch(c ->
                c.getStatus() == com.mvt.mvt_events.jpa.OrderCommand.PaymentStatus.PAID
                        || order.getItems().stream().noneMatch(i -> c.getId().equals(i.getCommandId())));

        if (mesaDone && allCmdsDone) {
            order.setStatus(FoodOrder.OrderStatus.COMPLETED);
            if (order.getCompletedAt() == null) order.setCompletedAt(now);
            if (order.getPaidAt() == null) order.setPaidAt(now);
            if (order.getTable() != null) {
                RestaurantTable t = order.getTable();
                t.setStatus(RestaurantTable.TableStatus.AVAILABLE);
                restaurantTableRepository.save(t);
                order.setTable(null);
            }
        }
    }

    /**
     * Dispara a verificação de auto-complete sem passar por closePartial.
     * Útil quando todas as comandas OPEN restantes estão vazias (R$ 0,00) e não há
     * closePartial a chamar, mas o pedido deveria estar COMPLETED.
     */
    @Transactional
    public FoodOrder autoComplete(Long orderId) {
        FoodOrder order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
        if (order.getStatus() == FoodOrder.OrderStatus.COMPLETED || order.getStatus() == FoodOrder.OrderStatus.CANCELLED) {
            return order;
        }
        tryAutoComplete(order, OffsetDateTime.now(ZONE));
        return orderRepository.save(order);
    }

    // ================================================================
    // ADDONS — HELPERS
    // ================================================================

    /**
     * Cria OrderItemAddons para um OrderItem a partir de uma lista de AddonRequests.
     * Valida que cada product pertence ao mesmo clientId do pedido e está `available`.
     * Retorna o total dos addons (sum of unitPrice * quantity) a ser somado no subtotal.
     *
     * Importante: addons carregam quantity independente do parent OrderItem.quantity —
     * "4x Classic Burger + 2x Cheddar" significa 2 cheddars totais (não 2 por burger).
     */
    private BigDecimal attachAddons(OrderItem item, List<AddonRequest> addonReqs, UUID clientId, FoodOrder.OrderType orderType) {
        if (addonReqs == null || addonReqs.isEmpty()) return BigDecimal.ZERO;

        BigDecimal total = BigDecimal.ZERO;
        for (AddonRequest ar : addonReqs) {
            if (ar == null || ar.productId == null || ar.quantity <= 0) continue;
            Product addonProduct = productRepository.findById(ar.productId)
                    .orElseThrow(() -> new RuntimeException("Adicional não encontrado: " + ar.productId));
            if (!addonProduct.getClient().getId().equals(clientId)) {
                throw new RuntimeException("Adicional " + addonProduct.getName() + " não pertence a este restaurante");
            }
            if (!addonProduct.getAvailable()) {
                throw new RuntimeException("Adicional " + addonProduct.getName() + " não está disponível");
            }
            BigDecimal addonPrice = addonProduct.priceFor(orderType);
            OrderItemAddon addon = OrderItemAddon.builder()
                    .orderItem(item)
                    .product(addonProduct)
                    .quantity(ar.quantity)
                    .unitPrice(addonPrice)
                    .build();
            item.getAddons().add(addon);
            total = total.add(addonPrice.multiply(BigDecimal.valueOf(ar.quantity)));
        }
        return total;
    }

    /** Soma dos addons de um item já persistido. */
    private BigDecimal sumAddons(OrderItem item) {
        if (item.getAddons() == null || item.getAddons().isEmpty()) return BigDecimal.ZERO;
        return item.getAddons().stream()
                .map(a -> a.getUnitPrice().multiply(BigDecimal.valueOf(a.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ================================================================
    // REQUEST DTO (inner class simples)
    // ================================================================

    public static class OrderItemRequest {
        public Long productId;
        public int quantity;
        public String notes;
        /** Comanda à qual o item pertence; null = compartilhado */
        public Long commandId;
        /** Observação por item (fase 2). Coexiste com `notes` legado. */
        public String observation;
        /** Adicionais pendurados neste item. Null/vazio = sem adicionais. */
        public java.util.List<AddonRequest> addons;

        /** True quando o item tem customização (obs ou addons) — desabilita dedup. */
        public boolean hasCustomization() {
            boolean hasObs = observation != null && !observation.isBlank();
            boolean hasAddons = addons != null && !addons.isEmpty();
            return hasObs || hasAddons;
        }
    }

    /** Adicional aninhado num OrderItemRequest. */
    public static class AddonRequest {
        public Long productId;
        public int quantity;
    }
}
