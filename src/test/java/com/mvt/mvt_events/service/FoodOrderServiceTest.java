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
}
