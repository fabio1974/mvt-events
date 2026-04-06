package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.dto.DeliveryCreateRequest;
import com.mvt.mvt_events.jpa.Delivery;
import com.mvt.mvt_events.jpa.Delivery.DeliveryStatus;
import com.mvt.mvt_events.jpa.Delivery.DeliveryType;
import com.mvt.mvt_events.jpa.Delivery.PreferredVehicleType;
import com.mvt.mvt_events.jpa.DeliveryStop;
import com.mvt.mvt_events.jpa.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes unitarios para o ciclo de vida de Delivery.
 *
 * Usa JUnit puro (sem Spring context) para validar enums, transicoes de status,
 * validacao de request e multi-stop.
 */
@DisplayName("Delivery - Ciclo de Vida")
class DeliveryControllerTest {

    // ============================================
    // HELPERS
    // ============================================

    private Delivery createDelivery(DeliveryStatus status) {
        Delivery delivery = new Delivery();
        delivery.setStatus(status);
        delivery.setDeliveryType(DeliveryType.DELIVERY);
        delivery.setPreferredVehicleType(PreferredVehicleType.ANY);
        delivery.setPaymentCompleted(false);
        delivery.setPaymentCaptured(false);
        return delivery;
    }

    private User createClient() {
        User client = new User();
        client.setName("Cliente Teste");
        client.setRole(User.Role.CUSTOMER);
        return client;
    }

    private User createCourier() {
        User courier = new User();
        courier.setName("Motoboy Teste");
        courier.setRole(User.Role.COURIER);
        return courier;
    }

    // ============================================
    // DeliveryStatus enum
    // ============================================

    @Nested
    @DisplayName("DeliveryStatus enum")
    class DeliveryStatusEnum {

        @Test
        @DisplayName("contem todos os status esperados")
        void hasAllExpectedValues() {
            assertThat(DeliveryStatus.values())
                    .extracting(Enum::name)
                    .containsExactlyInAnyOrder(
                            "PENDING",
                            "ACCEPTED",
                            "WAITING_PAYMENT",
                            "IN_TRANSIT",
                            "COMPLETED",
                            "CANCELLED"
                    );
        }

        @Test
        @DisplayName("PENDING e o status padrao")
        void pendingIsDefault() {
            Delivery delivery = new Delivery();
            assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.PENDING);
        }

        @Test
        @DisplayName("valueOf funciona para todos os status")
        void valueOfWorksForAll() {
            assertThat(DeliveryStatus.valueOf("PENDING")).isEqualTo(DeliveryStatus.PENDING);
            assertThat(DeliveryStatus.valueOf("ACCEPTED")).isEqualTo(DeliveryStatus.ACCEPTED);
            assertThat(DeliveryStatus.valueOf("WAITING_PAYMENT")).isEqualTo(DeliveryStatus.WAITING_PAYMENT);
            assertThat(DeliveryStatus.valueOf("IN_TRANSIT")).isEqualTo(DeliveryStatus.IN_TRANSIT);
            assertThat(DeliveryStatus.valueOf("COMPLETED")).isEqualTo(DeliveryStatus.COMPLETED);
            assertThat(DeliveryStatus.valueOf("CANCELLED")).isEqualTo(DeliveryStatus.CANCELLED);
        }

