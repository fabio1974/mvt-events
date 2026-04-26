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

    // ================================================================
    // ASSIGN TO COURIER
    // ================================================================

    @Nested
    @DisplayName("assignToCourier() — Aceitação de entregas pelo courier")
    class AssignToCourierTests {

        private Organization makeOrganization(Long id, String name, User owner) {
            Organization org = new Organization();
            org.setId(id);
            org.setName(name);
            org.setOwner(owner);
            return org;
        }

        private EmploymentContract makeEmploymentContract(User courier, Organization org) {
            EmploymentContract ec = new EmploymentContract();
            ec.setCourier(courier);
            ec.setOrganization(org);
            return ec;
        }

        private ClientContract makeClientContract(User client, Organization org, boolean primary) {
            ClientContract cc = new ClientContract();
            cc.setClient(client);
            cc.setOrganization(org);
            cc.setPrimary(primary);
            return cc;
        }

        @Test
        @DisplayName("Courier aceita delivery PENDING de CLIENT (fluxo com organização)")
        void courierAceitaDeliveryClientComOrganizacao() {
            User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            User courier = makeUser(courierId, "Pedro Moto", User.Role.COURIER);
            User organizer = makeUser(organizerId, "Ana Gerente", User.Role.ORGANIZER);
            Organization org = makeOrganization(1L, "Org ABC", organizer);

            Delivery delivery = makeDelivery(1L, client);
            delivery.setStatus(Delivery.DeliveryStatus.PENDING);

            when(deliveryRepository.findByIdWithJoins(1L)).thenReturn(Optional.of(delivery));
            when(userRepository.findById(courierId)).thenReturn(Optional.of(courier));
            when(vehicleRepository.findActiveVehicleByOwnerId(courierId)).thenReturn(Optional.empty());

            // findCommonOrganization mocks
            when(employmentContractRepository.findActiveByCourierId(courierId))
                    .thenReturn(List.of(makeEmploymentContract(courier, org)));
            when(clientContractRepository.findActiveByClientId(clientId))
                    .thenReturn(List.of(makeClientContract(client, org, true)));

            when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));
            when(deliveryRepository.findByIdWithJoins(1L)).thenReturn(Optional.of(delivery));

            // CLIENT + PIX → sem cobrança automática (skip createAutomaticCreditCardPayment)
            CustomerPaymentPreference pixPref = new CustomerPaymentPreference();
            pixPref.setPreferredPaymentType(PreferredPaymentType.PIX);
            when(preferenceService.getPreference(clientId)).thenReturn(pixPref);

            Delivery result = deliveryService.assignToCourier(1L, courierId, null);

            assertThat(result.getCourier()).isEqualTo(courier);
            assertThat(result.getStatus()).isEqualTo(Delivery.DeliveryStatus.ACCEPTED);
            assertThat(result.getOrganizer()).isEqualTo(organizer);
            assertThat(result.getAcceptedAt()).isNotNull();
            assertThat(courier.getCurrentDeliveryId()).isEqualTo(1L);
            verify(userRepository).save(courier);
        }

        @Test
        @DisplayName("Courier aceita delivery PENDING de CUSTOMER (fluxo sem organização)")
        void courierAceitaDeliveryCustomerSemOrganizacao() {
            User customer = makeUser(customerId, "Joao Cliente", User.Role.CUSTOMER);
            User courier = makeUser(courierId, "Pedro Moto", User.Role.COURIER);

            Delivery delivery = makeDelivery(1L, customer);
            delivery.setStatus(Delivery.DeliveryStatus.PENDING);
            delivery.setDeliveryType(Delivery.DeliveryType.DELIVERY);

            when(deliveryRepository.findByIdWithJoins(1L)).thenReturn(Optional.of(delivery));
            when(userRepository.findById(courierId)).thenReturn(Optional.of(courier));
            when(vehicleRepository.findActiveVehicleByOwnerId(courierId)).thenReturn(Optional.empty());
            when(preferenceService.getPreference(customerId)).thenReturn(null);
            when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));
            when(deliveryRepository.findByIdWithJoins(1L)).thenReturn(Optional.of(delivery));

            Delivery result = deliveryService.assignToCourier(1L, courierId, null);

            assertThat(result.getCourier()).isEqualTo(courier);
            assertThat(result.getStatus()).isEqualTo(Delivery.DeliveryStatus.ACCEPTED);
            assertThat(result.getOrganizer()).isNull();
            assertThat(courier.getCurrentDeliveryId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Delivery já aceita por outro courier lança exceção")
        void deliveryJaAceitaLancaExcecao() {
            User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            Delivery delivery = makeDelivery(1L, client);
            delivery.setStatus(Delivery.DeliveryStatus.ACCEPTED);

            when(deliveryRepository.findByIdWithJoins(1L)).thenReturn(Optional.of(delivery));

            assertThatThrownBy(() -> deliveryService.assignToCourier(1L, courierId, null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("já foi aceita por outro motoboy");
        }

        @Test
        @DisplayName("Usuário não-COURIER não pode aceitar delivery")
        void naoCourierNaoPodeAceitar() {
            User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            Delivery delivery = makeDelivery(1L, client);
            delivery.setStatus(Delivery.DeliveryStatus.PENDING);

            User userClient = makeUser(clientId, "Restaurante X", User.Role.CLIENT);

            when(deliveryRepository.findByIdWithJoins(1L)).thenReturn(Optional.of(delivery));
            when(userRepository.findById(clientId)).thenReturn(Optional.of(userClient));

            assertThatThrownBy(() -> deliveryService.assignToCourier(1L, clientId, null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("não é um courier");
        }

        @Test
        @DisplayName("CUSTOMER + PIX → status vira WAITING_PAYMENT")
        void customerPixStatusWaitingPayment() {
            User customer = makeUser(customerId, "Joao Cliente", User.Role.CUSTOMER);
            User courier = makeUser(courierId, "Pedro Moto", User.Role.COURIER);

            Delivery delivery = makeDelivery(1L, customer);
            delivery.setStatus(Delivery.DeliveryStatus.PENDING);
            delivery.setDeliveryType(Delivery.DeliveryType.DELIVERY);

            CustomerPaymentPreference pixPref = new CustomerPaymentPreference();
            pixPref.setPreferredPaymentType(PreferredPaymentType.PIX);

            when(deliveryRepository.findByIdWithJoins(1L)).thenReturn(Optional.of(delivery));
            when(userRepository.findById(courierId)).thenReturn(Optional.of(courier));
            when(vehicleRepository.findActiveVehicleByOwnerId(courierId)).thenReturn(Optional.empty());
            when(preferenceService.getPreference(customerId)).thenReturn(pixPref);
            when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));

            // Evitar que createPixPaymentForCustomer tente criar pagamento real
            when(paymentRepository.existsPendingOrPaidPaymentForDelivery(1L)).thenReturn(true);

            Delivery result = deliveryService.assignToCourier(1L, courierId, null);

            assertThat(result.getStatus()).isEqualTo(Delivery.DeliveryStatus.WAITING_PAYMENT);
        }

        @Test
        @DisplayName("Veículo ativo do courier é setado na delivery")
        void veiculoAtivoSetadoNaDelivery() {
            User customer = makeUser(customerId, "Joao Cliente", User.Role.CUSTOMER);
            User courier = makeUser(courierId, "Pedro Moto", User.Role.COURIER);

            Delivery delivery = makeDelivery(1L, customer);
            delivery.setStatus(Delivery.DeliveryStatus.PENDING);
            delivery.setDeliveryType(Delivery.DeliveryType.DELIVERY);

            Vehicle vehicle = new Vehicle();
            vehicle.setId(10L);

            when(deliveryRepository.findByIdWithJoins(1L)).thenReturn(Optional.of(delivery));
            when(userRepository.findById(courierId)).thenReturn(Optional.of(courier));
            when(vehicleRepository.findActiveVehicleByOwnerId(courierId)).thenReturn(Optional.of(vehicle));
            when(preferenceService.getPreference(customerId)).thenReturn(null);
            when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));
            when(deliveryRepository.findByIdWithJoins(1L)).thenReturn(Optional.of(delivery));

            Delivery result = deliveryService.assignToCourier(1L, courierId, null);

            assertThat(result.getVehicle()).isEqualTo(vehicle);
        }

        @Test
        @DisplayName("Level 3: CLIENT sem organização comum com courier — aceita com organizer=null")
        void clientSemOrganizacaoComumAceitaComOrganizerNull() {
            User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            User courier = makeUser(courierId, "Pedro Moto", User.Role.COURIER);

            Delivery delivery = makeDelivery(1L, client);
            delivery.setStatus(Delivery.DeliveryStatus.PENDING);

            when(deliveryRepository.findByIdWithJoins(1L)).thenReturn(Optional.of(delivery));
            when(userRepository.findById(courierId)).thenReturn(Optional.of(courier));
            when(vehicleRepository.findActiveVehicleByOwnerId(courierId)).thenReturn(Optional.empty());

            // Sem contratos em comum (Level 3 — courier nearby sem ligação contratual)
            when(employmentContractRepository.findActiveByCourierId(courierId))
                    .thenReturn(List.of());
            when(clientContractRepository.findActiveByClientId(clientId))
                    .thenReturn(List.of());

            when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));

            // PIX preference → skip createAutomaticCreditCardPayment
            CustomerPaymentPreference pixPref = new CustomerPaymentPreference();
            pixPref.setPreferredPaymentType(PreferredPaymentType.PIX);
            when(preferenceService.getPreference(clientId)).thenReturn(pixPref);

            Delivery result = deliveryService.assignToCourier(1L, courierId, null);

            assertThat(result.getCourier()).isEqualTo(courier);
            assertThat(result.getStatus()).isEqualTo(Delivery.DeliveryStatus.ACCEPTED);
            // Level 3: sem organização comum → organizer=null, plataforma absorve os 5%
            assertThat(result.getOrganizer()).isNull();
            assertThat(result.getAcceptedAt()).isNotNull();
        }
    }

    // ================================================================
    // START TRANSIT (deprecated, delegates to confirmPickup)
    // ================================================================

    @Nested
    @DisplayName("startTransit() — Iniciar transporte (ACCEPTED → IN_TRANSIT)")
    class StartTransitTests {

        @Test
        @DisplayName("Sucesso: ACCEPTED → IN_TRANSIT")
        void sucessoAceitaParaEmTransito() {
            User courier = makeUser(courierId, "Pedro", User.Role.COURIER);
            User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            Delivery delivery = makeDelivery(1L, client);
            delivery.setStatus(Delivery.DeliveryStatus.ACCEPTED);
            delivery.setCourier(courier);

            when(deliveryRepository.findByIdWithJoins(1L)).thenReturn(Optional.of(delivery));
            when(deliveryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(deliveryRepository.findByIdWithJoins(1L)).thenReturn(Optional.of(delivery));

            Delivery result = deliveryService.startTransit(1L, courierId);

            assertThat(result.getStatus()).isEqualTo(Delivery.DeliveryStatus.IN_TRANSIT);
            assertThat(result.getPickedUpAt()).isNotNull();
            assertThat(result.getInTransitAt()).isNotNull();
        }

        @Test
        @DisplayName("Courier errado não pode iniciar transporte")
        void courierErrado() {
            UUID outroCourierId = UUID.randomUUID();
            User courier = makeUser(courierId, "Pedro", User.Role.COURIER);
            Delivery delivery = makeDelivery(1L, makeUser(clientId, "X", User.Role.CLIENT));
            delivery.setStatus(Delivery.DeliveryStatus.ACCEPTED);
            delivery.setCourier(courier);

            when(deliveryRepository.findByIdWithJoins(1L)).thenReturn(Optional.of(delivery));

            assertThatThrownBy(() -> deliveryService.startTransit(1L, outroCourierId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Delivery não pertence a este courier");
        }

        @Test
        @DisplayName("Status inválido (PENDING) não pode iniciar transporte")
        void statusInvalido() {
            User courier = makeUser(courierId, "Pedro", User.Role.COURIER);
            Delivery delivery = makeDelivery(1L, makeUser(clientId, "X", User.Role.CLIENT));
            delivery.setStatus(Delivery.DeliveryStatus.PENDING);
            delivery.setCourier(courier);

            when(deliveryRepository.findByIdWithJoins(1L)).thenReturn(Optional.of(delivery));

            assertThatThrownBy(() -> deliveryService.startTransit(1L, courierId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Status inválido");
        }
    }

    // ================================================================
    // UPDATE STATUS
    // ================================================================

    @Nested
    @DisplayName("updateStatus() — Transições genéricas de status")
    class UpdateStatusTests {

        @Test
        @DisplayName("PENDING → CANCELLED é transição válida (status volta para PENDING)")
        void pendingParaCancelled() {
            Delivery delivery = makeDelivery(1L, makeUser(clientId, "X", User.Role.CLIENT));
            delivery.setStatus(Delivery.DeliveryStatus.PENDING);

            when(deliveryRepository.findByIdWithJoins(1L)).thenReturn(Optional.of(delivery));
            when(deliveryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Delivery result = deliveryService.updateStatus(1L, Delivery.DeliveryStatus.CANCELLED, "Motivo", null);

            // updateStatus CANCELLED sets cancelledAt but then resets status back to PENDING
            assertThat(result.getStatus()).isEqualTo(Delivery.DeliveryStatus.PENDING);
            assertThat(result.getCancelledAt()).isNotNull();
            assertThat(result.getCancellationReason()).isEqualTo("Motivo");
        }

        @Test
        @DisplayName("PENDING → ACCEPTED é transição válida")
        void pendingParaAccepted() {
            Delivery delivery = makeDelivery(1L, makeUser(clientId, "X", User.Role.CLIENT));
            delivery.setStatus(Delivery.DeliveryStatus.PENDING);

            when(deliveryRepository.findByIdWithJoins(1L)).thenReturn(Optional.of(delivery));
            when(deliveryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Delivery result = deliveryService.updateStatus(1L, Delivery.DeliveryStatus.ACCEPTED, null, null);

            assertThat(result.getStatus()).isEqualTo(Delivery.DeliveryStatus.ACCEPTED);
            assertThat(result.getAcceptedAt()).isNotNull();
        }

        @Test
        @DisplayName("ACCEPTED → IN_TRANSIT é transição válida")
        void acceptedParaInTransit() {
            User courier = makeUser(courierId, "Pedro", User.Role.COURIER);
            Delivery delivery = makeDelivery(1L, makeUser(clientId, "X", User.Role.CLIENT));
            delivery.setStatus(Delivery.DeliveryStatus.ACCEPTED);
            delivery.setCourier(courier);
            delivery.setAcceptedAt(OffsetDateTime.now());

            when(deliveryRepository.findByIdWithJoins(1L)).thenReturn(Optional.of(delivery));
            when(deliveryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Delivery result = deliveryService.updateStatus(1L, Delivery.DeliveryStatus.IN_TRANSIT, null, null);

            assertThat(result.getStatus()).isEqualTo(Delivery.DeliveryStatus.IN_TRANSIT);
            assertThat(result.getInTransitAt()).isNotNull();
        }

        @Test
        @DisplayName("IN_TRANSIT → COMPLETED é transição válida")
        void inTransitParaCompleted() {
            User courier = makeUser(courierId, "Pedro", User.Role.COURIER);
            Delivery delivery = makeDelivery(1L, makeUser(clientId, "X", User.Role.CLIENT));
            delivery.setStatus(Delivery.DeliveryStatus.IN_TRANSIT);
            delivery.setCourier(courier);
            delivery.setAcceptedAt(OffsetDateTime.now());
            delivery.setInTransitAt(OffsetDateTime.now());

            when(deliveryRepository.findByIdWithJoins(1L)).thenReturn(Optional.of(delivery));
            when(deliveryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Delivery result = deliveryService.updateStatus(1L, Delivery.DeliveryStatus.COMPLETED, null, null);

            assertThat(result.getStatus()).isEqualTo(Delivery.DeliveryStatus.COMPLETED);
            assertThat(result.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("WAITING_PAYMENT → ACCEPTED é transição válida")
        void waitingPaymentParaAccepted() {
            Delivery delivery = makeDelivery(1L, makeUser(clientId, "X", User.Role.CLIENT));
            delivery.setStatus(Delivery.DeliveryStatus.WAITING_PAYMENT);

            when(deliveryRepository.findByIdWithJoins(1L)).thenReturn(Optional.of(delivery));
            when(deliveryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Delivery result = deliveryService.updateStatus(1L, Delivery.DeliveryStatus.ACCEPTED, null, null);

            assertThat(result.getStatus()).isEqualTo(Delivery.DeliveryStatus.ACCEPTED);
        }

        @Test
        @DisplayName("COMPLETED → PENDING é transição inválida")
        void completedParaPendingInvalido() {
            Delivery delivery = makeDelivery(1L, makeUser(clientId, "X", User.Role.CLIENT));
            delivery.setStatus(Delivery.DeliveryStatus.COMPLETED);

            when(deliveryRepository.findByIdWithJoins(1L)).thenReturn(Optional.of(delivery));

            assertThatThrownBy(() -> deliveryService.updateStatus(1L, Delivery.DeliveryStatus.PENDING, null, null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Não é possível voltar para PENDING");
        }

        @Test
        @DisplayName("COMPLETED → CANCELLED é transição inválida")
        void completedParaCancelledInvalido() {
            Delivery delivery = makeDelivery(1L, makeUser(clientId, "X", User.Role.CLIENT));
            delivery.setStatus(Delivery.DeliveryStatus.COMPLETED);

            when(deliveryRepository.findByIdWithJoins(1L)).thenReturn(Optional.of(delivery));

            assertThatThrownBy(() -> deliveryService.updateStatus(1L, Delivery.DeliveryStatus.CANCELLED, null, null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Não é possível cancelar delivery completada");
        }

        @Test
        @DisplayName("ACCEPTED → COMPLETED é transição inválida (pula IN_TRANSIT)")
        void acceptedParaCompletedInvalido() {
            Delivery delivery = makeDelivery(1L, makeUser(clientId, "X", User.Role.CLIENT));
            delivery.setStatus(Delivery.DeliveryStatus.ACCEPTED);

            when(deliveryRepository.findByIdWithJoins(1L)).thenReturn(Optional.of(delivery));

            assertThatThrownBy(() -> deliveryService.updateStatus(1L, Delivery.DeliveryStatus.COMPLETED, null, null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("De ACCEPTED só pode ir para IN_TRANSIT");
        }

        @Test
        @DisplayName("CANCELLED → qualquer (exceto PENDING) é transição inválida")
        void cancelledParaAcceptedInvalido() {
            Delivery delivery = makeDelivery(1L, makeUser(clientId, "X", User.Role.CLIENT));
            delivery.setStatus(Delivery.DeliveryStatus.CANCELLED);

            when(deliveryRepository.findByIdWithJoins(1L)).thenReturn(Optional.of(delivery));

            assertThatThrownBy(() -> deliveryService.updateStatus(1L, Delivery.DeliveryStatus.ACCEPTED, null, null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("cancelada não pode mudar de status");
        }
    }

    // ================================================================
    // UPDATE
    // ================================================================

    @Nested
    @DisplayName("update() — Atualizar campos da delivery")
    class UpdateTests {

        @Test
        @DisplayName("CLIENT atualiza sua delivery PENDING com sucesso")
        void clientAtualizaPendingSucesso() {
            User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            Delivery existing = makeDelivery(1L, client);
            existing.setStatus(Delivery.DeliveryStatus.PENDING);

            Delivery updated = new Delivery();
            updated.setFromAddress("Rua Nova, 10");
            updated.setToAddress("Rua Destino, 20");
            updated.setRecipientName("Maria");
            updated.setRecipientPhone("85999998888");
            updated.setItemDescription("Pizza");

            when(deliveryRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(userRepository.findById(clientId)).thenReturn(Optional.of(client));
            when(deliveryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Delivery result = deliveryService.update(1L, updated, clientId);

            assertThat(result.getFromAddress()).isEqualTo("Rua Nova, 10");
            assertThat(result.getToAddress()).isEqualTo("Rua Destino, 20");
            assertThat(result.getRecipientName()).isEqualTo("Maria");
        }

        @Test
        @DisplayName("CLIENT não pode atualizar delivery COMPLETED")
        void clientNaoAtualizaCompleted() {
            User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            Delivery existing = makeDelivery(1L, client);
            existing.setStatus(Delivery.DeliveryStatus.COMPLETED);

            when(deliveryRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(userRepository.findById(clientId)).thenReturn(Optional.of(client));

            assertThatThrownBy(() -> deliveryService.update(1L, new Delivery(), clientId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Apenas entregas PENDING podem ser editadas");
        }

        @Test
        @DisplayName("COURIER não pode atualizar delivery")
        void courierNaoAtualiza() {
            User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            User courier = makeUser(courierId, "Pedro Moto", User.Role.COURIER);
            Delivery existing = makeDelivery(1L, client);
            existing.setStatus(Delivery.DeliveryStatus.PENDING);

            when(deliveryRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(userRepository.findById(courierId)).thenReturn(Optional.of(courier));

            assertThatThrownBy(() -> deliveryService.update(1L, new Delivery(), courierId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("não pode editar entregas");
        }

        @Test
        @DisplayName("CLIENT não pode atualizar delivery de outro CLIENT")
        void clientNaoAtualizaDeOutro() {
            UUID outroClientId = UUID.randomUUID();
            User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            User outroClient = makeUser(outroClientId, "Restaurante Y", User.Role.CLIENT);
            Delivery existing = makeDelivery(1L, client);
            existing.setStatus(Delivery.DeliveryStatus.PENDING);

            when(deliveryRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(userRepository.findById(outroClientId)).thenReturn(Optional.of(outroClient));

            assertThatThrownBy(() -> deliveryService.update(1L, new Delivery(), outroClientId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("só pode editar suas próprias entregas");
        }
    }

    // ================================================================
    // FREIGHT CALCULATION EDGE CASES
    // ================================================================

    @Nested
    @DisplayName("create() — Cálculo de frete: edge cases")
    class FreightEdgeCaseTests {

        @Test
        @DisplayName("Multi-stop adiciona additionalStopFee por parada extra")
        void multiStopAdicionaTaxaPorParadaExtra() {
            User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            Delivery delivery = makeDelivery(null, null);
            delivery.setDistanceKm(BigDecimal.valueOf(10.0));

            // 3 paradas = 2 extras
            DeliveryStop stop1 = new DeliveryStop();
            stop1.setLatitude(-3.70);
            stop1.setLongitude(-40.36);
            DeliveryStop stop2 = new DeliveryStop();
            stop2.setLatitude(-3.71);
            stop2.setLongitude(-40.37);
            DeliveryStop stop3 = new DeliveryStop();
            stop3.setLatitude(-3.72);
            stop3.setLongitude(-40.38);
            List<DeliveryStop> stops = new ArrayList<>();
            stops.add(stop1);
            stops.add(stop2);
            stops.add(stop3);
            delivery.setStops(stops);

            SiteConfiguration config = defaultConfig();

            when(userRepository.findById(clientId)).thenReturn(Optional.of(client));
            when(siteConfigurationService.getActiveConfiguration()).thenReturn(config);
            when(specialZoneService.findWorstZoneAcrossStops(anyList(), any())).thenReturn(Optional.empty());
            when(deliveryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Delivery result = deliveryService.create(delivery, clientId, clientId);

            // 10km x R$2.50 = R$25.00 base + 2 extras x R$2.00 = R$4.00 → R$29.00
            assertThat(result.getShippingFee()).isEqualByComparingTo("29.00");
        }

        @Test
        @DisplayName("Zona de perigo adiciona surcharge percentual ao frete")
        void zonaDePerigaAdicionaSurcharge() {
            User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            Delivery delivery = makeDelivery(null, null);
            delivery.setDistanceKm(BigDecimal.valueOf(10.0));

            SpecialZone dangerZone = new SpecialZone();
            dangerZone.setZoneType(SpecialZone.ZoneType.DANGER);
            dangerZone.setAddress("Zona Perigosa");

            SiteConfiguration config = defaultConfig();

            when(userRepository.findById(clientId)).thenReturn(Optional.of(client));
            when(siteConfigurationService.getActiveConfiguration()).thenReturn(config);
            when(specialZoneService.findNearestZone(anyDouble(), anyDouble()))
                    .thenReturn(Optional.of(dangerZone));
            when(deliveryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Delivery result = deliveryService.create(delivery, clientId, clientId);

            // 10km x R$2.50 = R$25.00 base + 20% surcharge = R$5.00 → R$30.00
            assertThat(result.getShippingFee()).isEqualByComparingTo("30.00");
        }

        @Test
        @DisplayName("Multi-stop em zona de perigo: surcharge + taxa por parada extra")
        void multiStopComZonaDePerigo() {
            User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            Delivery delivery = makeDelivery(null, null);
            delivery.setDistanceKm(BigDecimal.valueOf(10.0));

            // 2 paradas = 1 extra
            DeliveryStop stop1 = new DeliveryStop();
            stop1.setLatitude(-3.70);
            stop1.setLongitude(-40.36);
            DeliveryStop stop2 = new DeliveryStop();
            stop2.setLatitude(-3.71);
            stop2.setLongitude(-40.37);
            List<DeliveryStop> stops = new ArrayList<>();
            stops.add(stop1);
            stops.add(stop2);
            delivery.setStops(stops);

            SpecialZone dangerZone = new SpecialZone();
            dangerZone.setZoneType(SpecialZone.ZoneType.DANGER);
            dangerZone.setAddress("Zona Perigosa");

            SiteConfiguration config = defaultConfig();

            when(userRepository.findById(clientId)).thenReturn(Optional.of(client));
            when(siteConfigurationService.getActiveConfiguration()).thenReturn(config);
            when(specialZoneService.findWorstZoneAcrossStops(anyList(), any()))
                    .thenReturn(Optional.of(dangerZone));
            when(deliveryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Delivery result = deliveryService.create(delivery, clientId, clientId);

            // 10km x R$2.50 = R$25.00 base + 20% danger = R$5.00 → R$30.00 + 1 extra stop x R$2.00 = R$32.00
            assertThat(result.getShippingFee()).isEqualByComparingTo("32.00");
        }
    }

    // ================================================================
    // FIND ACTIVE / COMPLETED BY COURIER
    // ================================================================

    @Nested
    @DisplayName("findActiveByCourier() / findCompletedByCourier() — Delegação ao repositório")
    class FindByCourierTests {

        @Test
        @DisplayName("findActiveByCourier delega para o repositório")
        void findActiveByCourierDelega() {
            Delivery d1 = makeDelivery(1L, makeUser(clientId, "X", User.Role.CLIENT));
            d1.setStatus(Delivery.DeliveryStatus.ACCEPTED);
            d1.setCourier(makeUser(courierId, "Pedro", User.Role.COURIER));

            when(deliveryRepository.findActiveByCourierId(courierId)).thenReturn(List.of(d1));

            List<Delivery> result = deliveryService.findActiveByCourier(courierId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(1L);
            verify(deliveryRepository).findActiveByCourierId(courierId);
        }

        @Test
        @DisplayName("findCompletedByCourier sem filtro delega para repositório")
        void findCompletedByCourierDelega() {
            Delivery d1 = makeDelivery(1L, makeUser(clientId, "X", User.Role.CLIENT));
            d1.setStatus(Delivery.DeliveryStatus.COMPLETED);
            d1.setCourier(makeUser(courierId, "Pedro", User.Role.COURIER));

            when(deliveryRepository.findCompletedByCourierId(courierId)).thenReturn(List.of(d1));

            List<Delivery> result = deliveryService.findCompletedByCourier(courierId);

            assertThat(result).hasSize(1);
            verify(deliveryRepository).findCompletedByCourierId(courierId);
        }

        @Test
        @DisplayName("findCompletedByCourier com unpaidOnly=true filtra não pagos")
        void findCompletedByCourierUnpaidOnly() {
            when(deliveryRepository.findCompletedUnpaidByCourierId(courierId)).thenReturn(List.of());

            List<Delivery> result = deliveryService.findCompletedByCourier(courierId, true);

            assertThat(result).isEmpty();
            verify(deliveryRepository).findCompletedUnpaidByCourierId(courierId);
            verify(deliveryRepository, never()).findCompletedByCourierId(any());
        }
    }
}
