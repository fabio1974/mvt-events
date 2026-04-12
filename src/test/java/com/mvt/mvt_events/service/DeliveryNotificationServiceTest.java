package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.*;
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
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitarios do DeliveryNotificationService - algoritmo de notificacao
 * em 3 niveis para motoboys: organizacao titular, secundarias, broadcast geografico.
 */
@ExtendWith(MockitoExtension.class)
class DeliveryNotificationServiceTest {

    @Mock private PushNotificationService pushNotificationService;
    @Mock private ClientContractRepository clientContractRepository;
    @Mock private EmploymentContractRepository employmentContractRepository;
    @Mock private UserPushTokenRepository userPushTokenRepository;
    @Mock private UserRepository userRepository;
    @Mock private DeliveryRepository deliveryRepository;
    @Mock private VehicleRepository vehicleRepository;

    @InjectMocks
    private DeliveryNotificationService deliveryNotificationService;

    // ========== Helpers ==========

    private final UUID clientId = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private final UUID courier1Id = UUID.fromString("00000000-0000-0000-0000-000000000021");
    private final UUID courier2Id = UUID.fromString("00000000-0000-0000-0000-000000000022");
    private final UUID courier3Id = UUID.fromString("00000000-0000-0000-0000-000000000023");
    private final UUID courier4Id = UUID.fromString("00000000-0000-0000-0000-000000000024");

    private User makeUser(UUID id, String name, User.Role role) {
        User u = new User();
        u.setId(id);
        u.setName(name);
        u.setRole(role);
        u.setUsername(name.toLowerCase().replace(" ", ".") + "@zapi10.com");
        u.setEnabled(true);
        return u;
    }

    private Delivery makeDelivery(Long id) {
        User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
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
        d.setTotalAmount(BigDecimal.valueOf(15.00));
        d.setDistanceKm(BigDecimal.valueOf(5.0));
        d.setDeliveryType(Delivery.DeliveryType.DELIVERY);
        d.setPreferredVehicleType(Delivery.PreferredVehicleType.MOTORCYCLE);
        return d;
    }

    private Organization makeOrganization(Long id, String name) {
        Organization org = new Organization();
        org.setId(id);
        org.setName(name);
        return org;
    }

    private ClientContract makePrimaryContract(User client, Organization org) {
        ClientContract cc = new ClientContract();
        cc.setClient(client);
        cc.setOrganization(org);
        cc.setPrimary(true);
        cc.setStatus(ClientContract.ContractStatus.ACTIVE);
        return cc;
    }

    private ClientContract makeSecondaryContract(User client, Organization org) {
        ClientContract cc = new ClientContract();
        cc.setClient(client);
        cc.setOrganization(org);
        cc.setPrimary(false);
        cc.setStatus(ClientContract.ContractStatus.ACTIVE);
        return cc;
    }

    private EmploymentContract makeEmploymentContract(User courier, Organization org) {
        EmploymentContract ec = new EmploymentContract();
        ec.setCourier(courier);
        ec.setOrganization(org);
        ec.setActive(true);
        return ec;
    }

    private UserPushToken makeToken(User user) {
        UserPushToken token = new UserPushToken();
        token.setUserId(user.getId());
        token.setIsActive(true);
        return token;
    }

    /**
     * Stub base: delivery stays PENDING so notification process continues.
     * Also stubs push token for the given couriers.
     */
    private void stubDeliveryPending(Delivery delivery) {
        when(deliveryRepository.findById(delivery.getId())).thenReturn(Optional.of(delivery));
    }

    private void stubPushTokensFor(User... couriers) {
        for (User c : couriers) {
            lenient().when(userPushTokenRepository.findByUserIdAndIsActiveTrue(c.getId()))
                    .thenReturn(List.of(makeToken(c)));
        }
    }

    // ================================================================
    // LEVEL 1 - Primary Organization
    // ================================================================

    @Nested
    @DisplayName("Nivel 1 - Organizacao titular")
    class Level1Tests {

