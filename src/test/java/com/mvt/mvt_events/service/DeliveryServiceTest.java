package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.*;
import com.mvt.mvt_events.jpa.CustomerPaymentPreference.PreferredPaymentType;
import com.mvt.mvt_events.payment.service.PagarMeService;
import com.mvt.mvt_events.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários do DeliveryService — cobre fluxo de criação, aceitação,
 * coleta, conclusão e cancelamento de entregas. Validações de roles e regras de negócio.
 */
@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @Mock private DeliveryRepository deliveryRepository;
    @Mock private UserRepository userRepository;
    @Mock private DeliveryNotificationService deliveryNotificationService;
    @Mock private com.mvt.mvt_events.repository.OrganizationRepository organizationRepository;
    @Mock private EmploymentContractRepository employmentContractRepository;
    @Mock private ClientContractRepository clientContractRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private SiteConfigurationService siteConfigurationService;
    @Mock private SpecialZoneService specialZoneService;
    @Mock private PaymentRepository paymentRepository;
    @Mock private CustomerPaymentPreferenceService preferenceService;
    @Mock private CustomerCardService cardService;
    @Mock private PagarMeService pagarMeService;
    @Mock private PaymentService paymentService;
    @Mock private PushNotificationService pushNotificationService;
    @Mock private DeliveryStopRepository deliveryStopRepository;

    @InjectMocks
    private DeliveryService deliveryService;

    // ========== Helpers ==========

    private User makeUser(UUID id, String name, User.Role role) {
        User u = new User();
        u.setId(id);
        u.setName(name);
        u.setRole(role);
        u.setUsername(name.toLowerCase().replace(" ", ".") + "@zapi10.com");
        u.setEnabled(true);
        return u;
    }

    private SiteConfiguration defaultConfig() {
        SiteConfiguration c = new SiteConfiguration();
        c.setPricePerKm(BigDecimal.valueOf(2.50));
        c.setMinimumShippingFee(BigDecimal.valueOf(5.00));
        c.setCarPricePerKm(BigDecimal.valueOf(3.50));
        c.setCarMinimumShippingFee(BigDecimal.valueOf(8.00));
        c.setAdditionalStopFee(BigDecimal.valueOf(2.00));
        c.setDangerFeePercentage(BigDecimal.valueOf(20));
        c.setHighIncomeFeePercentage(BigDecimal.valueOf(10));
        return c;
    }

    private Delivery makeDelivery(Long id, User client) {
        Delivery d = new Delivery();
        d.setId(id);
        d.setClient(client);
        d.setStatus(Delivery.DeliveryStatus.PENDING);
        d.setFromAddress("Rua A, 100");
        d.setToAddress("Rua B, 200");
        d.setFromLatitude(-3.69);
        d.setFromLongitude(-40.35);
        d.setToLatitude(-3.70);
        d.setToLongitude(-40.36);
        d.setDistanceKm(BigDecimal.valueOf(5.0));
        return d;
    }

    // UUIDs fixos para consistência
    private final UUID adminId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID clientId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private final UUID customerId = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private final UUID courierId = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private final UUID organizerId = UUID.fromString("00000000-0000-0000-0000-000000000005");

    // ================================================================
    // CREATE
    // ================================================================

    @Nested
    @DisplayName("create() — Criação de entregas")
    class CreateTests {

        @Test
        @DisplayName("CLIENT cria entrega para si mesmo — sucesso")
        void clientCriaParaSiMesmo() {
            User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            Delivery delivery = makeDelivery(null, null);
            SiteConfiguration config = defaultConfig();

            when(userRepository.findById(clientId)).thenReturn(Optional.of(client));
            when(siteConfigurationService.getActiveConfiguration()).thenReturn(config);
            when(specialZoneService.findNearestZone(anyDouble(), anyDouble())).thenReturn(Optional.empty());
            when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> {
                Delivery d = inv.getArgument(0);
                d.setId(1L);
                return d;
            });

            Delivery result = deliveryService.create(delivery, clientId, clientId);

            assertThat(result.getClient()).isEqualTo(client);
            assertThat(result.getStatus()).isEqualTo(Delivery.DeliveryStatus.PENDING);
            assertThat(result.getDeliveryType()).isEqualTo(Delivery.DeliveryType.DELIVERY);
            assertThat(result.getTrackingToken()).isNotNull();
            assertThat(result.getTrackingTokenExpiresAt()).isAfter(OffsetDateTime.now());
            assertThat(result.getShippingFee()).isNotNull();
            verify(deliveryNotificationService).notifyAvailableDrivers(any());
        }

        @Test
        @DisplayName("ADMIN cria entrega para qualquer CLIENT — sucesso")
        void adminCriaParaCliente() {
            User admin = makeUser(adminId, "Admin", User.Role.ADMIN);
            User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            Delivery delivery = makeDelivery(null, null);

            when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
            when(userRepository.findById(clientId)).thenReturn(Optional.of(client));
            when(siteConfigurationService.getActiveConfiguration()).thenReturn(defaultConfig());
            when(specialZoneService.findNearestZone(anyDouble(), anyDouble())).thenReturn(Optional.empty());
            when(deliveryRepository.save(any())).thenAnswer(inv -> {
                Delivery d = inv.getArgument(0);
                d.setId(2L);
                return d;
            });

            Delivery result = deliveryService.create(delivery, adminId, clientId);

            assertThat(result.getClient()).isEqualTo(client);
            assertThat(result.getStatus()).isEqualTo(Delivery.DeliveryStatus.PENDING);
        }

        @Test
        @DisplayName("COURIER não pode criar entregas")
        void courierNaoPodeCriar() {
            User courier = makeUser(courierId, "Pedro Moto", User.Role.COURIER);
            User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);

            when(userRepository.findById(courierId)).thenReturn(Optional.of(courier));
            when(userRepository.findById(clientId)).thenReturn(Optional.of(client));

            assertThatThrownBy(() -> deliveryService.create(new Delivery(), courierId, clientId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("COURIER não pode criar entregas");
        }

        @Test
        @DisplayName("ORGANIZER não pode criar entregas")
        void organizerNaoPodeCriar() {
            User organizer = makeUser(organizerId, "Ana Gerente", User.Role.ORGANIZER);
            User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);

            when(userRepository.findById(organizerId)).thenReturn(Optional.of(organizer));
            when(userRepository.findById(clientId)).thenReturn(Optional.of(client));

            assertThatThrownBy(() -> deliveryService.create(new Delivery(), organizerId, clientId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("ORGANIZER não pode criar entregas");
        }

        @Test
        @DisplayName("CLIENT não pode criar entrega para outro CLIENT")
        void clientNaoPodeCriarParaOutro() {
            UUID outroClientId = UUID.randomUUID();
            User client1 = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            User client2 = makeUser(outroClientId, "Restaurante Y", User.Role.CLIENT);

            when(userRepository.findById(clientId)).thenReturn(Optional.of(client1));
            when(userRepository.findById(outroClientId)).thenReturn(Optional.of(client2));

            assertThatThrownBy(() -> deliveryService.create(new Delivery(), clientId, outroClientId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("só pode criar entregas para si mesmo");
        }

        @Test
        @DisplayName("Destinatário com role ORGANIZER é rejeitado")
        void destinatarioOrganizerRejeitado() {
            User admin = makeUser(adminId, "Admin", User.Role.ADMIN);
            User organizer = makeUser(organizerId, "Ana Gerente", User.Role.ORGANIZER);

            when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
            when(userRepository.findById(organizerId)).thenReturn(Optional.of(organizer));

            assertThatThrownBy(() -> deliveryService.create(new Delivery(), adminId, organizerId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("deve ser um CLIENT ou CUSTOMER");
        }

        @Test
        @DisplayName("Frete moto calculado corretamente (distância × preço/km)")
        void freteCalculadoMoto() {
            User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            Delivery delivery = makeDelivery(null, null);
            delivery.setDistanceKm(BigDecimal.valueOf(10.0));
            SiteConfiguration config = defaultConfig();

            when(userRepository.findById(clientId)).thenReturn(Optional.of(client));
            when(siteConfigurationService.getActiveConfiguration()).thenReturn(config);
            when(specialZoneService.findNearestZone(anyDouble(), anyDouble())).thenReturn(Optional.empty());
            when(deliveryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Delivery result = deliveryService.create(delivery, clientId, clientId);

            // 10km × R$2.50 = R$25.00
            assertThat(result.getShippingFee()).isEqualByComparingTo("25.00");
        }

        @Test
        @DisplayName("Frete não pode ser menor que o mínimo")
        void freteMinimoAplicado() {
            User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            Delivery delivery = makeDelivery(null, null);
            delivery.setDistanceKm(BigDecimal.valueOf(1.0)); // 1km × 2.50 = 2.50 < 5.00 (mínimo)

            when(userRepository.findById(clientId)).thenReturn(Optional.of(client));
            when(siteConfigurationService.getActiveConfiguration()).thenReturn(defaultConfig());
            when(specialZoneService.findNearestZone(anyDouble(), anyDouble())).thenReturn(Optional.empty());
            when(deliveryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Delivery result = deliveryService.create(delivery, clientId, clientId);

            assertThat(result.getShippingFee()).isEqualByComparingTo("5.00");
        }

        @Test
        @DisplayName("Frete carro usa preço/km e mínimo de carro")
        void freteCarroCalculado() {
            User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            Delivery delivery = makeDelivery(null, null);
            delivery.setDistanceKm(BigDecimal.valueOf(10.0));
            delivery.setPreferredVehicleType(Delivery.PreferredVehicleType.CAR);

            when(userRepository.findById(clientId)).thenReturn(Optional.of(client));
            when(siteConfigurationService.getActiveConfiguration()).thenReturn(defaultConfig());
            when(specialZoneService.findNearestZone(anyDouble(), anyDouble())).thenReturn(Optional.empty());
            when(deliveryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Delivery result = deliveryService.create(delivery, clientId, clientId);

            // 10km × R$3.50 = R$35.00
            assertThat(result.getShippingFee()).isEqualByComparingTo("35.00");
        }

        @Test
        @DisplayName("CUSTOMER não pode criar multi-stop")
        void customerNaoPodeMultistop() {
            User customer = makeUser(customerId, "João", User.Role.CUSTOMER);
            Delivery delivery = makeDelivery(null, null);
            List<DeliveryStop> stops = new ArrayList<>();
            stops.add(new DeliveryStop());
            stops.add(new DeliveryStop());
            delivery.setStops(stops);

            when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));

            assertThatThrownBy(() -> deliveryService.create(delivery, customerId, customerId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Apenas estabelecimentos");
        }

        @Test
        @DisplayName("Token de rastreamento é gerado na criação")
        void trackingTokenGerado() {
            User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            Delivery delivery = makeDelivery(null, null);

            when(userRepository.findById(clientId)).thenReturn(Optional.of(client));
            when(siteConfigurationService.getActiveConfiguration()).thenReturn(defaultConfig());
            when(specialZoneService.findNearestZone(anyDouble(), anyDouble())).thenReturn(Optional.empty());
            when(deliveryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Delivery result = deliveryService.create(delivery, clientId, clientId);

            assertThat(result.getTrackingToken()).isNotNull();
            assertThat(result.getTrackingTokenExpiresAt()).isNotNull();
            // Token expira em ~48h
            assertThat(result.getTrackingTokenExpiresAt()).isBefore(OffsetDateTime.now().plusHours(49));
            assertThat(result.getTrackingTokenExpiresAt()).isAfter(OffsetDateTime.now().plusHours(47));
        }
    }

    // ================================================================
    // CANCEL
    // ================================================================

    @Nested
    @DisplayName("cancel() — Cancelamento de entregas")
    class CancelTests {

        @Test
        @DisplayName("Cancela entrega PENDING com sucesso")
        void cancelaPending() {
            Delivery delivery = makeDelivery(1L, makeUser(clientId, "X", User.Role.CLIENT));
            delivery.setStatus(Delivery.DeliveryStatus.PENDING);

            when(deliveryRepository.findByIdWithJoins(1L)).thenReturn(Optional.of(delivery));
            when(deliveryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Delivery result = deliveryService.cancel(1L, null, "Cliente desistiu");

            assertThat(result.getStatus()).isEqualTo(Delivery.DeliveryStatus.CANCELLED);
            assertThat(result.getCancelledAt()).isNotNull();
            assertThat(result.getCancellationReason()).isEqualTo("Cliente desistiu");
        }

        @Test
        @DisplayName("Cancela entrega ACCEPTED remove courier")
        void cancelaAcceptedRemoveCourier() {
            User courier = makeUser(courierId, "Pedro", User.Role.COURIER);
            Delivery delivery = makeDelivery(1L, makeUser(clientId, "X", User.Role.CLIENT));
            delivery.setStatus(Delivery.DeliveryStatus.ACCEPTED);
            delivery.setCourier(courier);
            courier.setCurrentDeliveryId(1L);

            when(deliveryRepository.findByIdWithJoins(1L)).thenReturn(Optional.of(delivery));
            when(deliveryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Delivery result = deliveryService.cancel(1L, null, "Motivo");

            assertThat(result.getCourier()).isNull();
            assertThat(result.getOrganizer()).isNull();
            assertThat(courier.getCurrentDeliveryId()).isNull();
            verify(userRepository).save(courier);
        }

        @Test
        @DisplayName("Não pode cancelar entrega COMPLETED")
        void naoCancelaCompleted() {
            Delivery delivery = makeDelivery(1L, makeUser(clientId, "X", User.Role.CLIENT));
            delivery.setStatus(Delivery.DeliveryStatus.COMPLETED);

            when(deliveryRepository.findByIdWithJoins(1L)).thenReturn(Optional.of(delivery));

            assertThatThrownBy(() -> deliveryService.cancel(1L, null, "Motivo"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Não é possível cancelar delivery completada");
        }

        @Test
        @DisplayName("Não pode cancelar entrega já cancelada")
        void naoCancelaJaCancelada() {
            Delivery delivery = makeDelivery(1L, makeUser(clientId, "X", User.Role.CLIENT));
            delivery.setStatus(Delivery.DeliveryStatus.CANCELLED);

            when(deliveryRepository.findByIdWithJoins(1L)).thenReturn(Optional.of(delivery));

            assertThatThrownBy(() -> deliveryService.cancel(1L, null, "Motivo"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Delivery já está cancelada");
        }
    }

    // ================================================================
    // COMPLETE
    // ================================================================

    @Nested
    @DisplayName("complete() — Conclusão de entregas")
    class CompleteTests {

        @Test
        @DisplayName("Completa entrega IN_TRANSIT com sucesso")
        void completaInTransit() {
            User courier = makeUser(courierId, "Pedro", User.Role.COURIER);
            Delivery delivery = makeDelivery(1L, makeUser(clientId, "X", User.Role.CLIENT));
            delivery.setStatus(Delivery.DeliveryStatus.IN_TRANSIT);
            delivery.setCourier(courier);
            courier.setCurrentDeliveryId(1L);

            when(deliveryRepository.findByIdWithJoins(1L)).thenReturn(Optional.of(delivery));
            when(deliveryRepository.getRouteDistanceMeters(1L)).thenReturn(null);
            when(deliveryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Delivery result = deliveryService.complete(1L, courierId);

            assertThat(result.getStatus()).isEqualTo(Delivery.DeliveryStatus.COMPLETED);
            assertThat(result.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("Retorna sem erro se já está COMPLETED (idempotente)")
        void completaIdempotente() {
            User courier = makeUser(courierId, "Pedro", User.Role.COURIER);
            Delivery delivery = makeDelivery(1L, makeUser(clientId, "X", User.Role.CLIENT));
            delivery.setStatus(Delivery.DeliveryStatus.COMPLETED);
            delivery.setCourier(courier);

            when(deliveryRepository.findByIdWithJoins(1L)).thenReturn(Optional.of(delivery));

            Delivery result = deliveryService.complete(1L, courierId);

            assertThat(result.getStatus()).isEqualTo(Delivery.DeliveryStatus.COMPLETED);
            verify(deliveryRepository, never()).save(any());
        }

        @Test
        @DisplayName("Não pode completar se status não é IN_TRANSIT")
        void naoCompletaSeNaoInTransit() {
            User courier = makeUser(courierId, "Pedro", User.Role.COURIER);
            Delivery delivery = makeDelivery(1L, makeUser(clientId, "X", User.Role.CLIENT));
            delivery.setStatus(Delivery.DeliveryStatus.ACCEPTED);
            delivery.setCourier(courier);

            when(deliveryRepository.findByIdWithJoins(1L)).thenReturn(Optional.of(delivery));

            assertThatThrownBy(() -> deliveryService.complete(1L, courierId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Status inválido para completar");
        }

        @Test
        @DisplayName("Courier errado não pode completar")
        void courierErradoNaoCompleta() {
            UUID outroCourierId = UUID.randomUUID();
            User courier = makeUser(courierId, "Pedro", User.Role.COURIER);
            Delivery delivery = makeDelivery(1L, makeUser(clientId, "X", User.Role.CLIENT));
            delivery.setStatus(Delivery.DeliveryStatus.IN_TRANSIT);
            delivery.setCourier(courier);

            when(deliveryRepository.findByIdWithJoins(1L)).thenReturn(Optional.of(delivery));

            assertThatThrownBy(() -> deliveryService.complete(1L, outroCourierId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Delivery não pertence a este courier");
        }
    }

    // ================================================================
    // CONFIRM PICKUP
    // ================================================================

    @Nested
    @DisplayName("confirmPickup() — Confirmar coleta")
    class ConfirmPickupTests {

        @Test
        @DisplayName("Courier errado não pode confirmar coleta")
        void courierErrado() {
            UUID outroCourierId = UUID.randomUUID();
            User courier = makeUser(courierId, "Pedro", User.Role.COURIER);
            Delivery delivery = makeDelivery(1L, makeUser(clientId, "X", User.Role.CLIENT));
            delivery.setStatus(Delivery.DeliveryStatus.ACCEPTED);
            delivery.setCourier(courier);

            when(deliveryRepository.findByIdWithJoins(1L)).thenReturn(Optional.of(delivery));

            assertThatThrownBy(() -> deliveryService.confirmPickup(1L, outroCourierId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Delivery não pertence a este courier");
        }

        @Test
        @DisplayName("Não pode confirmar coleta se status não é ACCEPTED")
        void statusInvalido() {
            User courier = makeUser(courierId, "Pedro", User.Role.COURIER);
            Delivery delivery = makeDelivery(1L, makeUser(clientId, "X", User.Role.CLIENT));
            delivery.setStatus(Delivery.DeliveryStatus.PENDING);
            delivery.setCourier(courier);

            when(deliveryRepository.findByIdWithJoins(1L)).thenReturn(Optional.of(delivery));

            assertThatThrownBy(() -> deliveryService.confirmPickup(1L, courierId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Status inválido");
        }
    }

    // ================================================================
    // FIND
    // ================================================================

    @Nested
    @DisplayName("findById() — Busca por ID")
    class FindTests {

        @Test
        @DisplayName("Encontra delivery existente")
        void encontra() {
            Delivery delivery = makeDelivery(1L, makeUser(clientId, "X", User.Role.CLIENT));
            when(deliveryRepository.findByIdWithJoins(1L)).thenReturn(Optional.of(delivery));

            Delivery result = deliveryService.findById(1L, null);

            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Lança exceção se não encontra")
        void naoEncontra() {
            when(deliveryRepository.findByIdWithJoins(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deliveryService.findById(999L, null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("não encontrada");
        }
    }

    // ================================================================
    // DELETE
    // ================================================================

    @Nested
    @DisplayName("delete() — Exclusão de entregas")
    class DeleteTests {

        @Test
        @DisplayName("ADMIN pode deletar qualquer delivery")
        void adminDeleta() {
            Delivery delivery = makeDelivery(1L, makeUser(clientId, "X", User.Role.CLIENT));
            when(deliveryRepository.findByIdWithJoins(1L)).thenReturn(Optional.of(delivery));
            when(paymentRepository.existsByDeliveryIdLong(1L)).thenReturn(false);

            deliveryService.delete(1L, adminId, "ADMIN");

            verify(deliveryRepository).delete(delivery);
        }

        @Test
        @DisplayName("Não pode deletar delivery com pagamento associado")
        void naoDeleteComPagamento() {
            Delivery delivery = makeDelivery(1L, makeUser(clientId, "X", User.Role.CLIENT));
            when(deliveryRepository.findByIdWithJoins(1L)).thenReturn(Optional.of(delivery));
            when(paymentRepository.existsByDeliveryIdLong(1L)).thenReturn(true);

            assertThatThrownBy(() -> deliveryService.delete(1L, adminId, "ADMIN"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("associada a um pagamento");
        }

        @Test
        @DisplayName("COURIER não pode deletar delivery")
        void courierNaoDeleta() {
            Delivery delivery = makeDelivery(1L, makeUser(clientId, "X", User.Role.CLIENT));
            when(deliveryRepository.findByIdWithJoins(1L)).thenReturn(Optional.of(delivery));
            when(paymentRepository.existsByDeliveryIdLong(1L)).thenReturn(false);

            assertThatThrownBy(() -> deliveryService.delete(1L, courierId, "COURIER"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("não tem permissão");
        }
    }
}
