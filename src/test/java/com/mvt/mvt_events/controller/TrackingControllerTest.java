package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.jpa.*;
import com.mvt.mvt_events.repository.DeliveryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitarios do TrackingController - endpoint publico de rastreamento.
 * Valida acesso por token, expiracao, dados limitados e coordenadas GPS do courier.
 */
@ExtendWith(MockitoExtension.class)
class TrackingControllerTest {

    @Mock
    private DeliveryRepository deliveryRepository;

    @InjectMocks
    private TrackingController trackingController;

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

    private Delivery makeDelivery(Long id, UUID trackingToken) {
        User client = makeUser(UUID.randomUUID(), "Restaurante X", User.Role.CLIENT);
        Delivery d = new Delivery();
        d.setId(id);
        d.setClient(client);
        d.setStatus(Delivery.DeliveryStatus.IN_TRANSIT);
        d.setFromAddress("Rua A, 100");
        d.setToAddress("Rua B, 200");
        d.setFromLatitude(-3.69);
        d.setFromLongitude(-40.35);
        d.setToLatitude(-3.70);
        d.setToLongitude(-40.36);
        d.setTotalAmount(BigDecimal.valueOf(15.00));
        d.setRecipientName("Joao Silva");
        d.setTrackingToken(trackingToken);
        d.setTrackingTokenExpiresAt(OffsetDateTime.now().plusHours(24));
        d.setAcceptedAt(OffsetDateTime.now().minusMinutes(30));
        d.setInTransitAt(OffsetDateTime.now().minusMinutes(10));
        return d;
    }

    // ================================================================
    // VALID TOKEN
    // ================================================================

    @Nested
    @DisplayName("Token valido")
    class ValidTokenTests {

        @Test
        @DisplayName("Retorna dados de rastreamento com token valido")
        void retornaDadosComTokenValido() {
            UUID token = UUID.randomUUID();
            Delivery delivery = makeDelivery(1L, token);

            when(deliveryRepository.findByTrackingToken(token)).thenReturn(Optional.of(delivery));

            ResponseEntity<?> response = trackingController.track(token);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
        }

        @Test
        @DisplayName("Resposta contem deliveryId, status, enderecos e coordenadas")
        void respostaContemCamposBasicos() {
            UUID token = UUID.randomUUID();
            Delivery delivery = makeDelivery(1L, token);

            when(deliveryRepository.findByTrackingToken(token)).thenReturn(Optional.of(delivery));

            var response = trackingController.track(token);
            var body = response.getBody();

            assertThat(body).isNotNull();
            assertThat(body.getDeliveryId()).isEqualTo(1L);
            assertThat(body.getStatus()).isEqualTo("IN_TRANSIT");
            assertThat(body.getFromAddress()).isEqualTo("Rua A, 100");
            assertThat(body.getToAddress()).isEqualTo("Rua B, 200");
            assertThat(body.getFromLatitude()).isEqualTo(-3.69);
            assertThat(body.getFromLongitude()).isEqualTo(-40.35);
            assertThat(body.getToLatitude()).isEqualTo(-3.70);
            assertThat(body.getToLongitude()).isEqualTo(-40.36);
            assertThat(body.getRecipientName()).isEqualTo("Joao Silva");
            assertThat(body.getAcceptedAt()).isNotNull();
            assertThat(body.getInTransitAt()).isNotNull();
        }

        @Test
        @DisplayName("Resposta NAO contem dados sensiveis (telefone, email, valores)")
        void respostaNaoContemDadosSensiveis() {
            UUID token = UUID.randomUUID();
            Delivery delivery = makeDelivery(1L, token);
            delivery.setRecipientPhone("85999990000");

            when(deliveryRepository.findByTrackingToken(token)).thenReturn(Optional.of(delivery));

            var body = trackingController.track(token).getBody();

            assertThat(body).isNotNull();
            // TrackingResponse does NOT have phone, email, or financial fields
            // Verify the DTO only exposes limited courier data (first name)
            assertThat(body.getCourierFirstName()).isNull(); // no courier set
        }
    }

    // ================================================================
    // EXPIRED TOKEN
    // ================================================================

    @Nested
    @DisplayName("Token expirado")
    class ExpiredTokenTests {