        @Test
        @DisplayName("Notifica apenas couriers da organizacao titular (isPrimary=true)")
        void notificaCouriersDaOrgTitular() throws Exception {
            Delivery delivery = makeDelivery(1L);
            Organization primaryOrg = makeOrganization(1L, "Org Principal");
            User courier1 = makeUser(courier1Id, "Motoboy A", User.Role.COURIER);

            ClientContract primaryContract = makePrimaryContract(delivery.getClient(), primaryOrg);
            EmploymentContract empContract = makeEmploymentContract(courier1, primaryOrg);

            when(clientContractRepository.findPrimaryByClient(delivery.getClient()))
                    .thenReturn(Optional.of(primaryContract));
            when(employmentContractRepository.findActiveByOrganization(primaryOrg))
                    .thenReturn(List.of(empContract));
            when(userRepository.findAvailableCouriersNearbyWithVehicleType(
                    eq(delivery.getFromLatitude()), eq(delivery.getFromLongitude()),
                    eq(5.0), eq(VehicleType.MOTORCYCLE), anyList()))
                    .thenReturn(List.of(courier1));

            stubDeliveryPending(delivery);
            stubPushTokensFor(courier1);

            // Stub: delivery gets accepted after level 1 -> stop at level 1
            // Use a counter so first call returns PENDING, second returns ACCEPTED
            Delivery acceptedDelivery = makeDelivery(1L);
            acceptedDelivery.setStatus(Delivery.DeliveryStatus.ACCEPTED);
            when(deliveryRepository.findById(1L))
                    .thenReturn(Optional.of(delivery))   // check before sending
                    .thenReturn(Optional.of(acceptedDelivery)); // check after timeout

            CompletableFuture<Void> future = deliveryNotificationService.notifyAvailableDrivers(delivery);
            future.get();

            verify(pushNotificationService).sendHybridNotificationToUser(
                    eq(courier1Id), anyString(), anyString(), anyMap());
        }

        @Test
        @DisplayName("Sem contrato titular retorna false e pula para nivel 2")
        void semContratoTitularPulaParaNivel2() throws Exception {
            Delivery delivery = makeDelivery(1L);

            when(clientContractRepository.findPrimaryByClient(delivery.getClient()))
                    .thenReturn(Optional.empty());
            // Level 2 - no secondary contracts either
            when(clientContractRepository.findActiveByClient(delivery.getClient()))
                    .thenReturn(List.of());
            // Level 3 - no couriers nearby
            when(userRepository.findAvailableCouriersNearbyWithVehicleType(
                    anyDouble(), anyDouble(), anyDouble(), any(VehicleType.class), anyList()))
                    .thenReturn(List.of());

            CompletableFuture<Void> future = deliveryNotificationService.notifyAvailableDrivers(delivery);
            future.get();

            // No push notifications sent because no couriers found anywhere
            verify(pushNotificationService, never()).sendHybridNotificationToUser(
                    any(), anyString(), anyString(), anyMap());
        }

        @Test
        @DisplayName("Org titular sem couriers ativos retorna false")
        void orgTitularSemCouriers() throws Exception {
            Delivery delivery = makeDelivery(1L);
            Organization primaryOrg = makeOrganization(1L, "Org Principal");
            ClientContract primaryContract = makePrimaryContract(delivery.getClient(), primaryOrg);

            when(clientContractRepository.findPrimaryByClient(delivery.getClient()))
                    .thenReturn(Optional.of(primaryContract));
            when(employmentContractRepository.findActiveByOrganization(primaryOrg))
                    .thenReturn(List.of()); // nenhum courier ativo

            // Level 2 and 3 stubs - empty
            when(clientContractRepository.findActiveByClient(delivery.getClient()))
                    .thenReturn(List.of(primaryContract));
            when(userRepository.findAvailableCouriersNearbyWithVehicleType(
                    anyDouble(), anyDouble(), anyDouble(), any(VehicleType.class), anyList()))
                    .thenReturn(List.of());

            CompletableFuture<Void> future = deliveryNotificationService.notifyAvailableDrivers(delivery);
            future.get();

            verify(pushNotificationService, never()).sendHybridNotificationToUser(
                    any(), anyString(), anyString(), anyMap());
        }

