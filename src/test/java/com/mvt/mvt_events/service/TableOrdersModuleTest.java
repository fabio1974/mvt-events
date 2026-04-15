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
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class TableOrdersModuleTest {

    // ================================================================
    // RestaurantTableService tests
    // ================================================================

    @Nested
    @DisplayName("RestaurantTableService")
    class TableServiceTests {

        @Mock private RestaurantTableRepository tableRepository;
        @Mock private UserRepository userRepository;
        @InjectMocks private RestaurantTableService tableService;

        private final UUID clientId = UUID.randomUUID();

        private User makeClient() {
            User u = new User();
            u.setId(clientId);
            u.setName("Tex Burger");
            u.setRole(User.Role.CLIENT);
            return u;
        }

        @Test
        @DisplayName("Criar mesa com sucesso")
        void criarMesa() {
            User client = makeClient();
            when(userRepository.findById(clientId)).thenReturn(Optional.of(client));
            when(tableRepository.existsByClientIdAndNumber(clientId, 1)).thenReturn(false);
            when(tableRepository.save(any())).thenAnswer(inv -> {
                RestaurantTable t = inv.getArgument(0);
                t.setId(1L);
                return t;
            });

            RestaurantTable table = tableService.create(clientId, 1, "VIP", 4);

            assertThat(table.getNumber()).isEqualTo(1);
            assertThat(table.getLabel()).isEqualTo("VIP");
            assertThat(table.getSeats()).isEqualTo(4);
            assertThat(table.getActive()).isTrue();
        }

        @Test
        @DisplayName("Rejeitar mesa duplicada")
        void rejeitarMesaDuplicada() {
            User client = makeClient();
            when(userRepository.findById(clientId)).thenReturn(Optional.of(client));
            when(tableRepository.existsByClientIdAndNumber(clientId, 1)).thenReturn(true);

            assertThatThrownBy(() -> tableService.create(clientId, 1, null, null))
                    .hasMessageContaining("já existe");
        }

        @Test
        @DisplayName("Rejeitar se não é CLIENT")
        void rejeitarNaoClient() {
            User courier = new User();
            courier.setId(clientId);
            courier.setRole(User.Role.COURIER);
            when(userRepository.findById(clientId)).thenReturn(Optional.of(courier));

            assertThatThrownBy(() -> tableService.create(clientId, 1, null, null))
                    .hasMessageContaining("Apenas estabelecimentos");
        }

        @Test
        @DisplayName("Criar mesas em lote")
        void criarEmLote() {
            User client = makeClient();
            when(userRepository.findById(clientId)).thenReturn(Optional.of(client));
            when(tableRepository.existsByClientIdAndNumber(eq(clientId), anyInt())).thenReturn(false);
            when(tableRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            List<RestaurantTable> tables = tableService.createBatch(clientId, 1, 10, 4);

            assertThat(tables).hasSize(10);
            verify(tableRepository, times(10)).save(any());
        }

        @Test
        @DisplayName("Atualizar mesa — validar ownership")
        void atualizarValidaOwnership() {
            User client = makeClient();
            User otherClient = new User();
            otherClient.setId(UUID.randomUUID());

            RestaurantTable table = new RestaurantTable();
            table.setId(1L);
            table.setClient(otherClient);
            when(tableRepository.findById(1L)).thenReturn(Optional.of(table));

            assertThatThrownBy(() -> tableService.update(1L, clientId, "VIP", null, null))
                    .hasMessageContaining("não pertence");
        }
    }

    // ================================================================
    // FoodOrderService — pedido de mesa não dispara delivery
    // ================================================================

    @Nested
    @DisplayName("FoodOrderService — TABLE orders")
    class TableOrderTests {

        @Mock private FoodOrderRepository orderRepository;
        @Mock private ProductRepository productRepository;
        @Mock private UserRepository userRepository;
        @Mock private StoreProfileRepository storeProfileRepository;
        @Mock private DeliveryService deliveryService;
        @Mock private DeliveryStopRepository deliveryStopRepository;
        @Mock private PushNotificationService pushNotificationService;
        @Mock private SiteConfigurationService siteConfigurationService;
        @Mock private GoogleDirectionsService googleDirectionsService;
        @Mock private RestaurantTableRepository restaurantTableRepository;
        @InjectMocks private FoodOrderService foodOrderService;

        private final UUID clientId = UUID.randomUUID();
        private final UUID waiterId = UUID.randomUUID();

        private User makeClient() {
            User u = new User();
            u.setId(clientId);
            u.setName("Tex Burger");
            u.setRole(User.Role.CLIENT);
            return u;
        }

        private User makeWaiter() {
            User u = new User();
            u.setId(waiterId);
            u.setName("João Garçom");
            u.setRole(User.Role.WAITER);
            return u;
        }

        @Test
        @DisplayName("Criar pedido de mesa com sucesso")
        void criarPedidoMesa() {
            User client = makeClient();
            User waiter = makeWaiter();
            StoreProfile store = StoreProfile.builder().tableOrdersEnabled(true).build();
            RestaurantTable table = RestaurantTable.builder()
                    .id(1L).client(client).number(5).active(true).build();
            Product product = new Product();
            product.setId(1L);
            product.setName("Tex Bacon");
            product.setPrice(BigDecimal.valueOf(20.00));
            product.setClient(client);
            product.setAvailable(true);

            when(userRepository.findById(waiterId)).thenReturn(Optional.of(waiter));
            when(userRepository.findById(clientId)).thenReturn(Optional.of(client));
            when(storeProfileRepository.findByUserId(clientId)).thenReturn(Optional.of(store));
            when(restaurantTableRepository.findById(1L)).thenReturn(Optional.of(table));
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(orderRepository.save(any())).thenAnswer(inv -> {
                FoodOrder o = inv.getArgument(0);
                o.setId(1L);
                return o;
            });

            FoodOrderService.OrderItemRequest item = new FoodOrderService.OrderItemRequest();
            item.productId = 1L;
            item.quantity = 2;

            FoodOrder order = foodOrderService.createTableOrder(
                    waiterId, clientId, 1L, List.of(item), "Sem cebola");

            assertThat(order.getOrderType()).isEqualTo(FoodOrder.OrderType.TABLE);
            assertThat(order.getWaiter()).isEqualTo(waiter);
            assertThat(order.getTable()).isEqualTo(table);
            assertThat(order.getDeliveryFee()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(order.getSubtotal()).isEqualByComparingTo(BigDecimal.valueOf(40.00));
            assertThat(order.getTotal()).isEqualByComparingTo(BigDecimal.valueOf(40.00));
        }

        @Test
        @DisplayName("Rejeitar se módulo de mesas desabilitado")
        void rejeitarModuloDesabilitado() {
            User waiter = makeWaiter();
            User client = makeClient();
            StoreProfile store = StoreProfile.builder().tableOrdersEnabled(false).build();

            when(userRepository.findById(waiterId)).thenReturn(Optional.of(waiter));
            when(userRepository.findById(clientId)).thenReturn(Optional.of(client));
            when(storeProfileRepository.findByUserId(clientId)).thenReturn(Optional.of(store));

            assertThatThrownBy(() -> foodOrderService.createTableOrder(
                    waiterId, clientId, null, List.of(), null))
                    .hasMessageContaining("não está habilitado");
        }

        @Test
        @DisplayName("Rejeitar se não é WAITER")
        void rejeitarNaoWaiter() {
            User customer = new User();
            customer.setId(waiterId);
            customer.setRole(User.Role.CUSTOMER);
            when(userRepository.findById(waiterId)).thenReturn(Optional.of(customer));

            assertThatThrownBy(() -> foodOrderService.createTableOrder(
                    waiterId, clientId, null, List.of(), null))
                    .hasMessageContaining("Apenas garçons");
        }

        @Test
        @DisplayName("markReady de TABLE order NÃO cria delivery")
        void markReadyTableSemDelivery() {
            User client = makeClient();
            User waiter = makeWaiter();
            FoodOrder order = new FoodOrder();
            order.setId(1L);
            order.setClient(client);
            order.setCustomer(waiter);
            order.setWaiter(waiter);
            order.setOrderType(FoodOrder.OrderType.TABLE);
            order.setStatus(FoodOrder.OrderStatus.PREPARING);
            order.setSubtotal(BigDecimal.valueOf(40.00));
            order.setDeliveryFee(BigDecimal.ZERO);
            order.setTotal(BigDecimal.valueOf(40.00));
            order.setItems(new ArrayList<>());

            when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));
            when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            FoodOrder result = foodOrderService.markReady(1L, clientId);

            assertThat(result.getStatus()).isEqualTo(FoodOrder.OrderStatus.READY);
            // NÃO deve ter chamado deliveryService
            verify(deliveryService, never()).create(any(), any(), any());
        }

        @Test
        @DisplayName("markReady de DELIVERY order continua criando delivery")
        void markReadyDeliveryCriaDelivery() {
            User client = makeClient();
            User customer = new User();
            customer.setId(UUID.randomUUID());
            customer.setName("Cliente");
            customer.setRole(User.Role.CUSTOMER);
            customer.setPhoneDdd("85");
            customer.setPhoneNumber("999888777");

            Address addr = new Address();
            addr.setStreet("Rua A");
            addr.setNumber("100");
            addr.setLatitude(-3.85);
            addr.setLongitude(-40.92);
            addr.setIsDefault(true);
            addr.setUser(client);
            client.addAddress(addr);

            FoodOrder order = new FoodOrder();
            order.setId(2L);
            order.setClient(client);
            order.setCustomer(customer);
            order.setOrderType(FoodOrder.OrderType.DELIVERY);
            order.setStatus(FoodOrder.OrderStatus.PREPARING);
            order.setSubtotal(BigDecimal.valueOf(50.00));
            order.setDeliveryFee(BigDecimal.valueOf(5.50));
            order.setTotal(BigDecimal.valueOf(55.50));
            order.setDeliveryAddress("Rua B, 200");
            order.setDeliveryLatitude(-3.86);
            order.setDeliveryLongitude(-40.93);
            order.setItems(new ArrayList<>());

            when(orderRepository.findByIdWithItems(2L)).thenReturn(Optional.of(order));
            when(orderRepository.findRecentDeliveringByClient(eq(clientId), any())).thenReturn(List.of());
            when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(googleDirectionsService.getRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble(), isNull()))
                    .thenReturn(List.of(new double[]{-3.85, -40.92}, new double[]{-3.86, -40.93}));
            when(deliveryService.create(any(), any(), any())).thenAnswer(inv -> {
                Delivery d = inv.getArgument(0);
                d.setId(100L);
                return d;
            });

            FoodOrder result = foodOrderService.markReady(2L, clientId);

            verify(deliveryService).create(any(Delivery.class), eq(clientId), eq(clientId));
        }
    }

    // ================================================================
    // OrderType enum
    // ================================================================

    @Nested
    @DisplayName("OrderType enum")
    class OrderTypeTests {

        @Test
        @DisplayName("DELIVERY e TABLE existem")
        void enumValues() {
            assertThat(FoodOrder.OrderType.values()).containsExactly(
                    FoodOrder.OrderType.DELIVERY, FoodOrder.OrderType.TABLE);
        }

        @Test
        @DisplayName("WAITER role existe")
        void waiterRoleExists() {
            assertThat(User.Role.valueOf("WAITER")).isEqualTo(User.Role.WAITER);
        }
    }
}