        @Test
        @DisplayName("status invalido lanca IllegalArgumentException")
        void invalidStatus_throwsException() {
            assertThatThrownBy(() -> DeliveryStatus.valueOf("PICKED_UP"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ============================================
    // Status transitions
    // ============================================

    @Nested
    @DisplayName("Status transitions")
    class StatusTransitions {

        @Test
        @DisplayName("PENDING -> ACCEPTED e valido")
        void pendingToAccepted_isValid() {
            Delivery delivery = createDelivery(DeliveryStatus.PENDING);
            assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.PENDING);

            // Simular aceitacao
            delivery.setStatus(DeliveryStatus.ACCEPTED);
            delivery.setCourier(createCourier());
            delivery.setAcceptedAt(OffsetDateTime.now());

            assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.ACCEPTED);
            assertThat(delivery.getCourier()).isNotNull();
            assertThat(delivery.getAcceptedAt()).isNotNull();
        }

        @Test
        @DisplayName("ACCEPTED -> IN_TRANSIT e valido")
        void acceptedToInTransit_isValid() {
            Delivery delivery = createDelivery(DeliveryStatus.ACCEPTED);
            delivery.setCourier(createCourier());
            delivery.setAcceptedAt(OffsetDateTime.now());

            // Simular inicio do transporte
            delivery.setStatus(DeliveryStatus.IN_TRANSIT);
            delivery.setInTransitAt(OffsetDateTime.now());

            assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.IN_TRANSIT);
            assertThat(delivery.getInTransitAt()).isNotNull();
        }

        @Test
        @DisplayName("IN_TRANSIT -> COMPLETED e valido")
        void inTransitToCompleted_isValid() {
            Delivery delivery = createDelivery(DeliveryStatus.IN_TRANSIT);
            delivery.setCourier(createCourier());
            delivery.setPickedUpAt(OffsetDateTime.now().minusMinutes(30));
            delivery.setInTransitAt(OffsetDateTime.now().minusMinutes(30));

            // Simular conclusao
            delivery.setStatus(DeliveryStatus.COMPLETED);
            delivery.setCompletedAt(OffsetDateTime.now());

            assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.COMPLETED);
            assertThat(delivery.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("PENDING -> CANCELLED e valido")
        void pendingToCancelled_isValid() {
            Delivery delivery = createDelivery(DeliveryStatus.PENDING);

            delivery.setStatus(DeliveryStatus.CANCELLED);
            delivery.setCancelledAt(OffsetDateTime.now());
            delivery.setCancellationReason("Cliente desistiu");

            assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.CANCELLED);
            assertThat(delivery.getCancellationReason()).isEqualTo("Cliente desistiu");
        }

        @Test
        @DisplayName("PENDING -> WAITING_PAYMENT e valido")
        void pendingToWaitingPayment_isValid() {
            Delivery delivery = createDelivery(DeliveryStatus.PENDING);

            delivery.setStatus(DeliveryStatus.WAITING_PAYMENT);

            assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.WAITING_PAYMENT);
        }

        @Test
        @DisplayName("actualDeliveryTimeMinutes calculado apos completar")
        void actualDeliveryTimeMinutes_calculatedAfterCompletion() {
            Delivery delivery = createDelivery(DeliveryStatus.COMPLETED);
            OffsetDateTime pickup = OffsetDateTime.now().minusMinutes(45);
            OffsetDateTime completed = OffsetDateTime.now();

            delivery.setPickedUpAt(pickup);
            delivery.setCompletedAt(completed);

            assertThat(delivery.getActualDeliveryTimeMinutes()).isNotNull();
            assertThat(delivery.getActualDeliveryTimeMinutes()).isBetween(44L, 46L);
        }

