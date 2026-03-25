package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.Delivery;
import com.mvt.mvt_events.jpa.DeliveryStop;
import com.mvt.mvt_events.repository.DeliveryRepository;
import com.mvt.mvt_events.repository.DeliveryStopRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PlannedRouteService.
 *
 * Google Directions is mocked — we verify:
 *  - Nearest-neighbor stop ordering
 *  - Open-route fallback (first stop as destination fails → tries next)
 *  - Cooldown: no double recalculation within 60s
 *  - Cooldown released when Google returns empty (to allow retry)
 *  - Approach route (ACCEPTED) created and updated independently of planned_route
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class PlannedRouteServiceTest {

    @Mock
    private GoogleDirectionsService googleDirectionsService;

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private DeliveryStopRepository deliveryStopRepository;

    @InjectMocks
    private PlannedRouteService service;

    // Fake polyline returned by a successful Google Directions call
    private static final List<double[]> FAKE_ROUTE = List.of(
            new double[]{-3.85, -40.91},
            new double[]{-3.86, -40.92},
            new double[]{-3.87, -40.93}
    );

    // Courier starts at Ubajara centro
    private static final double COURIER_LAT = -3.850;
    private static final double COURIER_LNG = -40.920;

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    /** Creates a PENDING stop at the given coordinates. */
    private DeliveryStop stop(long id, double lat, double lng) {
        DeliveryStop s = new DeliveryStop();
        s.setId(id);
        s.setLatitude(lat);
        s.setLongitude(lng);
        s.setStatus(DeliveryStop.StopStatus.PENDING);
        return s;
    }

    /** Creates a delivery with the given stops and no existing planned route. */
    private Delivery delivery(long id, List<DeliveryStop> stops) {
        Delivery d = new Delivery();
        d.setId(id);
        d.setFromLatitude(COURIER_LAT - 0.001);
        d.setFromLongitude(COURIER_LNG - 0.001);
        d.setStops(stops);
        return d;
    }

    // ---------------------------------------------------------------------------
    // Nearest-Neighbor Ordering
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("Nearest-neighbor ordering")
    class NearestNeighborTests {

        @Test
        @DisplayName("Stops são ordenados do mais próximo ao mais distante do courier")
        void stopsOrderedByNearestNeighbor() {
            // Stop A: 500m do courier
            // Stop B: 200m do courier — mais próximo
            // Stop C: 1km do courier, mas perto de B — vai depois de B pelo NN
            DeliveryStop stopA = stop(1L, -3.854, -40.920); // ~400m south
            DeliveryStop stopB = stop(2L, -3.852, -40.920); // ~200m south — nearest
            DeliveryStop stopC = stop(3L, -3.856, -40.925); // ~700m SW — farthest from courier, nearest from B

            Delivery d = delivery(1L, List.of(stopA, stopB, stopC));

            // No existing route → recalculates immediately
            when(deliveryRepository.getPlannedRouteAsGeoJson(1L)).thenReturn(null);
            when(googleDirectionsService.getRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyList()))
                    .thenReturn(FAKE_ROUTE);
            when(deliveryStopRepository.maxCompletionOrder(1L)).thenReturn(0);

            service.handleDeliveryRouteUpdate(d, COURIER_LAT, COURIER_LNG);

            // Capture the destination used in the first (successful) Google call
            ArgumentCaptor<Double> destLatCaptor = ArgumentCaptor.forClass(Double.class);
            ArgumentCaptor<Double> destLngCaptor = ArgumentCaptor.forClass(Double.class);
            verify(googleDirectionsService, atLeastOnce())
                    .getRoute(anyDouble(), anyDouble(), destLatCaptor.capture(), destLngCaptor.capture(), anyList());

            // The first call should use stopB (nearest) as destination
            assertThat(destLatCaptor.getAllValues().get(0)).isEqualTo(stopB.getLatitude());
        }
    }

    // ---------------------------------------------------------------------------
    // Open-Route Fallback
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("Open-route fallback")
    class OpenRouteTests {

        @Test
        @DisplayName("Quando primeiro destino falha, tenta o segundo e persiste a rota")
        void fallsBackToSecondStopWhenFirstDestinationFails() {
            // Nearest stop (A) fails as destination; next stop (B) succeeds
            DeliveryStop stopA = stop(1L, -3.852, -40.920); // nearest
            DeliveryStop stopB = stop(2L, -3.855, -40.922); // farthest

            Delivery d = delivery(1L, List.of(stopA, stopB));
            when(deliveryRepository.getPlannedRouteAsGeoJson(1L)).thenReturn(null);
            when(deliveryStopRepository.maxCompletionOrder(1L)).thenReturn(0);

            // First call (stopA as destination) → NOT_FOUND (empty)
            // Second call (stopB as destination) → OK
            when(googleDirectionsService.getRoute(
                    anyDouble(), anyDouble(),
                    eq(stopA.getLatitude()), eq(stopA.getLongitude()),
                    anyList()))
                    .thenReturn(List.of()); // NOT_FOUND simulado

            when(googleDirectionsService.getRoute(
                    anyDouble(), anyDouble(),
                    eq(stopB.getLatitude()), eq(stopB.getLongitude()),
                    anyList()))
                    .thenReturn(FAKE_ROUTE); // OK

            service.handleDeliveryRouteUpdate(d, COURIER_LAT, COURIER_LNG);

            // Rota deve ter sido persistida
            verify(deliveryRepository).updatePlannedRoute(eq(1L), anyString());

            // Dois attempts ao Google: primeiro falha, segundo sucede
            verify(googleDirectionsService, times(2))
                    .getRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyList());
        }

        @Test
        @DisplayName("Quando todos os destinos falham, cooldown é liberado para retry")
        void releasesCooldwnWhenAllDestinationsFail() {
            DeliveryStop stopA = stop(1L, -3.852, -40.920);
            DeliveryStop stopB = stop(2L, -3.855, -40.922);

            Delivery d = delivery(1L, List.of(stopA, stopB));
            when(deliveryRepository.getPlannedRouteAsGeoJson(1L)).thenReturn(null);

            // Todos os destinos retornam NOT_FOUND
            when(googleDirectionsService.getRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyList()))
                    .thenReturn(List.of());

            service.handleDeliveryRouteUpdate(d, COURIER_LAT, COURIER_LNG);

            // Rota NÃO deve ter sido persistida
            verify(deliveryRepository, never()).updatePlannedRoute(anyLong(), anyString());

            // Cooldown liberado → nova chamada não bloqueada por cooldown
            // Simulando desvio detectado → deve tentar novamente
            when(deliveryRepository.getPlannedRouteAsGeoJson(1L)).thenReturn("existing");
            when(deliveryRepository.getDistanceFromPlannedRouteMeters(1L, COURIER_LAT, COURIER_LNG))
                    .thenReturn(200.0); // acima do threshold de 100m

            service.handleDeliveryRouteUpdate(d, COURIER_LAT, COURIER_LNG);

            // Google foi chamado novamente (cooldown não está bloqueando)
            verify(googleDirectionsService, atLeast(3))
                    .getRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyList());
        }

        @Test
        @DisplayName("Quando primeiro destino funciona, apenas uma chamada ao Google")
        void onlyOneGoogleCallWhenFirstDestinationSucceeds() {
            DeliveryStop stopA = stop(1L, -3.852, -40.920);
            DeliveryStop stopB = stop(2L, -3.855, -40.922);
            DeliveryStop stopC = stop(3L, -3.858, -40.924);

            Delivery d = delivery(1L, List.of(stopA, stopB, stopC));
            when(deliveryRepository.getPlannedRouteAsGeoJson(1L)).thenReturn(null);
            when(deliveryStopRepository.maxCompletionOrder(1L)).thenReturn(0);

            // Primeiro stop como destino já funciona
            when(googleDirectionsService.getRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyList()))
                    .thenReturn(FAKE_ROUTE);

            service.handleDeliveryRouteUpdate(d, COURIER_LAT, COURIER_LNG);

            // Apenas 1 chamada ao Google (parou no primeiro sucesso)
            verify(googleDirectionsService, times(1))
                    .getRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyList());

            verify(deliveryRepository).updatePlannedRoute(eq(1L), anyString());
        }
    }

    // ---------------------------------------------------------------------------
    // Cooldown
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("Cooldown logic")
    class CooldownTests {

        @Test
        @DisplayName("Dentro do cooldown, não recalcula mesmo com desvio acima de 100m")
        void noCooldownRecalculationWithinCooldown() {
            DeliveryStop stopA = stop(1L, -3.852, -40.920);
            Delivery d = delivery(1L, List.of(stopA));

            // Rota já existe
            when(deliveryRepository.getPlannedRouteAsGeoJson(1L)).thenReturn("existing");
            when(deliveryStopRepository.maxCompletionOrder(1L)).thenReturn(0);
            when(googleDirectionsService.getRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyList()))
                    .thenReturn(FAKE_ROUTE);
            // Desvio detectado → acima do threshold
            when(deliveryRepository.getDistanceFromPlannedRouteMeters(1L, COURIER_LAT, COURIER_LNG))
                    .thenReturn(200.0);

            // Primeira chamada: recalcula e seta cooldown
            service.handleDeliveryRouteUpdate(d, COURIER_LAT, COURIER_LNG);
            verify(googleDirectionsService, times(1))
                    .getRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyList());

            // Segunda chamada imediata: cooldown ativo → Google NÃO deve ser chamado
            service.handleDeliveryRouteUpdate(d, COURIER_LAT, COURIER_LNG);
            verify(googleDirectionsService, times(1)) // ainda apenas 1
                    .getRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyList());
        }

        @Test
        @DisplayName("clearCooldown permite recálculo imediato após conclusão")
        void clearCooldownAllowsImmediateRecalculation() {
            DeliveryStop stopA = stop(1L, -3.852, -40.920);
            Delivery d = delivery(1L, List.of(stopA));

            when(deliveryRepository.getPlannedRouteAsGeoJson(1L)).thenReturn("existing");
            when(deliveryStopRepository.maxCompletionOrder(1L)).thenReturn(0);
            when(googleDirectionsService.getRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyList()))
                    .thenReturn(FAKE_ROUTE);
            when(deliveryRepository.getDistanceFromPlannedRouteMeters(1L, COURIER_LAT, COURIER_LNG))
                    .thenReturn(200.0);

            service.handleDeliveryRouteUpdate(d, COURIER_LAT, COURIER_LNG);
            verify(googleDirectionsService, times(1))
                    .getRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyList());

            // Limpa cooldown (simula entrega concluída / reinício)
            service.clearCooldown(1L);

            // Nova chamada após clear: deve recalcular
            service.handleDeliveryRouteUpdate(d, COURIER_LAT, COURIER_LNG);
            verify(googleDirectionsService, times(2))
                    .getRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyList());
        }
    }

    // ---------------------------------------------------------------------------
    // Approach Route (ACCEPTED phase)
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("Approach route (ACCEPTED phase)")
    class ApproachRouteTests {

        @Test
        @DisplayName("Cria approach_planned_route quando não existe ainda")
        void createsApproachRouteWhenAbsent() {
            Delivery d = delivery(1L, List.of());

            when(deliveryRepository.getApproachPlannedRouteAsGeoJson(1L)).thenReturn(null);
            when(googleDirectionsService.getRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyList()))
                    .thenReturn(FAKE_ROUTE);

            service.handleApproachRouteUpdate(d, COURIER_LAT, COURIER_LNG);

            verify(deliveryRepository).updateApproachPlannedRoute(eq(1L), anyString());
        }

        @Test
        @DisplayName("Approach route não afeta planned_route (campos independentes)")
        void approachRouteDoesNotTouchPlannedRoute() {
            Delivery d = delivery(1L, List.of());

            when(deliveryRepository.getApproachPlannedRouteAsGeoJson(1L)).thenReturn(null);
            when(googleDirectionsService.getRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyList()))
                    .thenReturn(FAKE_ROUTE);

            service.handleApproachRouteUpdate(d, COURIER_LAT, COURIER_LNG);

            verify(deliveryRepository, never()).updatePlannedRoute(anyLong(), anyString());
        }

        @Test
        @DisplayName("Cooldown da approach é independente do cooldown da rota principal")
        void approachAndMainCooldownAreIndependent() {
            DeliveryStop stopA = stop(1L, -3.852, -40.920);
            Delivery d = delivery(1L, List.of(stopA));

            // Abastece cooldown do planned_route
            when(deliveryRepository.getPlannedRouteAsGeoJson(1L)).thenReturn(null);
            when(deliveryStopRepository.maxCompletionOrder(1L)).thenReturn(0);
            when(googleDirectionsService.getRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyList()))
                    .thenReturn(FAKE_ROUTE);
            service.handleDeliveryRouteUpdate(d, COURIER_LAT, COURIER_LNG);

            // Cooldown do planned_route está ativo, mas approach ainda deve funcionar
            when(deliveryRepository.getApproachPlannedRouteAsGeoJson(1L)).thenReturn(null);
            service.handleApproachRouteUpdate(d, COURIER_LAT, COURIER_LNG);

            // updateApproachPlannedRoute foi chamado mesmo com cooldown do IN_TRANSIT ativo
            verify(deliveryRepository).updateApproachPlannedRoute(eq(1L), anyString());
        }

        @Test
        @DisplayName("Approach falha → cooldown liberado para retry no próximo GPS update")
        void approachFailureReleasesCooldown() {
            Delivery d = delivery(1L, List.of());

            when(deliveryRepository.getApproachPlannedRouteAsGeoJson(1L)).thenReturn(null);
            // Primeira tentativa falha
            when(googleDirectionsService.getRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyList()))
                    .thenReturn(List.of());

            service.handleApproachRouteUpdate(d, COURIER_LAT, COURIER_LNG);

            // Não persistiu
            verify(deliveryRepository, never()).updateApproachPlannedRoute(anyLong(), anyString());

            // Segunda tentativa: rota ainda null → deve tentar de novo (cooldown liberado)
            when(googleDirectionsService.getRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyList()))
                    .thenReturn(FAKE_ROUTE);

            service.handleApproachRouteUpdate(d, COURIER_LAT, COURIER_LNG);

            verify(deliveryRepository).updateApproachPlannedRoute(eq(1L), anyString());
        }
    }

    // ---------------------------------------------------------------------------
    // WKT format
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("WKT gerado corretamente")
    class WktTests {

        @Test
        @DisplayName("WKT persiste no formato LINESTRING(lng lat, ...)")
        void wktFormatIsCorrect() {
            DeliveryStop stopA = stop(1L, -3.852, -40.920);
            Delivery d = delivery(1L, List.of(stopA));

            when(deliveryRepository.getPlannedRouteAsGeoJson(1L)).thenReturn(null);
            when(deliveryStopRepository.maxCompletionOrder(1L)).thenReturn(0);
            when(googleDirectionsService.getRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyList()))
                    .thenReturn(FAKE_ROUTE);

            service.handleDeliveryRouteUpdate(d, COURIER_LAT, COURIER_LNG);

            ArgumentCaptor<String> wktCaptor = ArgumentCaptor.forClass(String.class);
            verify(deliveryRepository).updatePlannedRoute(eq(1L), wktCaptor.capture());

            String wkt = wktCaptor.getValue();
            assertThat(wkt).startsWith("LINESTRING(");
            assertThat(wkt).endsWith(")");
            // WKT usa lng lat (ordem PostGIS)
            assertThat(wkt).contains("-40.91 -3.85");
        }
    }
}