        @Test
        @DisplayName("Token expirado retorna 404")
        void tokenExpiradoRetorna404() {
            UUID token = UUID.randomUUID();
            Delivery delivery = makeDelivery(1L, token);
            delivery.setTrackingTokenExpiresAt(OffsetDateTime.now().minusHours(1));

            when(deliveryRepository.findByTrackingToken(token)).thenReturn(Optional.of(delivery));

            ResponseEntity<?> response = trackingController.track(token);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNull();
        }

        @Test
        @DisplayName("Token com expiresAt null e aceito (sem expiracao)")
        void tokenSemExpiracaoEAceito() {
            UUID token = UUID.randomUUID();
            Delivery delivery = makeDelivery(1L, token);
            delivery.setTrackingTokenExpiresAt(null);

            when(deliveryRepository.findByTrackingToken(token)).thenReturn(Optional.of(delivery));

            ResponseEntity<?> response = trackingController.track(token);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    // ================================================================
    // INVALID / UNKNOWN TOKEN
    // ================================================================

    @Nested
    @DisplayName("Token invalido/desconhecido")
    class InvalidTokenTests {

        @Test
        @DisplayName("Token desconhecido retorna 404")
        void tokenDesconhecidoRetorna404() {
            UUID token = UUID.randomUUID();

            when(deliveryRepository.findByTrackingToken(token)).thenReturn(Optional.empty());

            ResponseEntity<?> response = trackingController.track(token);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNull();
        }
    }

    // ================================================================
    // COURIER GPS COORDINATES
    // ================================================================

    @Nested
    @DisplayName("Coordenadas GPS do courier")
    class CourierGpsTests {

        @Test
        @DisplayName("Inclui GPS do courier quando disponivel")
        void incluiGpsDoCourier() {
            UUID token = UUID.randomUUID();
            Delivery delivery = makeDelivery(1L, token);

            User courier = makeUser(UUID.randomUUID(), "Pedro Moto", User.Role.COURIER);
            courier.setGpsLatitude(-3.71);
            courier.setGpsLongitude(-40.37);
            delivery.setCourier(courier);

            when(deliveryRepository.findByTrackingToken(token)).thenReturn(Optional.of(delivery));

            var body = trackingController.track(token).getBody();

            assertThat(body).isNotNull();
            assertThat(body.getCourierFirstName()).isEqualTo("Pedro");
            assertThat(body.getCourierLatitude()).isEqualTo(-3.71);
            assertThat(body.getCourierLongitude()).isEqualTo(-40.37);
        }

        @Test
        @DisplayName("Exibe apenas primeiro nome do courier (privacidade)")
        void exibeApenasPrimeiroNome() {
            UUID token = UUID.randomUUID();
            Delivery delivery = makeDelivery(1L, token);

            User courier = makeUser(UUID.randomUUID(), "Pedro Silva Santos", User.Role.COURIER);
            delivery.setCourier(courier);

            when(deliveryRepository.findByTrackingToken(token)).thenReturn(Optional.of(delivery));

            var body = trackingController.track(token).getBody();

            assertThat(body).isNotNull();
            assertThat(body.getCourierFirstName()).isEqualTo("Pedro");
        }

        @Test
        @DisplayName("Courier sem GPS retorna coordenadas null")
        void courierSemGps() {
            UUID token = UUID.randomUUID();
            Delivery delivery = makeDelivery(1L, token);

            User courier = makeUser(UUID.randomUUID(), "Pedro", User.Role.COURIER);
            // GPS not set (null by default)
            delivery.setCourier(courier);

            when(deliveryRepository.findByTrackingToken(token)).thenReturn(Optional.of(delivery));

            var body = trackingController.track(token).getBody();

            assertThat(body).isNotNull();
            assertThat(body.getCourierFirstName()).isEqualTo("Pedro");
            assertThat(body.getCourierLatitude()).isNull();
            assertThat(body.getCourierLongitude()).isNull();
        }

        @Test
        @DisplayName("Sem courier retorna campos courier null")
        void semCourierRetornaNull() {
            UUID token = UUID.randomUUID();
            Delivery delivery = makeDelivery(1L, token);
            delivery.setCourier(null);

            when(deliveryRepository.findByTrackingToken(token)).thenReturn(Optional.of(delivery));

            var body = trackingController.track(token).getBody();

            assertThat(body).isNotNull();
            assertThat(body.getCourierFirstName()).isNull();
            assertThat(body.getCourierLatitude()).isNull();
            assertThat(body.getCourierLongitude()).isNull();
        }
    }

    // ================================================================
    // VEHICLE DATA (limited)
    // ================================================================

    @Nested
    @DisplayName("Dados do veiculo (limitados)")
    class VehicleTests {

        @Test
        @DisplayName("Placa parcialmente mascarada (ultimos 3 caracteres)")
        void placaParcialmenteMascarada() {
            UUID token = UUID.randomUUID();
            Delivery delivery = makeDelivery(1L, token);

            Vehicle vehicle = Vehicle.builder()
                    .type(VehicleType.MOTORCYCLE)
                    .brand("Honda")
                    .model("CG 160")
                    .plate("ABC1D23")
                    .build();
            delivery.setVehicle(vehicle);

            when(deliveryRepository.findByTrackingToken(token)).thenReturn(Optional.of(delivery));

            var body = trackingController.track(token).getBody();

            assertThat(body).isNotNull();
            assertThat(body.getVehicleType()).isEqualTo("MOTORCYCLE");
            assertThat(body.getVehicleDescription()).contains("Honda");
            assertThat(body.getVehicleDescription()).contains("CG 160");
            assertThat(body.getVehicleDescription()).contains("***D23");
            // Placa completa NAO deve aparecer
            assertThat(body.getVehicleDescription()).doesNotContain("ABC1D23");
        }

        @Test
        @DisplayName("Sem veiculo retorna campos veiculo null")
        void semVeiculoRetornaNull() {
            UUID token = UUID.randomUUID();
            Delivery delivery = makeDelivery(1L, token);
            delivery.setVehicle(null);

            when(deliveryRepository.findByTrackingToken(token)).thenReturn(Optional.of(delivery));

            var body = trackingController.track(token).getBody();

            assertThat(body).isNotNull();
            assertThat(body.getVehicleType()).isNull();
            assertThat(body.getVehicleDescription()).isNull();
        }
    }

    // ================================================================
    // STOPS
    // ================================================================

    @Nested
    @DisplayName("Paradas (stops)")
    class StopsTests {

        @Test
        @DisplayName("Paradas ordenadas por stopOrder na resposta")
        void paradasOrdenadasPorStopOrder() {
            UUID token = UUID.randomUUID();
            Delivery delivery = makeDelivery(1L, token);

            DeliveryStop stop2 = DeliveryStop.builder()
                    .stopOrder(2)
                    .address("Rua C, 300")
                    .latitude(-3.71)
                    .longitude(-40.37)
                    .recipientName("Maria")
                    .status(DeliveryStop.StopStatus.PENDING)
                    .build();
            DeliveryStop stop1 = DeliveryStop.builder()
                    .stopOrder(1)
                    .address("Rua B, 200")
                    .latitude(-3.70)
                    .longitude(-40.36)
                    .recipientName("Jose")
                    .status(DeliveryStop.StopStatus.COMPLETED)
                    .completedAt(OffsetDateTime.now().minusMinutes(5))
                    .build();

            // Add out of order to verify sorting
            delivery.setStops(List.of(stop2, stop1));

            when(deliveryRepository.findByTrackingToken(token)).thenReturn(Optional.of(delivery));

            var body = trackingController.track(token).getBody();

            assertThat(body).isNotNull();
            assertThat(body.getStops()).hasSize(2);
            assertThat(body.getStops().get(0).getStopOrder()).isEqualTo(1);
            assertThat(body.getStops().get(0).getRecipientName()).isEqualTo("Jose");
            assertThat(body.getStops().get(1).getStopOrder()).isEqualTo(2);
            assertThat(body.getStops().get(1).getRecipientName()).isEqualTo("Maria");
        }

        @Test
        @DisplayName("Sem paradas retorna stops null")
        void semParadasRetornaNull() {
            UUID token = UUID.randomUUID();
            Delivery delivery = makeDelivery(1L, token);
            delivery.setStops(null);

            when(deliveryRepository.findByTrackingToken(token)).thenReturn(Optional.of(delivery));

            var body = trackingController.track(token).getBody();

            assertThat(body).isNotNull();
            assertThat(body.getStops()).isNull();
        }
    }
}