        @Test
        @DisplayName("actualDeliveryTimeMinutes null quando nao completou")
        void actualDeliveryTimeMinutes_nullWhenNotCompleted() {
            Delivery delivery = createDelivery(DeliveryStatus.IN_TRANSIT);
            delivery.setPickedUpAt(OffsetDateTime.now());
            // completedAt nao setado

            assertThat(delivery.getActualDeliveryTimeMinutes()).isNull();
        }
    }

    // ============================================
    // DeliveryCreateRequest validation
    // ============================================

    @Nested
    @DisplayName("DeliveryCreateRequest - campos obrigatorios")
    class DeliveryCreateRequestValidation {

        @Test
        @DisplayName("request valida com todos os campos obrigatorios")
        void validRequest_hasAllRequiredFields() {
            DeliveryCreateRequest request = DeliveryCreateRequest.builder()
                    .client(DeliveryCreateRequest.EntityReference.builder().id("user_123").build())
                    .fromAddress("Rua A, 100")
                    .fromLatitude(-23.5505)
                    .fromLongitude(-46.6333)
                    .toAddress("Rua B, 200")
                    .toLatitude(-23.5600)
                    .toLongitude(-46.6400)
                    .totalAmount(new BigDecimal("25.00"))
                    .distanceKm(new BigDecimal("5.2"))
                    .preferredVehicleType("MOTORCYCLE")
                    .build();

            assertThat(request.getClient()).isNotNull();
            assertThat(request.getClient().getId()).isEqualTo("user_123");
            assertThat(request.getFromAddress()).isEqualTo("Rua A, 100");
            assertThat(request.getFromLatitude()).isEqualTo(-23.5505);
            assertThat(request.getFromLongitude()).isEqualTo(-46.6333);
            assertThat(request.getToAddress()).isEqualTo("Rua B, 200");
            assertThat(request.getTotalAmount()).isEqualByComparingTo(new BigDecimal("25.00"));
        }

        @Test
        @DisplayName("request sem client: campo null")
        void requestWithoutClient_isNull() {
            DeliveryCreateRequest request = DeliveryCreateRequest.builder()
                    .fromAddress("Rua A, 100")
                    .fromLatitude(-23.5505)
                    .fromLongitude(-46.6333)
                    .build();

            assertThat(request.getClient()).isNull();
        }

        @Test
        @DisplayName("request sem fromAddress: campo null")
        void requestWithoutFromAddress_isNull() {
            DeliveryCreateRequest request = DeliveryCreateRequest.builder()
                    .client(DeliveryCreateRequest.EntityReference.builder().id("user_123").build())
                    .build();

            assertThat(request.getFromAddress()).isNull();
        }

        @Test
        @DisplayName("hasStops retorna false quando stops null")
        void hasStops_falseWhenNull() {
            DeliveryCreateRequest request = DeliveryCreateRequest.builder()
                    .client(DeliveryCreateRequest.EntityReference.builder().id("user_123").build())
                    .build();

            assertThat(request.hasStops()).isFalse();
        }

        @Test
        @DisplayName("hasStops retorna false quando stops vazio")
        void hasStops_falseWhenEmpty() {
            DeliveryCreateRequest request = DeliveryCreateRequest.builder()
                    .client(DeliveryCreateRequest.EntityReference.builder().id("user_123").build())
                    .stops(List.of())
                    .build();

            assertThat(request.hasStops()).isFalse();
        }

        @Test
        @DisplayName("hasStops retorna true quando tem stops")
        void hasStops_trueWhenPresent() {
            DeliveryCreateRequest.StopRequest stop = DeliveryCreateRequest.StopRequest.builder()
                    .address("Rua B, 200")
                    .latitude(-23.56)
                    .longitude(-46.64)
                    .build();

            DeliveryCreateRequest request = DeliveryCreateRequest.builder()
                    .client(DeliveryCreateRequest.EntityReference.builder().id("user_123").build())
                    .stops(List.of(stop))
                    .build();

            assertThat(request.hasStops()).isTrue();
        }
    }

    // ============================================
    // Multi-stop: DeliveryStop
    // ============================================

    @Nested
    @DisplayName("Multi-stop - DeliveryStop entities")
    class MultiStop {

        @Test
        @DisplayName("array de 3 stops cria 3 DeliveryStop entities")
        void threeStops_createsThreeEntities() {
            Delivery delivery = createDelivery(DeliveryStatus.PENDING);
            delivery.setClient(createClient());
            delivery.setFromAddress("Origem, 100");

            // Simular criacao de stops a partir do request
            DeliveryStop stop1 = DeliveryStop.builder()
                    .delivery(delivery)
                    .stopOrder(1)
                    .address("Destino A, 200")
                    .latitude(-23.56)
                    .longitude(-46.64)
                    .recipientName("Maria")
                    .status(DeliveryStop.StopStatus.PENDING)
                    .build();

            DeliveryStop stop2 = DeliveryStop.builder()
                    .delivery(delivery)
                    .stopOrder(2)
                    .address("Destino B, 300")
                    .latitude(-23.57)
                    .longitude(-46.65)
                    .recipientName("Joao")
                    .status(DeliveryStop.StopStatus.PENDING)
                    .build();

            DeliveryStop stop3 = DeliveryStop.builder()
                    .delivery(delivery)
                    .stopOrder(3)
                    .address("Destino C, 400")
                    .latitude(-23.58)
                    .longitude(-46.66)
                    .recipientName("Pedro")
                    .status(DeliveryStop.StopStatus.PENDING)
                    .build();

            delivery.getStops().addAll(List.of(stop1, stop2, stop3));

            assertThat(delivery.getStops()).hasSize(3);
            assertThat(delivery.getStops().get(0).getStopOrder()).isEqualTo(1);
            assertThat(delivery.getStops().get(1).getStopOrder()).isEqualTo(2);
            assertThat(delivery.getStops().get(2).getStopOrder()).isEqualTo(3);
        }

        @Test
        @DisplayName("stop status padrao e PENDING")
        void stopDefaultStatus_isPending() {
            DeliveryStop stop = DeliveryStop.builder()
                    .stopOrder(1)
                    .address("Rua X, 100")
                    .build();

            assertThat(stop.getStatus()).isEqualTo(DeliveryStop.StopStatus.PENDING);
        }

        @Test
        @DisplayName("stop pode ser marcado como COMPLETED")
        void stop_canBeCompleted() {
            DeliveryStop stop = DeliveryStop.builder()
                    .stopOrder(1)
                    .address("Rua X, 100")
                    .status(DeliveryStop.StopStatus.PENDING)
                    .build();

            stop.setStatus(DeliveryStop.StopStatus.COMPLETED);
            stop.setCompletedAt(OffsetDateTime.now());
            stop.setCompletionOrder(1);

            assertThat(stop.getStatus()).isEqualTo(DeliveryStop.StopStatus.COMPLETED);
            assertThat(stop.getCompletedAt()).isNotNull();
            assertThat(stop.getCompletionOrder()).isEqualTo(1);
        }

        @Test
        @DisplayName("stop pode ser SKIPPED")
        void stop_canBeSkipped() {
            DeliveryStop stop = DeliveryStop.builder()
                    .stopOrder(1)
                    .address("Rua X, 100")
                    .status(DeliveryStop.StopStatus.PENDING)
                    .build();

            stop.setStatus(DeliveryStop.StopStatus.SKIPPED);
            stop.setCompletionOrder(0);

            assertThat(stop.getStatus()).isEqualTo(DeliveryStop.StopStatus.SKIPPED);
            assertThat(stop.getCompletionOrder()).isEqualTo(0);
        }

        @Test
        @DisplayName("StopStatus enum tem 3 valores")
        void stopStatus_hasThreeValues() {
            assertThat(DeliveryStop.StopStatus.values())
                    .extracting(Enum::name)
                    .containsExactlyInAnyOrder("PENDING", "COMPLETED", "SKIPPED");
        }
    }

    // ============================================
    // PreferredVehicleType
    // ============================================

    @Nested
    @DisplayName("PreferredVehicleType enum")
    class PreferredVehicleTypeEnum {

        @Test
        @DisplayName("MOTORCYCLE e um tipo valido")
        void motorcycle_isValid() {
            Delivery delivery = createDelivery(DeliveryStatus.PENDING);
            delivery.setPreferredVehicleType(PreferredVehicleType.MOTORCYCLE);

            assertThat(delivery.getPreferredVehicleType()).isEqualTo(PreferredVehicleType.MOTORCYCLE);
        }

        @Test
        @DisplayName("CAR e um tipo valido")
        void car_isValid() {
            Delivery delivery = createDelivery(DeliveryStatus.PENDING);
            delivery.setPreferredVehicleType(PreferredVehicleType.CAR);

            assertThat(delivery.getPreferredVehicleType()).isEqualTo(PreferredVehicleType.CAR);
        }

        @Test
        @DisplayName("ANY e o tipo padrao")
        void any_isDefault() {
            Delivery delivery = new Delivery();
            assertThat(delivery.getPreferredVehicleType()).isEqualTo(PreferredVehicleType.ANY);
        }

        @Test
        @DisplayName("enum contem exatamente 3 valores")
        void hasExactlyThreeValues() {
            assertThat(PreferredVehicleType.values())
                    .extracting(Enum::name)
                    .containsExactlyInAnyOrder("MOTORCYCLE", "CAR", "ANY");
        }

        @Test
        @DisplayName("valueOf funciona para todos os tipos")
        void valueOf_worksForAll() {
            assertThat(PreferredVehicleType.valueOf("MOTORCYCLE")).isEqualTo(PreferredVehicleType.MOTORCYCLE);
            assertThat(PreferredVehicleType.valueOf("CAR")).isEqualTo(PreferredVehicleType.CAR);
            assertThat(PreferredVehicleType.valueOf("ANY")).isEqualTo(PreferredVehicleType.ANY);
        }
    }

    // ============================================
    // DeliveryType & PaymentTiming
    // ============================================

    @Nested
    @DisplayName("DeliveryType e PaymentTiming")
    class DeliveryTypeAndPaymentTiming {

        @Test
        @DisplayName("DELIVERY paga ON_ACCEPT")
        void delivery_paysOnAccept() {
            Delivery delivery = createDelivery(DeliveryStatus.PENDING);
            delivery.setDeliveryType(DeliveryType.DELIVERY);

            assertThat(delivery.getPaymentTiming()).isEqualTo(Delivery.PaymentTiming.ON_ACCEPT);
        }

        @Test
        @DisplayName("RIDE paga ON_TRANSIT_START")
        void ride_paysOnTransitStart() {
            Delivery delivery = createDelivery(DeliveryStatus.PENDING);
            delivery.setDeliveryType(DeliveryType.RIDE);

            assertThat(delivery.getPaymentTiming()).isEqualTo(Delivery.PaymentTiming.ON_TRANSIT_START);
        }

        @Test
        @DisplayName("DeliveryType enum tem 2 valores")
        void deliveryType_hasTwoValues() {
            assertThat(DeliveryType.values())
                    .extracting(Enum::name)
                    .containsExactlyInAnyOrder("DELIVERY", "RIDE");
        }
    }

    // ============================================
    // isFromTrustedClient
    // ============================================

    @Nested
    @DisplayName("isFromTrustedClient")
    class IsFromTrustedClient {

        @Test
        @DisplayName("CLIENT (estabelecimento) e confiavel")
        void client_isTrusted() {
            Delivery delivery = createDelivery(DeliveryStatus.PENDING);
            User client = new User();
            client.setRole(User.Role.CLIENT);
            delivery.setClient(client);

            assertThat(delivery.isFromTrustedClient()).isTrue();
        }

        @Test
        @DisplayName("CUSTOMER nao e confiavel")
        void customer_isNotTrusted() {
            Delivery delivery = createDelivery(DeliveryStatus.PENDING);
            User customer = new User();
            customer.setRole(User.Role.CUSTOMER);
            delivery.setClient(customer);

            assertThat(delivery.isFromTrustedClient()).isFalse();
        }

        @Test
        @DisplayName("sem client retorna false")
        void noClient_returnsFalse() {
            Delivery delivery = createDelivery(DeliveryStatus.PENDING);
            delivery.setClient(null);

            assertThat(delivery.isFromTrustedClient()).isFalse();
        }
    }

    // ============================================
    // Computed fields
    // ============================================

    @Nested
    @DisplayName("Computed fields")
    class ComputedFields {

        @Test
        @DisplayName("getClientName retorna nome do client")
        void clientName_returnsName() {
            Delivery delivery = createDelivery(DeliveryStatus.PENDING);
            User client = createClient();
            client.setName("Maria Silva");
            delivery.setClient(client);

            assertThat(delivery.getClientName()).isEqualTo("Maria Silva");
        }

        @Test
        @DisplayName("getClientName retorna N/A quando sem client")
        void clientName_returnsNA_whenNull() {
            Delivery delivery = createDelivery(DeliveryStatus.PENDING);
            delivery.setClient(null);

            assertThat(delivery.getClientName()).isEqualTo("N/A");
        }

        @Test
        @DisplayName("getCourierName retorna nome do courier")
        void courierName_returnsName() {
            Delivery delivery = createDelivery(DeliveryStatus.ACCEPTED);
            User courier = createCourier();
            courier.setName("Joao Motoboy");
            delivery.setCourier(courier);

            assertThat(delivery.getCourierName()).isEqualTo("Joao Motoboy");
        }

        @Test
        @DisplayName("getCourierName retorna 'Nao atribuido' quando sem courier")
        void courierName_returnsDefault_whenNull() {
            Delivery delivery = createDelivery(DeliveryStatus.PENDING);

            assertThat(delivery.getCourierName()).contains("atribu");
        }
    }
}