        @Test
        @DisplayName("Tenta raio estendido (30km) quando nenhum courier no raio inicial (5km)")
        void tentaRaioEstendido() throws Exception {
            Delivery delivery = makeDelivery(1L);
            Organization primaryOrg = makeOrganization(1L, "Org Principal");
            User courier1 = makeUser(courier1Id, "Motoboy A", User.Role.COURIER);

            ClientContract primaryContract = makePrimaryContract(delivery.getClient(), primaryOrg);
            EmploymentContract empContract = makeEmploymentContract(courier1, primaryOrg);

            when(clientContractRepository.findPrimaryByClient(delivery.getClient()))
                    .thenReturn(Optional.of(primaryContract));
            when(employmentContractRepository.findActiveByOrganization(primaryOrg))
                    .thenReturn(List.of(empContract));

            // 5km -> empty, 30km -> finds courier
            when(userRepository.findAvailableCouriersNearbyWithVehicleType(
                    eq(delivery.getFromLatitude()), eq(delivery.getFromLongitude()),
                    eq(5.0), eq(VehicleType.MOTORCYCLE), anyList()))
                    .thenReturn(List.of());
            when(userRepository.findAvailableCouriersNearbyWithVehicleType(
                    eq(delivery.getFromLatitude()), eq(delivery.getFromLongitude()),
                    eq(30.0), eq(VehicleType.MOTORCYCLE), anyList()))
                    .thenReturn(List.of(courier1));

            // Delivery accepted after level 1
            Delivery acceptedDelivery = makeDelivery(1L);
            acceptedDelivery.setStatus(Delivery.DeliveryStatus.ACCEPTED);
            when(deliveryRepository.findById(1L))
                    .thenReturn(Optional.of(delivery))
                    .thenReturn(Optional.of(acceptedDelivery));

            stubPushTokensFor(courier1);

            CompletableFuture<Void> future = deliveryNotificationService.notifyAvailableDrivers(delivery);
            future.get();

            verify(pushNotificationService).sendHybridNotificationToUser(
                    eq(courier1Id), anyString(), anyString(), anyMap());
        }
    }

    // ================================================================
    // LEVEL 2 - Secondary Organizations
    // ================================================================

    @Nested
    @DisplayName("Nivel 2 - Organizacoes secundarias")
    class Level2Tests {

        @Test
        @DisplayName("Notifica couriers de organizacoes secundarias")
        void notificaCouriersOrgsSecundarias() throws Exception {
            Delivery delivery = makeDelivery(1L);
            Organization primaryOrg = makeOrganization(1L, "Org Principal");
            Organization secondaryOrg = makeOrganization(2L, "Org Secundaria");
            User courier2 = makeUser(courier2Id, "Motoboy B", User.Role.COURIER);

            ClientContract primaryContract = makePrimaryContract(delivery.getClient(), primaryOrg);
            ClientContract secondaryContract = makeSecondaryContract(delivery.getClient(), secondaryOrg);

            // Level 1: no primary contract (skip to level 2 immediately)
            when(clientContractRepository.findPrimaryByClient(delivery.getClient()))
                    .thenReturn(Optional.empty());

            // Level 2: secondary contracts
            when(clientContractRepository.findActiveByClient(delivery.getClient()))
                    .thenReturn(List.of(primaryContract, secondaryContract));
            when(employmentContractRepository.findActiveByOrganization(secondaryOrg))
                    .thenReturn(List.of(makeEmploymentContract(courier2, secondaryOrg)));
            when(userRepository.findAvailableCouriersNearbyWithVehicleType(
                    eq(delivery.getFromLatitude()), eq(delivery.getFromLongitude()),
                    eq(5.0), eq(VehicleType.MOTORCYCLE), anyList()))
                    .thenReturn(List.of(courier2));

            // Delivery accepted after level 2
            Delivery acceptedDelivery = makeDelivery(1L);
            acceptedDelivery.setStatus(Delivery.DeliveryStatus.ACCEPTED);
            when(deliveryRepository.findById(1L))
                    .thenReturn(Optional.of(delivery))
                    .thenReturn(Optional.of(acceptedDelivery));

            stubPushTokensFor(courier2);

            CompletableFuture<Void> future = deliveryNotificationService.notifyAvailableDrivers(delivery);
            future.get();

            verify(pushNotificationService).sendHybridNotificationToUser(
                    eq(courier2Id), anyString(), anyString(), anyMap());
        }

