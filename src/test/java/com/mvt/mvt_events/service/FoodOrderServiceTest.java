package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.*;
import com.mvt.mvt_events.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários do FoodOrderService — garante que deliveries criadas a partir
 * de pedidos food sejam idênticas às criadas pelo CRUD (mobile/FE), e que o
 * callback de status sincronize corretamente pedido ↔ delivery.
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class FoodOrderServiceTest {

    @Mock private FoodOrderRepository orderRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;
    @Mock private StoreProfileRepository storeProfileRepository;
    @Mock private DeliveryService deliveryService;
    @Mock private DeliveryStopRepository deliveryStopRepository;
    @Mock private PushNotificationService pushNotificationService;
    @Mock private SiteConfigurationService siteConfigurationService;
    @Mock private GoogleDirectionsService googleDirectionsService;

    @InjectMocks
    private FoodOrderService foodOrderService;

    // UUIDs fixos
    private final UUID clientId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID customerId = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private static final ZoneId ZONE = ZoneId.of("America/Fortaleza");

    // Coordenadas fixas (restaurante e cliente)
    private static final double STORE_LAT = -3.854;
    private static final double STORE_LNG = -40.922;
    private static final double DEST_LAT = -3.853;
    private static final double DEST_LNG = -40.920;

    // ========== Helpers ==========

    private User makeClient() {
        User u = new User();
        u.setId(clientId);
        u.setName("Tex Burger");
        u.setRole(User.Role.CLIENT);
        u.setGpsLatitude(STORE_LAT);
        u.setGpsLongitude(STORE_LNG);

        // Endereço cadastrado
        Address addr = new Address();
        addr.setStreet("Av. Constituintes");
        addr.setNumber("360");
        addr.setNeighborhood("Centro");
        addr.setLatitude(STORE_LAT);
        addr.setLongitude(STORE_LNG);
        addr.setIsDefault(true);
        addr.setUser(u);
        u.addAddress(addr);

        return u;
    }

    private User makeCustomer() {
        User u = new User();
        u.setId(customerId);
        u.setName("João Cliente");
        u.setRole(User.Role.CUSTOMER);
        u.setPhoneDdd("85");
        u.setPhoneNumber("999888777");
        u.setGpsLatitude(DEST_LAT);
        u.setGpsLongitude(DEST_LNG);
        return u;
    }

    private Product makeProduct(Long id, String name, BigDecimal price) {
        Product p = new Product();
        p.setId(id);
        p.setName(name);
        p.setPrice(price);
        return p;
    }

    private OrderItem makeItem(FoodOrder order, Product product, int qty) {
        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setQuantity(qty);
        item.setUnitPrice(product.getPrice());
        return item;
    }

    private FoodOrder makeOrder(User client, User customer) {
        Product texBacon = makeProduct(1L, "Tex Bacon", BigDecimal.valueOf(20.00));
        Product heineken = makeProduct(2L, "Heineken", BigDecimal.valueOf(12.00));
        Product panqueca = makeProduct(3L, "Panqueca de Frango", BigDecimal.valueOf(15.00));

        FoodOrder order = new FoodOrder();
        order.setId(1L);
        order.setClient(client);
        order.setCustomer(customer);
        order.setStatus(FoodOrder.OrderStatus.PREPARING);
        order.setSubtotal(BigDecimal.valueOf(67.00));
        order.setDeliveryFee(BigDecimal.valueOf(5.50));
        order.setTotal(BigDecimal.valueOf(72.50));
        order.setDeliveryAddress("R. Trinta Um de Dezembro, 121 - Ubajara, CE");
        order.setDeliveryLatitude(DEST_LAT);
        order.setDeliveryLongitude(DEST_LNG);
        order.setCreatedAt(OffsetDateTime.now(ZONE));
        order.setUpdatedAt(OffsetDateTime.now(ZONE));

        List<OrderItem> items = new ArrayList<>();
        items.add(makeItem(order, texBacon, 2));
        items.add(makeItem(order, heineken, 1));
        items.add(makeItem(order, panqueca, 1));
        order.setItems(items);

        return order;
    }

    // ================================================================
    // DELIVERY CRIADA A PARTIR DO PEDIDO — PARIDADE COM CRUD
    // ================================================================

    @Nested
    @DisplayName("markReady() → createDeliveryFromOrder() — paridade com CRUD")
    class DeliveryFromOrderParity {

        private User client;
        private User customer;
        private FoodOrder order;
        private ArgumentCaptor<Delivery> deliveryCaptor;

        @BeforeEach
        void setup() {
            client = makeClient();
            customer = makeCustomer();
            order = makeOrder(client, customer);
            deliveryCaptor = ArgumentCaptor.forClass(Delivery.class);

            when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));
            when(orderRepository.findRecentDeliveringByClient(eq(clientId), any())).thenReturn(List.of());
            when(orderRepository.save(any(FoodOrder.class))).thenAnswer(inv -> inv.getArgument(0));

            // Google Directions retorna rota simulada
            List<double[]> fakeRoute = List.of(
                    new double[]{STORE_LAT, STORE_LNG},
                    new double[]{-3.8535, -40.921},
                    new double[]{DEST_LAT, DEST_LNG}
            );
            when(googleDirectionsService.getRoute(
                    eq(STORE_LAT), eq(STORE_LNG), eq(DEST_LAT), eq(DEST_LNG), isNull()
            )).thenReturn(fakeRoute);

            // deliveryService.create() retorna a delivery recebida com id
            when(deliveryService.create(any(Delivery.class), eq(clientId), eq(clientId)))
                    .thenAnswer(inv -> {
                        Delivery d = inv.getArgument(0);
                        d.setId(100L);
                        return d;
                    });
        }

        @Test
        @DisplayName("fromAddress usa endereço cadastrado do client, não o nome")
        void fromAddressUsaEnderecoReal() {
            foodOrderService.markReady(1L, clientId);

            verify(deliveryService).create(deliveryCaptor.capture(), eq(clientId), eq(clientId));
            Delivery d = deliveryCaptor.getValue();

            assertThat(d.getFromAddress()).contains("Av. Constituintes");
            assertThat(d.getFromAddress()).doesNotContain("Tex Burger");
        }

        @Test
        @DisplayName("fromLatitude e fromLongitude vêm do Address do client")
        void fromCoordenadasDoAddress() {
            foodOrderService.markReady(1L, clientId);

            verify(deliveryService).create(deliveryCaptor.capture(), eq(clientId), eq(clientId));
            Delivery d = deliveryCaptor.getValue();

            assertThat(d.getFromLatitude()).isEqualTo(STORE_LAT);
            assertThat(d.getFromLongitude()).isEqualTo(STORE_LNG);
        }

        @Test
        @DisplayName("toAddress, toLatitude, toLongitude vêm do pedido")
        void destinoVemDoPedido() {
            foodOrderService.markReady(1L, clientId);

            verify(deliveryService).create(deliveryCaptor.capture(), eq(clientId), eq(clientId));
            Delivery d = deliveryCaptor.getValue();

            assertThat(d.getToAddress()).isEqualTo("R. Trinta Um de Dezembro, 121 - Ubajara, CE");
            assertThat(d.getToLatitude()).isEqualTo(DEST_LAT);
            assertThat(d.getToLongitude()).isEqualTo(DEST_LNG);
        }

        @Test
        @DisplayName("recipientName e recipientPhone vêm do customer")
        void destinatarioDoCustomer() {
            foodOrderService.markReady(1L, clientId);

            verify(deliveryService).create(deliveryCaptor.capture(), eq(clientId), eq(clientId));
            Delivery d = deliveryCaptor.getValue();

            assertThat(d.getRecipientName()).isEqualTo("João Cliente");
            assertThat(d.getRecipientPhone()).isEqualTo("85999888777");
        }

        @Test
        @DisplayName("itemDescription descreve os itens do pedido (quantidade x nome)")
        void itemDescriptionDescritivo() {
            foodOrderService.markReady(1L, clientId);

            verify(deliveryService).create(deliveryCaptor.capture(), eq(clientId), eq(clientId));
            Delivery d = deliveryCaptor.getValue();

            assertThat(d.getItemDescription()).startsWith("Pedido #1 — ");
            assertThat(d.getItemDescription()).contains("2x Tex Bacon");
            assertThat(d.getItemDescription()).contains("1x Heineken");
            assertThat(d.getItemDescription()).contains("1x Panqueca de Frango");
        }

        @Test
        @DisplayName("totalAmount é o subtotal do pedido (sem deliveryFee)")
        void totalAmountEhSubtotal() {
            foodOrderService.markReady(1L, clientId);

            verify(deliveryService).create(deliveryCaptor.capture(), eq(clientId), eq(clientId));
            Delivery d = deliveryCaptor.getValue();

            assertThat(d.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(67.00));
        }

        @Test
        @DisplayName("shippingFee NÃO é pré-setado — DeliveryService.create() calcula")
        void shippingFeeNaoPreSetado() {
            foodOrderService.markReady(1L, clientId);

            verify(deliveryService).create(deliveryCaptor.capture(), eq(clientId), eq(clientId));
            Delivery d = deliveryCaptor.getValue();

            assertThat(d.getShippingFee()).isNull();
        }

        @Test
        @DisplayName("distanceKm calculado via Google Directions (não haversine)")
        void distanceKmViaGoogleDirections() {
            foodOrderService.markReady(1L, clientId);

            verify(deliveryService).create(deliveryCaptor.capture(), eq(clientId), eq(clientId));
            Delivery d = deliveryCaptor.getValue();

            assertThat(d.getDistanceKm()).isNotNull();
            assertThat(d.getDistanceKm().doubleValue()).isGreaterThan(0);
            verify(googleDirectionsService).getRoute(STORE_LAT, STORE_LNG, DEST_LAT, DEST_LNG, null);
        }

        @Test
        @DisplayName("plannedRouteCoordinates preenchido com rota do Google")
        void plannedRoutePreenchido() {
            foodOrderService.markReady(1L, clientId);

            verify(deliveryService).create(deliveryCaptor.capture(), eq(clientId), eq(clientId));
            Delivery d = deliveryCaptor.getValue();

            assertThat(d.getPlannedRouteCoordinates()).isNotNull();
            assertThat(d.getPlannedRouteCoordinates()).hasSize(3);
            assertThat(d.getPlannedRouteCoordinates().get(0)).containsExactly(STORE_LAT, STORE_LNG);
        }

        @Test
        @DisplayName("fallback para haversine quando Google Directions falha")
        void fallbackHaversineQuandoGoogleFalha() {
            when(googleDirectionsService.getRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble(), isNull()))
                    .thenReturn(List.of());

            foodOrderService.markReady(1L, clientId);

            verify(deliveryService).create(deliveryCaptor.capture(), eq(clientId), eq(clientId));
            Delivery d = deliveryCaptor.getValue();

            assertThat(d.getDistanceKm()).isNotNull();
            assertThat(d.getDistanceKm().doubleValue()).isGreaterThan(0);
            assertThat(d.getPlannedRouteCoordinates()).isNull();
        }

        @Test
        @DisplayName("deliveryType é DELIVERY")
        void deliveryTypeDelivery() {
            foodOrderService.markReady(1L, clientId);

            verify(deliveryService).create(deliveryCaptor.capture(), eq(clientId), eq(clientId));
            assertThat(deliveryCaptor.getValue().getDeliveryType()).isEqualTo(Delivery.DeliveryType.DELIVERY);
        }

        @Test
        @DisplayName("preferredVehicleType é MOTORCYCLE")
        void vehicleTypeMotorcycle() {
            foodOrderService.markReady(1L, clientId);

            verify(deliveryService).create(deliveryCaptor.capture(), eq(clientId), eq(clientId));
            assertThat(deliveryCaptor.getValue().getPreferredVehicleType())
                    .isEqualTo(Delivery.PreferredVehicleType.MOTORCYCLE);
        }

        @Test
        @DisplayName("cria DeliveryStop com dados do destino (igual CRUD single-stop)")
        void criaDeliveryStop() {
            foodOrderService.markReady(1L, clientId);

            verify(deliveryService).create(deliveryCaptor.capture(), eq(clientId), eq(clientId));
            Delivery d = deliveryCaptor.getValue();

            assertThat(d.getStops()).hasSize(1);
            DeliveryStop stop = d.getStops().get(0);
            assertThat(stop.getStopOrder()).isEqualTo(1);
            assertThat(stop.getAddress()).isEqualTo("R. Trinta Um de Dezembro, 121 - Ubajara, CE");
            assertThat(stop.getLatitude()).isEqualTo(DEST_LAT);
            assertThat(stop.getLongitude()).isEqualTo(DEST_LNG);
            assertThat(stop.getRecipientName()).isEqualTo("João Cliente");
            assertThat(stop.getRecipientPhone()).isEqualTo("85999888777");
            assertThat(stop.getStatus()).isEqualTo(DeliveryStop.StopStatus.PENDING);
        }

        @Test
        @DisplayName("delivery é criada via deliveryService.create() (mesma pipeline do CRUD)")
        void usaDeliveryServiceCreate() {
            foodOrderService.markReady(1L, clientId);

            verify(deliveryService).create(any(Delivery.class), eq(clientId), eq(clientId));
        }

        @Test
        @DisplayName("client sem Address cadastrado — fallback para GPS e nome")
        void fallbackSemAddress() {
            // Remover endereços do client
            client.getAddresses().clear();

            foodOrderService.markReady(1L, clientId);

            verify(deliveryService).create(deliveryCaptor.capture(), eq(clientId), eq(clientId));
            Delivery d = deliveryCaptor.getValue();

            assertThat(d.getFromAddress()).isEqualTo("Tex Burger");
            assertThat(d.getFromLatitude()).isEqualTo(STORE_LAT);
            assertThat(d.getFromLongitude()).isEqualTo(STORE_LNG);
        }
    }

    // ================================================================
    // STATUS DO PEDIDO — SINCRONIZAÇÃO COM DELIVERY
    // ================================================================

    @Nested
    @DisplayName("markReady() — status do pedido permanece READY")
    class MarkReadyStatus {

        private ArgumentCaptor<Delivery> deliveryCaptor;

        @BeforeEach
        void setup() {
            deliveryCaptor = ArgumentCaptor.forClass(Delivery.class);
            User client = makeClient();
            User customer = makeCustomer();
            FoodOrder order = makeOrder(client, customer);

            when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));
            when(orderRepository.findRecentDeliveringByClient(eq(clientId), any())).thenReturn(List.of());
            when(orderRepository.save(any(FoodOrder.class))).thenAnswer(inv -> inv.getArgument(0));
            when(googleDirectionsService.getRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble(), isNull()))
                    .thenReturn(List.of(new double[]{STORE_LAT, STORE_LNG}, new double[]{DEST_LAT, DEST_LNG}));
            when(deliveryService.create(any(Delivery.class), eq(clientId), eq(clientId)))
                    .thenAnswer(inv -> { Delivery d = inv.getArgument(0); d.setId(100L); return d; });
        }

        @Test
        @DisplayName("pedido permanece READY após criar delivery (não pula pra DELIVERING)")
        void pedidoPermanecReady() {
            FoodOrder result = foodOrderService.markReady(1L, clientId);

            assertThat(result.getStatus()).isEqualTo(FoodOrder.OrderStatus.READY);
        }

        @Test
        @DisplayName("delivery vinculada ao pedido via delivery.order")
        void deliveryVinculada() {
            foodOrderService.markReady(1L, clientId);

            verify(deliveryService).create(deliveryCaptor.capture(), eq(clientId), eq(clientId));
            Delivery d = deliveryCaptor.getValue();
            assertThat(d.getOrder()).isNotNull();
            assertThat(d.getOrder().getId()).isEqualTo(1L);
        }
    }

    // ================================================================
    // CALLBACK — SINCRONIZAÇÃO DELIVERY → PEDIDO
    // ================================================================

    @Nested
    @DisplayName("onDeliveryStatusChanged() — callback sincroniza status")
    class StatusCallback {

        private FoodOrder order;

        @BeforeEach
        void setup() {
            order = new FoodOrder();
            order.setId(1L);
            order.setStatus(FoodOrder.OrderStatus.READY);
            when(orderRepository.save(any(FoodOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        }

        private Delivery makeDeliveryWithStatus(Delivery.DeliveryStatus status) {
            Delivery d = new Delivery();
            d.setId(100L);
            d.setStatus(status);
            return d;
        }

        @Test
        @DisplayName("delivery ACCEPTED → pedido DELIVERING")
        void acceptedToDelivering() {
            when(orderRepository.findByDeliveryId(100L)).thenReturn(Optional.of(order));

            Delivery d = makeDeliveryWithStatus(Delivery.DeliveryStatus.ACCEPTED);
            foodOrderService.onDeliveryStatusChanged(d);

            assertThat(order.getStatus()).isEqualTo(FoodOrder.OrderStatus.DELIVERING);
        }

        @Test
        @DisplayName("delivery IN_TRANSIT → pedido DELIVERING")
        void inTransitToDelivering() {
            when(orderRepository.findByDeliveryId(100L)).thenReturn(Optional.of(order));

            Delivery d = makeDeliveryWithStatus(Delivery.DeliveryStatus.IN_TRANSIT);
            foodOrderService.onDeliveryStatusChanged(d);

            assertThat(order.getStatus()).isEqualTo(FoodOrder.OrderStatus.DELIVERING);
        }

        @Test
        @DisplayName("delivery COMPLETED → pedido COMPLETED com timestamp")
        void completedToCompleted() {
            order.setStatus(FoodOrder.OrderStatus.DELIVERING);
            when(orderRepository.findByDeliveryId(100L)).thenReturn(Optional.of(order));

            Delivery d = makeDeliveryWithStatus(Delivery.DeliveryStatus.COMPLETED);
            OffsetDateTime completedAt = OffsetDateTime.now();
            d.setCompletedAt(completedAt);

            foodOrderService.onDeliveryStatusChanged(d);

            assertThat(order.getStatus()).isEqualTo(FoodOrder.OrderStatus.COMPLETED);
            assertThat(order.getCompletedAt()).isEqualTo(completedAt);
        }

        @Test
        @DisplayName("delivery CANCELLED → pedido volta pra READY")
        void cancelledToReady() {
            order.setStatus(FoodOrder.OrderStatus.DELIVERING);
            when(orderRepository.findByDeliveryId(100L)).thenReturn(Optional.of(order));

            Delivery d = makeDeliveryWithStatus(Delivery.DeliveryStatus.CANCELLED);
            d.setCancelledAt(OffsetDateTime.now());
            d.setCancellationReason("Sem courier disponível");

            foodOrderService.onDeliveryStatusChanged(d);

            assertThat(order.getStatus()).isEqualTo(FoodOrder.OrderStatus.READY);
        }

        @Test
        @DisplayName("delivery PENDING (courier desistiu) → pedido volta pra READY")
        void pendingToReady() {
            order.setStatus(FoodOrder.OrderStatus.DELIVERING);
            when(orderRepository.findByDeliveryId(100L)).thenReturn(Optional.of(order));

            Delivery d = makeDeliveryWithStatus(Delivery.DeliveryStatus.PENDING);
            foodOrderService.onDeliveryStatusChanged(d);

            assertThat(order.getStatus()).isEqualTo(FoodOrder.OrderStatus.READY);
        }

        @Test
        @DisplayName("delivery sem pedido vinculado — noop")
        void semPedidoVinculado() {
            when(orderRepository.findByDeliveryId(100L)).thenReturn(Optional.empty());

            Delivery d = makeDeliveryWithStatus(Delivery.DeliveryStatus.ACCEPTED);
            foodOrderService.onDeliveryStatusChanged(d);

            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("delivery WAITING_PAYMENT → sem mudança no pedido")
        void waitingPaymentNoop() {
            when(orderRepository.findByDeliveryId(100L)).thenReturn(Optional.of(order));

            Delivery d = makeDeliveryWithStatus(Delivery.DeliveryStatus.WAITING_PAYMENT);
            foodOrderService.onDeliveryStatusChanged(d);

            assertThat(order.getStatus()).isEqualTo(FoodOrder.OrderStatus.READY);
            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("status já igual — sem save redundante")
        void statusIgualNaoSalva() {
            order.setStatus(FoodOrder.OrderStatus.DELIVERING);
            when(orderRepository.findByDeliveryId(100L)).thenReturn(Optional.of(order));

            Delivery d = makeDeliveryWithStatus(Delivery.DeliveryStatus.IN_TRANSIT);
            foodOrderService.onDeliveryStatusChanged(d);

            // DELIVERING → DELIVERING = sem mudança
            verify(orderRepository, never()).save(any());
        }
    }

    // ================================================================
    // CREATE() — VALIDAÇÃO DE DISTÂNCIA MÍNIMA E ORIGEM FIXA
    // ================================================================

    /**
     * Cobertura do bug do Text Burger em produção: o BE estava usando
     * {@code client.getGpsLatitude()} (posição LIVE do dono) em vez das
     * coordenadas fixas do endereço cadastrado. Quando o dono abria o app
     * em outra cidade, pedidos legítimos eram bloqueados por "38m de distância".
     *
     * Estes testes amarram:
     *  1. Coordenadas do restaurante vêm SEMPRE do {@code Address} default (fixo)
     *  2. GPS do dono é usado apenas como fallback quando não há Address
     *  3. Distância é medida via Google Directions (rota rodável), não haversine
     *  4. Haversine é fallback só se a Directions API retornar -1
     *  5. Limite mínimo é configurável via {@code SiteConfiguration}; zero desliga
     */
    @Nested
    @DisplayName("create() — validação de distância usando endereço fixo do estabelecimento")
    class CreateOrderDistanceValidation {

        // Endereço fixo do restaurante em Ubajara, CE
        private static final double STORE_ADDRESS_LAT = -3.854;
        private static final double STORE_ADDRESS_LNG = -40.922;

        // GPS do dono do restaurante em Fortaleza (sobrescrito por uso recente do app)
        // — propositalmente em lugar diferente do Address, pra detectar uso indevido
        private static final double OWNER_GPS_LAT = -3.731;
        private static final double OWNER_GPS_LNG = -38.526;

        // Endereço de entrega do customer (também em Fortaleza, perto do GPS do dono)
        // Se o BE usasse GPS do dono: distância ~100m → pedido seria bloqueado (BUG)
        // Se o BE usa Address do restaurante: distância ~500km → pedido passa (CORRETO)
        private static final double CUSTOMER_DELIVERY_LAT = -3.732;
        private static final double CUSTOMER_DELIVERY_LNG = -38.527;

        private User client;
        private User customer;
        private Product product;
        private SiteConfiguration siteConfig;

        @BeforeEach
        void setup() {
            // Cliente (restaurante) com Address em Ubajara e GPS em Fortaleza
            client = new User();
            client.setId(clientId);
            client.setName("Text Burger");
            client.setRole(User.Role.CLIENT);
            client.setEnabled(true);
            client.setGpsLatitude(OWNER_GPS_LAT);
            client.setGpsLongitude(OWNER_GPS_LNG);
            // BankAccount mock — só pra passar em hasBankAccount()
            BankAccount bank = new BankAccount();
            client.setBankAccount(bank);

            Address addr = new Address();
            addr.setStreet("Av. Constituintes");
            addr.setNumber("360");
            addr.setLatitude(STORE_ADDRESS_LAT);
            addr.setLongitude(STORE_ADDRESS_LNG);
            addr.setIsDefault(true);
            addr.setUser(client);
            client.addAddress(addr);

            // Customer
            customer = new User();
            customer.setId(customerId);
            customer.setName("João Cliente");
            customer.setRole(User.Role.CUSTOMER);

            // Product do restaurante
            product = new Product();
            product.setId(1L);
            product.setName("Tex Bacon");
            product.setPrice(BigDecimal.valueOf(20.00));
            product.setAvailable(true);
            product.setClient(client);

            // SiteConfig default: 50m mínimo + campos obrigatórios pra taxa de entrega
            siteConfig = new SiteConfiguration();
            siteConfig.setMinOrderDistanceMeters(50);
            siteConfig.setPricePerKm(BigDecimal.valueOf(1.50));
            siteConfig.setMinimumShippingFee(BigDecimal.valueOf(5.00));

            // Mocks comuns
            when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));
            when(userRepository.findById(clientId)).thenReturn(Optional.of(client));
            when(storeProfileRepository.findByUserId(clientId)).thenReturn(Optional.empty());
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(siteConfigurationService.getActiveConfiguration()).thenReturn(siteConfig);
            when(orderRepository.save(any(FoodOrder.class))).thenAnswer(inv -> {
                FoodOrder o = inv.getArgument(0);
                o.setId(999L);
                return o;
            });
        }

        private List<FoodOrderService.OrderItemRequest> oneItem() {
            FoodOrderService.OrderItemRequest req = new FoodOrderService.OrderItemRequest();
            req.productId = 1L;
            req.quantity = 1;
            return List.of(req);
        }

        @Test
        @DisplayName("usa Address fixo do restaurante, não GPS do dono (bug Text Burger)")
        void usaEnderecoFixoDoClientNaoGps() {
            // Google Directions é chamado com as coords do ADDRESS do restaurante (Ubajara),
            // NÃO com o GPS do dono (Fortaleza). Se o BE passar GPS do dono,
            // o verify() abaixo vai falhar.
            when(googleDirectionsService.getDistanceMeters(
                    eq(CUSTOMER_DELIVERY_LAT), eq(CUSTOMER_DELIVERY_LNG),
                    eq(STORE_ADDRESS_LAT), eq(STORE_ADDRESS_LNG)))
                    .thenReturn(500_000); // 500 km — distância realista Fortaleza→Ubajara

            FoodOrder order = foodOrderService.create(
                    customerId, clientId, oneItem(), null,
                    "Rua 31 de Dezembro - Fortaleza/CE",
                    CUSTOMER_DELIVERY_LAT, CUSTOMER_DELIVERY_LNG);

            assertThat(order).isNotNull();
            assertThat(order.getStatus()).isEqualTo(FoodOrder.OrderStatus.PLACED);

            // Prova que foi a Directions API com as coords do Address, não do GPS
            verify(googleDirectionsService).getDistanceMeters(
                    eq(CUSTOMER_DELIVERY_LAT), eq(CUSTOMER_DELIVERY_LNG),
                    eq(STORE_ADDRESS_LAT), eq(STORE_ADDRESS_LNG));
            verify(googleDirectionsService, never()).getDistanceMeters(
                    anyDouble(), anyDouble(),
                    eq(OWNER_GPS_LAT), eq(OWNER_GPS_LNG));
        }

        @Test
        @DisplayName("cai pro GPS do dono quando Address não tem lat/lng")
        void caiNoGpsQuandoAddressSemCoords() {
            // Remove coords do Address — simula restaurante sem endereço geocodificado
            Address addr = client.getAddress();
            addr.setLatitude(null);
            addr.setLongitude(null);

            when(googleDirectionsService.getDistanceMeters(
                    anyDouble(), anyDouble(),
                    eq(OWNER_GPS_LAT), eq(OWNER_GPS_LNG)))
                    .thenReturn(5_000);

            FoodOrder order = foodOrderService.create(
                    customerId, clientId, oneItem(), null,
                    "destino qualquer", CUSTOMER_DELIVERY_LAT, CUSTOMER_DELIVERY_LNG);

            assertThat(order).isNotNull();
            verify(googleDirectionsService).getDistanceMeters(
                    eq(CUSTOMER_DELIVERY_LAT), eq(CUSTOMER_DELIVERY_LNG),
                    eq(OWNER_GPS_LAT), eq(OWNER_GPS_LNG));
        }

        @Test
        @DisplayName("bloqueia pedido quando distância Directions < mínimo")
        void bloqueiaQuandoDistanciaMenorQueMinimo() {
            when(googleDirectionsService.getDistanceMeters(
                    anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(30); // 30m — abaixo do mínimo de 50m

            assertThatThrownBy(() -> foodOrderService.create(
                    customerId, clientId, oneItem(), null,
                    "destino perto", CUSTOMER_DELIVERY_LAT, CUSTOMER_DELIVERY_LNG))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("muito próximo")
                    .hasMessageContaining("30m");

            verify(orderRepository, never()).save(any(FoodOrder.class));
        }

        @Test
        @DisplayName("permite pedido quando minOrderDistanceMeters = 0 (validação desligada)")
        void permiteQuandoMinZero() {
            siteConfig.setMinOrderDistanceMeters(0);

            FoodOrder order = foodOrderService.create(
                    customerId, clientId, oneItem(), null,
                    "destino super perto",
                    STORE_ADDRESS_LAT + 0.00001, STORE_ADDRESS_LNG + 0.00001);

            assertThat(order).isNotNull();
            // Directions nem deve ser chamada quando min=0
            verify(googleDirectionsService, never()).getDistanceMeters(
                    anyDouble(), anyDouble(), anyDouble(), anyDouble());
        }

        @Test
        @DisplayName("usa Google Directions primeiro, haversine só como fallback")
        void usaDirectionsAntesDeHaversine() {
            when(googleDirectionsService.getDistanceMeters(
                    anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(10_000); // 10 km

            foodOrderService.create(
                    customerId, clientId, oneItem(), null,
                    "destino", CUSTOMER_DELIVERY_LAT, CUSTOMER_DELIVERY_LNG);

            verify(googleDirectionsService).getDistanceMeters(
                    anyDouble(), anyDouble(), anyDouble(), anyDouble());
        }

        @Test
        @DisplayName("fallback pro haversine quando Directions retorna -1")
        void fallbackHaversineQuandoDirectionsFalha() {
            // Directions indisponível (API down ou key não configurada)
            when(googleDirectionsService.getDistanceMeters(
                    anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(-1);

            // Coords realistas: Fortaleza → Ubajara em linha reta = centenas de km
            // Haversine vai retornar algo >> 50m, então a validação passa
            FoodOrder order = foodOrderService.create(
                    customerId, clientId, oneItem(), null,
                    "destino distante", CUSTOMER_DELIVERY_LAT, CUSTOMER_DELIVERY_LNG);

            assertThat(order).isNotNull();
            verify(googleDirectionsService).getDistanceMeters(
                    anyDouble(), anyDouble(), anyDouble(), anyDouble());
        }

        @Test
        @DisplayName("exige coordenadas de entrega — lança se ambas null")
        void exigeCoordenadasDeEntrega() {
            assertThatThrownBy(() -> foodOrderService.create(
                    customerId, clientId, oneItem(), null,
                    "endereço sem coords", null, null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("endereço de entrega");
        }
    }
}
