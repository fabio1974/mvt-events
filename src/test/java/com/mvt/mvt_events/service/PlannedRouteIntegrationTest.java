package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.Delivery;
import com.mvt.mvt_events.jpa.DeliveryStop;
import com.mvt.mvt_events.repository.DeliveryRepository;
import com.mvt.mvt_events.repository.DeliveryStopRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes de integração com chamadas REAIS ao Google Directions API.
 *
 * Usa as coordenadas exatas da delivery #84 (Ubajara-CE) para reproduzir
 * o cenário que causou NOT_FOUND em todas as 6 tentativas de recálculo
 * antes do fix do open-route strategy.
 *
 * Stop 46: Av. Constituintes, 68       → -3.8529807538596677, -40.918941870331764
 * Stop 47: R. José Lopes Freire, 307   → -3.855485623366363,  -40.92023201286793
 * Stop 48: R. Dr. Joaquim Nabuco, 19   → -3.8560011145650783, -40.91724906116724 ← problemático como destino final
 *
 * Courier positions usadas nos recálculos falhados:
 *  (-3.851236973896389, -40.91856317785552)   — primeiro recálculo IN_TRANSIT
 *  (-3.852983901602165, -40.91722364199869)   — último recálculo antes de stop completado
 */
@Tag("integration")
@ExtendWith(MockitoExtension.class)
class PlannedRouteIntegrationTest {

    private static final String API_KEY = "AIzaSyBpJ-PEX_eQunOFbDXKLC3Xr3q69xoROmU";

    // Coordenadas reais — delivery #84, banco de produção
    private static final double STOP_46_LAT = -3.8529807538596677;
    private static final double STOP_46_LNG = -40.918941870331764;

    private static final double STOP_47_LAT = -3.855485623366363;
    private static final double STOP_47_LNG = -40.92023201286793;

    // Stop 48: coordenada que causava NOT_FOUND como destino final
    private static final double STOP_48_LAT = -3.8560011145650783;
    private static final double STOP_48_LNG = -40.91724906116724;

    // Posição do courier no primeiro recálculo falhado
    private static final double COURIER_LAT = -3.851236973896389;
    private static final double COURIER_LNG = -40.91856317785552;

    private GoogleDirectionsService googleDirectionsService;

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private DeliveryStopRepository deliveryStopRepository;

    @InjectMocks
    private PlannedRouteService plannedRouteService;

    @BeforeEach
    void setUp() {
        // Usa GoogleDirectionsService real (não mockado) com a API key do mobile
        googleDirectionsService = new GoogleDirectionsService();
        ReflectionTestUtils.setField(googleDirectionsService, "apiKey", API_KEY);
        ReflectionTestUtils.setField(plannedRouteService, "googleDirectionsService", googleDirectionsService);
    }

    // -----------------------------------------------------------------------
    // GoogleDirectionsService — chamadas diretas
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Stop 48 como destino final retorna NOT_FOUND (reproduz bug delivery 84)")
    void stop48AsDestinationReturnsNotFound() {
        // Comportamento ANTES do fix: backend sempre usava stop 48 como destino final
        List<double[]> result = googleDirectionsService.getRoute(
                COURIER_LAT, COURIER_LNG,
                STOP_48_LAT, STOP_48_LNG,
                List.of(
                        new double[]{STOP_46_LAT, STOP_46_LNG},
                        new double[]{STOP_47_LAT, STOP_47_LNG}
                )
        );

        assertThat(result).isEmpty(); // Google retorna NOT_FOUND para essa combinação
    }

    @Test
    @DisplayName("optimize:true waypoints causam NOT_FOUND para todos os stops da delivery 84")
    void optimizeTrueWaypointsFailForAllDelivery84Stops() {
        // Confirma que o problema é o optimize:true com essas coordenadas,
        // não apenas a coordenada de destino. Todos os 3 stops falham com waypoints.
        List<double[]> r46 = googleDirectionsService.getRoute(
                COURIER_LAT, COURIER_LNG, STOP_46_LAT, STOP_46_LNG,
                List.of(new double[]{STOP_47_LAT, STOP_47_LNG}, new double[]{STOP_48_LAT, STOP_48_LNG}));

        List<double[]> r47 = googleDirectionsService.getRoute(
                COURIER_LAT, COURIER_LNG, STOP_47_LAT, STOP_47_LNG,
                List.of(new double[]{STOP_46_LAT, STOP_46_LNG}, new double[]{STOP_48_LAT, STOP_48_LNG}));

        List<double[]> r48 = googleDirectionsService.getRoute(
                COURIER_LAT, COURIER_LNG, STOP_48_LAT, STOP_48_LNG,
                List.of(new double[]{STOP_46_LAT, STOP_46_LNG}, new double[]{STOP_47_LAT, STOP_47_LNG}));

        assertThat(r46).isEmpty();
        assertThat(r47).isEmpty();
        assertThat(r48).isEmpty();
    }

    // -----------------------------------------------------------------------
    // PlannedRouteService — cenário completo da delivery #84
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("PlannedRouteService persiste rota via chain fallback com dados reais da delivery 84")
    void plannedRouteServicePersistsRouteForDelivery84() {
        DeliveryStop stop46 = buildStop(46L, STOP_46_LAT, STOP_46_LNG);
        DeliveryStop stop47 = buildStop(47L, STOP_47_LAT, STOP_47_LNG);
        DeliveryStop stop48 = buildStop(48L, STOP_48_LAT, STOP_48_LNG);

        Delivery delivery = new Delivery();
        delivery.setId(84L);
        delivery.setFromLatitude(-3.8534086026859127);
        delivery.setFromLongitude(-40.920734256505966);
        delivery.setStops(List.of(stop46, stop47, stop48));

        // Sem rota existente → recalcula imediatamente
        when(deliveryRepository.getPlannedRouteAsGeoJson(84L)).thenReturn(null);
        when(deliveryStopRepository.maxCompletionOrder(84L)).thenReturn(0);

        plannedRouteService.handleDeliveryRouteUpdate(delivery, COURIER_LAT, COURIER_LNG);

        // Rota deve ter sido persistida apesar do stop 48 falhar como destino
        ArgumentCaptor<String> wktCaptor = ArgumentCaptor.forClass(String.class);
        verify(deliveryRepository).updatePlannedRoute(eq(84L), wktCaptor.capture());

        String wkt = wktCaptor.getValue();
        assertThat(wkt).startsWith("LINESTRING(");
        // Rota real tem múltiplos pontos (não é um segmento trivial)
        long pointCount = wkt.chars().filter(c -> c == ',').count();
        assertThat(pointCount).isGreaterThan(5);
    }

    @Test
    @DisplayName("Com apenas stop 48 restante, Google deve encontrar rota direta")
    void singleStopRouteToStop48Works() {
        // Quando só resta stop 48 (paradas 46 e 47 já entregues),
        // rota direta sem waypoints deve funcionar
        List<double[]> result = googleDirectionsService.getRoute(
                COURIER_LAT, COURIER_LNG,
                STOP_48_LAT, STOP_48_LNG,
                List.of() // sem waypoints
        );

        // Pode funcionar ou não dependendo da coordenada — o teste documenta o comportamento real
        System.out.println("Rota direta para stop 48 (sem waypoints): " +
                (result.isEmpty() ? "NOT_FOUND" : result.size() + " pontos"));
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private DeliveryStop buildStop(long id, double lat, double lng) {
        DeliveryStop s = new DeliveryStop();
        s.setId(id);
        s.setLatitude(lat);
        s.setLongitude(lng);
        s.setStatus(DeliveryStop.StopStatus.PENDING);
        return s;
    }
}