        @Test
        @DisplayName("Exclui couriers ja notificados no nivel 1")
        void excluiCouriersJaNotificadosNoNivel1() throws Exception {
            Delivery delivery = makeDelivery(1L);
            Organization primaryOrg = makeOrganization(1L, "Org Principal");
            Organization secondaryOrg = makeOrganization(2L, "Org Secundaria");
            User courier1 = makeUser(courier1Id, "Motoboy A", User.Role.COURIER);

            ClientContract primaryContract = makePrimaryContract(delivery.getClient(), primaryOrg);
            ClientContract secondaryContract = makeSecondaryContract(delivery.getClient(), secondaryOrg);

            // Level 1: finds courier1 in primary org
            when(clientContractRepository.findPrimaryByClient(delivery.getClient()))
                    .thenReturn(Optional.of(primaryContract));
            when(employmentContractRepository.findActiveByOrganization(primaryOrg))
                    .thenReturn(List.of(makeEmploymentContract(courier1, primaryOrg)));
            // Level 2: courier1 also in secondary org
            when(clientContractRepository.findActiveByClient(delivery.getClient()))
                    .thenReturn(List.of(primaryContract, secondaryContract));
            when(employmentContractRepository.findActiveByOrganization(secondaryOrg))
                    .thenReturn(List.of(makeEmploymentContract(courier1, secondaryOrg)));

            // All levels use same repo call — courier1 is always found
            when(userRepository.findAvailableCouriersNearbyWithVehicleType(
                    anyDouble(), anyDouble(), anyDouble(), eq(VehicleType.MOTORCYCLE), anyList()))
                    .thenReturn(List.of(courier1));

            // Delivery stays PENDING throughout all levels
            Delivery pendingDelivery = makeDelivery(1L);
            pendingDelivery.setStatus(Delivery.DeliveryStatus.PENDING);

            when(deliveryRepository.findById(1L))
                    .thenReturn(Optional.of(pendingDelivery));

            stubPushTokensFor(courier1);

            CompletableFuture<Void> future = deliveryNotificationService.notifyAvailableDrivers(delivery);
            future.get();

            // courier1 should only be notified ONCE (level 1), not again in level 2 or 3
            verify(pushNotificationService, times(1)).sendHybridNotificationToUser(
                    eq(courier1Id), anyString(), anyString(), anyMap());
        }

        @Test
        @DisplayName("Sem contratos secundarios retorna false")
        void semContratosSecundarios() throws Exception {
            Delivery delivery = makeDelivery(1L);
            Organization primaryOrg = makeOrganization(1L, "Org Principal");
            ClientContract primaryContract = makePrimaryContract(delivery.getClient(), primaryOrg);

            // Level 1: no primary contract
            when(clientContractRepository.findPrimaryByClient(delivery.getClient()))
                    .thenReturn(Optional.empty());
            // Level 2: only primary contract (filtered out as secondary)
            when(clientContractRepository.findActiveByClient(delivery.getClient()))
                    .thenReturn(List.of(primaryContract));
            // Level 3: no couriers
            when(userRepository.findAvailableCouriersNearbyWithVehicleType(
                    anyDouble(), anyDouble(), anyDouble(), any(VehicleType.class), anyList()))
                    .thenReturn(List.of());

            CompletableFuture<Void> future = deliveryNotificationService.notifyAvailableDrivers(delivery);
            future.get();

            verify(pushNotificationService, never()).sendHybridNotificationToUser(
                    any(), anyString(), anyString(), anyMap());
        }
    }

    // ================================================================
    // LEVEL 3 - All Nearby Couriers (Broadcast)
    // ================================================================

    @Nested
    @DisplayName("Nivel 3 - Broadcast todos couriers proximos")
    class Level3Tests {

        @Test
        @DisplayName("Notifica couriers proximos que nao foram notificados nos niveis anteriores")
        void notificaCouriersNovosNoNivel3() throws Exception {
            Delivery delivery = makeDelivery(1L);
            User courier3 = makeUser(courier3Id, "Motoboy C", User.Role.COURIER);

            // Level 1: no primary contract
            when(clientContractRepository.findPrimaryByClient(delivery.getClient()))
                    .thenReturn(Optional.empty());
            // Level 2: no contracts
            when(clientContractRepository.findActiveByClient(delivery.getClient()))
                    .thenReturn(List.of());
            // Level 3: finds courier3
            when(userRepository.findAvailableCouriersNearbyWithVehicleType(
                    eq(delivery.getFromLatitude()), eq(delivery.getFromLongitude()),
                    eq(5.0), eq(VehicleType.MOTORCYCLE), anyList()))
                    .thenReturn(List.of(courier3));

            when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));
            stubPushTokensFor(courier3);

            CompletableFuture<Void> future = deliveryNotificationService.notifyAvailableDrivers(delivery);
            future.get();

            verify(pushNotificationService).sendHybridNotificationToUser(
                    eq(courier3Id), anyString(), anyString(), anyMap());
        }

        @Test
        @DisplayName("Nivel 3 sem couriers proximos nao envia notificacao")
        void nivel3SemCouriersProximos() throws Exception {
            Delivery delivery = makeDelivery(1L);

            // Level 1 + 2: empty
            when(clientContractRepository.findPrimaryByClient(delivery.getClient()))
                    .thenReturn(Optional.empty());
            when(clientContractRepository.findActiveByClient(delivery.getClient()))
                    .thenReturn(List.of());
            // Level 3: empty in both radii
            when(userRepository.findAvailableCouriersNearbyWithVehicleType(
                    anyDouble(), anyDouble(), eq(5.0), any(VehicleType.class), anyList()))
                    .thenReturn(List.of());
            when(userRepository.findAvailableCouriersNearbyWithVehicleType(
                    anyDouble(), anyDouble(), eq(30.0), any(VehicleType.class), anyList()))
                    .thenReturn(List.of());

            CompletableFuture<Void> future = deliveryNotificationService.notifyAvailableDrivers(delivery);
            future.get();

            verify(pushNotificationService, never()).sendHybridNotificationToUser(
                    any(), anyString(), anyString(), anyMap());
        }

        @Test
        @DisplayName("Nivel 3 tenta raio estendido quando raio inicial esta vazio")
        void nivel3TentaRaioEstendido() throws Exception {
            Delivery delivery = makeDelivery(1L);
            User courier4 = makeUser(courier4Id, "Motoboy D", User.Role.COURIER);

            // Level 1 + 2: empty
            when(clientContractRepository.findPrimaryByClient(delivery.getClient()))
                    .thenReturn(Optional.empty());
            when(clientContractRepository.findActiveByClient(delivery.getClient()))
                    .thenReturn(List.of());
            // Level 3: 5km empty, 30km finds courier4
            when(userRepository.findAvailableCouriersNearbyWithVehicleType(
                    anyDouble(), anyDouble(), eq(5.0), eq(VehicleType.MOTORCYCLE), anyList()))
                    .thenReturn(List.of());
            when(userRepository.findAvailableCouriersNearbyWithVehicleType(
                    anyDouble(), anyDouble(), eq(30.0), eq(VehicleType.MOTORCYCLE), anyList()))
                    .thenReturn(List.of(courier4));

            when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));
            stubPushTokensFor(courier4);

            CompletableFuture<Void> future = deliveryNotificationService.notifyAvailableDrivers(delivery);
            future.get();

            verify(pushNotificationService).sendHybridNotificationToUser(
                    eq(courier4Id), anyString(), anyString(), anyMap());
        }
    }

    // ================================================================
    // DELIVERY ACCEPTED DURING NOTIFICATION
    // ================================================================

    @Nested
    @DisplayName("Delivery aceita durante notificacao")
    class DeliveryAcceptedTests {

        @Test
        @DisplayName("Para de notificar quando delivery e aceita apos nivel 1")
        void paraQuandoAceitaAposNivel1() throws Exception {
            Delivery delivery = makeDelivery(1L);
            Organization primaryOrg = makeOrganization(1L, "Org Principal");
            User courier1 = makeUser(courier1Id, "Motoboy A", User.Role.COURIER);
            User courier3 = makeUser(courier3Id, "Motoboy C", User.Role.COURIER);

            ClientContract primaryContract = makePrimaryContract(delivery.getClient(), primaryOrg);

            when(clientContractRepository.findPrimaryByClient(delivery.getClient()))
                    .thenReturn(Optional.of(primaryContract));
            when(employmentContractRepository.findActiveByOrganization(primaryOrg))
                    .thenReturn(List.of(makeEmploymentContract(courier1, primaryOrg)));
            when(userRepository.findAvailableCouriersNearbyWithVehicleType(
                    anyDouble(), anyDouble(), eq(5.0), eq(VehicleType.MOTORCYCLE), anyList()))
                    .thenReturn(List.of(courier1));

            stubPushTokensFor(courier1, courier3);

            // Delivery: PENDING before L1 notification, ACCEPTED after L1 timeout
            Delivery acceptedDelivery = makeDelivery(1L);
            acceptedDelivery.setStatus(Delivery.DeliveryStatus.ACCEPTED);
            when(deliveryRepository.findById(1L))
                    .thenReturn(Optional.of(delivery))         // isDeliveryStillPending check before notification
                    .thenReturn(Optional.of(acceptedDelivery)); // isDeliveryStillPending check after timeout

            CompletableFuture<Void> future = deliveryNotificationService.notifyAvailableDrivers(delivery);
            future.get();

            // Only courier1 should be notified (level 1); courier3 should NOT be notified
            verify(pushNotificationService, times(1)).sendHybridNotificationToUser(
                    eq(courier1Id), anyString(), anyString(), anyMap());
            verify(pushNotificationService, never()).sendHybridNotificationToUser(
                    eq(courier3Id), anyString(), anyString(), anyMap());
        }
    }

    // ================================================================
    // EDGE CASES
    // ================================================================

    @Nested
    @DisplayName("Edge cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Courier sem push token ativo e ignorado")
        void courierSemPushToken() throws Exception {
            Delivery delivery = makeDelivery(1L);
            Organization primaryOrg = makeOrganization(1L, "Org Principal");
            User courier1 = makeUser(courier1Id, "Motoboy A", User.Role.COURIER);

            ClientContract primaryContract = makePrimaryContract(delivery.getClient(), primaryOrg);

            when(clientContractRepository.findPrimaryByClient(delivery.getClient()))
                    .thenReturn(Optional.of(primaryContract));
            when(employmentContractRepository.findActiveByOrganization(primaryOrg))
                    .thenReturn(List.of(makeEmploymentContract(courier1, primaryOrg)));
            when(userRepository.findAvailableCouriersNearbyWithVehicleType(
                    anyDouble(), anyDouble(), eq(5.0), eq(VehicleType.MOTORCYCLE), anyList()))
                    .thenReturn(List.of(courier1));

            // Courier has NO push tokens
            when(userPushTokenRepository.findByUserIdAndIsActiveTrue(courier1Id))
                    .thenReturn(List.of());

            // Delivery accepted after L1 timeout
            Delivery acceptedDelivery = makeDelivery(1L);
            acceptedDelivery.setStatus(Delivery.DeliveryStatus.ACCEPTED);
            when(deliveryRepository.findById(1L))
                    .thenReturn(Optional.of(delivery))
                    .thenReturn(Optional.of(acceptedDelivery));

            CompletableFuture<Void> future = deliveryNotificationService.notifyAvailableDrivers(delivery);
            future.get();

            // Push notification should NOT be sent (no token)
            verify(pushNotificationService, never()).sendHybridNotificationToUser(
                    any(), anyString(), anyString(), anyMap());
        }

        @Test
        @DisplayName("Delivery RIDE usa filtro PASSENGER_TRANSPORT")
        void deliveryRideUsaFiltroPassengerTransport() throws Exception {
            Delivery delivery = makeDelivery(1L);
            delivery.setDeliveryType(Delivery.DeliveryType.RIDE);
            delivery.setPreferredVehicleType(Delivery.PreferredVehicleType.CAR);
            // Client set as CUSTOMER so resolveVehicleTypeFilter returns CAR
            delivery.getClient().setRole(User.Role.CUSTOMER);

            User courier1 = makeUser(courier1Id, "Motorista A", User.Role.COURIER);

            // Level 1 + 2: empty
            when(clientContractRepository.findPrimaryByClient(delivery.getClient()))
                    .thenReturn(Optional.empty());
            when(clientContractRepository.findActiveByClient(delivery.getClient()))
                    .thenReturn(List.of());

            // Level 3: finds courier with CAR + PASSENGER_TRANSPORT filter
            when(userRepository.findAvailableCouriersNearbyWithVehicleType(
                    anyDouble(), anyDouble(), eq(5.0), eq(VehicleType.CAR),
                    eq(List.of("PASSENGER_TRANSPORT", "BOTH"))))
                    .thenReturn(List.of(courier1));

            when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));
            stubPushTokensFor(courier1);

            CompletableFuture<Void> future = deliveryNotificationService.notifyAvailableDrivers(delivery);
            future.get();

            verify(pushNotificationService).sendHybridNotificationToUser(
                    eq(courier1Id), anyString(), anyString(), anyMap());
        }

        @Test
        @DisplayName("PreferredVehicleType ANY usa busca sem filtro de veiculo")
        void preferredVehicleTypeAnyNaoFiltra() throws Exception {
            Delivery delivery = makeDelivery(1L);
            delivery.setPreferredVehicleType(Delivery.PreferredVehicleType.ANY);
            delivery.getClient().setRole(User.Role.CUSTOMER);

            User courier1 = makeUser(courier1Id, "Motoboy A", User.Role.COURIER);

            // Level 1 + 2: empty
            when(clientContractRepository.findPrimaryByClient(delivery.getClient()))
                    .thenReturn(Optional.empty());
            when(clientContractRepository.findActiveByClient(delivery.getClient()))
                    .thenReturn(List.of());

            // Level 3: findAvailableCouriersNearby (without vehicle type filter)
            when(userRepository.findAvailableCouriersNearby(
                    anyDouble(), anyDouble(), eq(5.0), eq(List.of("DELIVERY", "BOTH"))))
                    .thenReturn(List.of(courier1));

            when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));
            stubPushTokensFor(courier1);

            CompletableFuture<Void> future = deliveryNotificationService.notifyAvailableDrivers(delivery);
            future.get();

            // Should call findAvailableCouriersNearby (without vehicle type), not WithVehicleType
            verify(userRepository).findAvailableCouriersNearby(
                    anyDouble(), anyDouble(), eq(5.0), anyList());
            verify(pushNotificationService).sendHybridNotificationToUser(
                    eq(courier1Id), anyString(), anyString(), anyMap());
        }

        @Test
        @DisplayName("CLIENT sempre filtra MOTORCYCLE independente de preferredVehicleType")
        void clientSempreFiltraMoto() throws Exception {
            Delivery delivery = makeDelivery(1L);
            delivery.setPreferredVehicleType(Delivery.PreferredVehicleType.CAR); // CLIENT ignora isso
            // Client role stays CLIENT

            // Level 1 + 2: empty
            when(clientContractRepository.findPrimaryByClient(delivery.getClient()))
                    .thenReturn(Optional.empty());
            when(clientContractRepository.findActiveByClient(delivery.getClient()))
                    .thenReturn(List.of());

            // Level 3: should be called with MOTORCYCLE regardless of preferredVehicleType
            when(userRepository.findAvailableCouriersNearbyWithVehicleType(
                    anyDouble(), anyDouble(), eq(5.0), eq(VehicleType.MOTORCYCLE),
                    eq(List.of("DELIVERY", "BOTH"))))
                    .thenReturn(List.of());
            when(userRepository.findAvailableCouriersNearbyWithVehicleType(
                    anyDouble(), anyDouble(), eq(30.0), eq(VehicleType.MOTORCYCLE),
                    eq(List.of("DELIVERY", "BOTH"))))
                    .thenReturn(List.of());

            CompletableFuture<Void> future = deliveryNotificationService.notifyAvailableDrivers(delivery);
            future.get();

            // Verify MOTORCYCLE was used, not CAR
            verify(userRepository, never()).findAvailableCouriersNearbyWithVehicleType(
                    anyDouble(), anyDouble(), anyDouble(), eq(VehicleType.CAR), anyList());
        }
    }
}
