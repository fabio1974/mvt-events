package com.mvt.mvt_events.service;

import com.mvt.mvt_events.dto.CourierEarningsResponse;
import com.mvt.mvt_events.jpa.*;
import com.mvt.mvt_events.repository.DeliveryRepository;
import com.mvt.mvt_events.repository.SiteConfigurationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitarios do CourierEarningsService -- cobre getCourierEarnings
 * com e sem filtro recent, deliveries sem pagamento PAID, e calculo de splits.
 */
@ExtendWith(MockitoExtension.class)
class CourierEarningsServiceTest {

    @Mock private DeliveryRepository deliveryRepository;
    @Mock private PaymentSplitCalculator splitCalculator;
    @Mock private SiteConfigurationRepository siteConfigurationRepository;

    @InjectMocks
    private CourierEarningsService courierEarningsService;

    // ========== Helpers ==========

    private final UUID courierId = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private SiteConfiguration makeConfig() {
        SiteConfiguration c = new SiteConfiguration();
        c.setOrganizerPercentage(BigDecimal.valueOf(5));
        c.setPlatformPercentage(BigDecimal.valueOf(8));
        c.setPricePerKm(BigDecimal.valueOf(2.50));
        c.setCarPricePerKm(BigDecimal.valueOf(3.50));
        c.setMinimumShippingFee(BigDecimal.valueOf(5));
        c.setCarMinimumShippingFee(BigDecimal.valueOf(8));
        c.setDangerFeePercentage(BigDecimal.ZERO);
        c.setHighIncomeFeePercentage(BigDecimal.ZERO);
        c.setCreditCardFeePercentage(BigDecimal.ZERO);
        c.setDeliveryHistoryDays(7);
        c.setPaymentHistoryDays(7);
        return c;
    }

    private User makeUser(UUID id, String name, User.Role role) {
        User u = new User();
        u.setId(id);
        u.setName(name);
        u.setRole(role);
        return u;
    }

    private Payment makePaidPayment(Long id, BigDecimal amount) {
        Payment p = new Payment();
        p.setId(id);
        p.setAmount(amount);
        p.setStatus(PaymentStatus.PAID);
        p.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        return p;
    }

    private Delivery makePaidDelivery(Long id, BigDecimal shippingFee, OffsetDateTime completedAt) {
        Delivery d = new Delivery();
        d.setId(id);
        d.setStatus(Delivery.DeliveryStatus.COMPLETED);
        d.setShippingFee(shippingFee);
        d.setCompletedAt(completedAt);
        d.setFromAddress("Rua A, 100");
        d.setToAddress("Rua B, 200");
        d.setDistanceKm(BigDecimal.valueOf(5));
        d.setDeliveryType(Delivery.DeliveryType.DELIVERY);
        d.setClient(makeUser(UUID.randomUUID(), "Cliente X", User.Role.CUSTOMER));

        User courier = makeUser(courierId, "Pedro Moto", User.Role.COURIER);
        d.setCourier(courier);

        Payment payment = makePaidPayment(id, shippingFee);
        d.setPayments(new ArrayList<>(List.of(payment)));

        return d;
    }

    private void stubSplitCalculator(SiteConfiguration config, BigDecimal shippingFee, boolean hasOrganizer) {
        BigDecimal feeCents = shippingFee.multiply(BigDecimal.valueOf(100));

        when(splitCalculator.toCents(shippingFee)).thenReturn(feeCents);
        when(splitCalculator.calculateCourierPercentage(config)).thenReturn(BigDecimal.valueOf(87));
        when(splitCalculator.hasValidOrganizer(any())).thenReturn(hasOrganizer);

        BigDecimal courierCents = feeCents.multiply(BigDecimal.valueOf(87))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);
        when(splitCalculator.calculateCourierAmount(feeCents, config)).thenReturn(courierCents);

        BigDecimal organizerCents = BigDecimal.ZERO;
        if (hasOrganizer) {
            organizerCents = feeCents.multiply(BigDecimal.valueOf(5))
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);
            when(splitCalculator.calculateOrganizerAmount(feeCents, config)).thenReturn(organizerCents);
            when(splitCalculator.calculatePlatformPercentage(config, true)).thenReturn(BigDecimal.valueOf(8));
        } else {
            when(splitCalculator.calculatePlatformPercentage(config, false)).thenReturn(BigDecimal.valueOf(13));
        }

        BigDecimal platformCents = feeCents.subtract(courierCents).subtract(organizerCents);
        when(splitCalculator.calculatePlatformAmount(feeCents, courierCents, organizerCents))
                .thenReturn(platformCents);

        BigDecimal courierReais = courierCents.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal organizerReais = organizerCents.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal platformReais = platformCents.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        when(splitCalculator.toReais(courierCents, 2)).thenReturn(courierReais);
        when(splitCalculator.toReais(organizerCents, 2)).thenReturn(organizerReais);
        when(splitCalculator.toReais(platformCents, 2)).thenReturn(platformReais);
    }

    // ================================================================
    // getCourierEarnings
    // ================================================================

    @Nested
    @DisplayName("getCourierEarnings() -- Historico de recebimentos")
    class GetCourierEarningsTests {

        @Test
        @DisplayName("Retorna recebimentos de deliveries pagas")
        void retornaRecebimentosPagos() {
            SiteConfiguration config = makeConfig();
            BigDecimal fee = BigDecimal.valueOf(25.00);
            Delivery delivery = makePaidDelivery(1L, fee, OffsetDateTime.now().minusDays(1));

            when(siteConfigurationRepository.findActiveConfiguration()).thenReturn(Optional.of(config));
            when(deliveryRepository.findByCourierIdAndStatus(courierId, Delivery.DeliveryStatus.COMPLETED))
                    .thenReturn(List.of(delivery));
            stubSplitCalculator(config, fee, false);

            CourierEarningsResponse result = courierEarningsService.getCourierEarnings(courierId, null);

            assertThat(result.getTotalDeliveries()).isEqualTo(1);
            assertThat(result.getTotalEarnings()).isGreaterThan(BigDecimal.ZERO);
            assertThat(result.getDeliveries()).hasSize(1);

            CourierEarningsResponse.DeliveryEarningDetail detail = result.getDeliveries().get(0);
            assertThat(detail.getDeliveryId()).isEqualTo(1L);
            assertThat(detail.getTotalAmount()).isEqualByComparingTo(fee);
            assertThat(detail.getCourierAmount()).isGreaterThan(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Retorna vazio quando nao ha deliveries completadas")
        void retornaVazioSemDeliveries() {
            SiteConfiguration config = makeConfig();
            when(siteConfigurationRepository.findActiveConfiguration()).thenReturn(Optional.of(config));
            when(deliveryRepository.findByCourierIdAndStatus(courierId, Delivery.DeliveryStatus.COMPLETED))
                    .thenReturn(List.of());

            CourierEarningsResponse result = courierEarningsService.getCourierEarnings(courierId, null);

            assertThat(result.getTotalDeliveries()).isEqualTo(0);
            assertThat(result.getTotalEarnings()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getDeliveries()).isEmpty();
        }

        @Test
        @DisplayName("Filtra deliveries sem pagamento PAID")
        void filtraDeliveriesSemPagamentoPaid() {
            SiteConfiguration config = makeConfig();
            Delivery paidDelivery = makePaidDelivery(1L, BigDecimal.valueOf(20), OffsetDateTime.now());

            // Delivery sem pagamento PAID
            Delivery unpaidDelivery = new Delivery();
            unpaidDelivery.setId(2L);
            unpaidDelivery.setStatus(Delivery.DeliveryStatus.COMPLETED);
            unpaidDelivery.setShippingFee(BigDecimal.valueOf(15));
            Payment pendingPayment = new Payment();
            pendingPayment.setStatus(PaymentStatus.PENDING);
            unpaidDelivery.setPayments(new ArrayList<>(List.of(pendingPayment)));

            when(siteConfigurationRepository.findActiveConfiguration()).thenReturn(Optional.of(config));
            when(deliveryRepository.findByCourierIdAndStatus(courierId, Delivery.DeliveryStatus.COMPLETED))
                    .thenReturn(List.of(paidDelivery, unpaidDelivery));
            stubSplitCalculator(config, BigDecimal.valueOf(20), false);

            CourierEarningsResponse result = courierEarningsService.getCourierEarnings(courierId, null);

            assertThat(result.getTotalDeliveries()).isEqualTo(1);
        }

        @Test
        @DisplayName("Filtra deliveries sem payments")
        void filtraDeliveriesSemPayments() {
            SiteConfiguration config = makeConfig();

            Delivery noPayments = new Delivery();
            noPayments.setId(3L);
            noPayments.setStatus(Delivery.DeliveryStatus.COMPLETED);
            noPayments.setPayments(null);

            when(siteConfigurationRepository.findActiveConfiguration()).thenReturn(Optional.of(config));
            when(deliveryRepository.findByCourierIdAndStatus(courierId, Delivery.DeliveryStatus.COMPLETED))
                    .thenReturn(List.of(noPayments));

            CourierEarningsResponse result = courierEarningsService.getCourierEarnings(courierId, null);

            assertThat(result.getTotalDeliveries()).isEqualTo(0);
        }

        @Test
        @DisplayName("Filtra por recent=true usando deliveryHistoryDays da config")
        void filtraPorRecent() {
            SiteConfiguration config = makeConfig();
            config.setDeliveryHistoryDays(3);

            // Delivery recente (dentro do filtro)
            Delivery recentDelivery = makePaidDelivery(1L, BigDecimal.valueOf(20),
                    OffsetDateTime.now(ZoneId.of("America/Fortaleza")).minusDays(1));

            // Delivery antiga (fora do filtro)
            Delivery oldDelivery = makePaidDelivery(2L, BigDecimal.valueOf(30),
                    OffsetDateTime.now(ZoneId.of("America/Fortaleza")).minusDays(10));

            when(siteConfigurationRepository.findActiveConfiguration()).thenReturn(Optional.of(config));
            when(deliveryRepository.findByCourierIdAndStatus(courierId, Delivery.DeliveryStatus.COMPLETED))
                    .thenReturn(List.of(recentDelivery, oldDelivery));
            stubSplitCalculator(config, BigDecimal.valueOf(20), false);

            CourierEarningsResponse result = courierEarningsService.getCourierEarnings(courierId, true);

            assertThat(result.getTotalDeliveries()).isEqualTo(1);
            assertThat(result.getDeliveries().get(0).getDeliveryId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("recent=false nao filtra por data")
        void recentFalseNaoFiltra() {
            SiteConfiguration config = makeConfig();
            config.setDeliveryHistoryDays(3);

            Delivery recentDelivery = makePaidDelivery(1L, BigDecimal.valueOf(20), OffsetDateTime.now().minusDays(1));
            Delivery oldDelivery = makePaidDelivery(2L, BigDecimal.valueOf(30), OffsetDateTime.now().minusDays(10));

            when(siteConfigurationRepository.findActiveConfiguration()).thenReturn(Optional.of(config));
            when(deliveryRepository.findByCourierIdAndStatus(courierId, Delivery.DeliveryStatus.COMPLETED))
                    .thenReturn(List.of(recentDelivery, oldDelivery));
            stubSplitCalculator(config, BigDecimal.valueOf(20), false);
            stubSplitCalculator(config, BigDecimal.valueOf(30), false);

            CourierEarningsResponse result = courierEarningsService.getCourierEarnings(courierId, false);

            assertThat(result.getTotalDeliveries()).isEqualTo(2);
        }

        @Test
        @DisplayName("Soma total de ganhos de multiplas deliveries")
        void somaTotalDeGanhos() {
            SiteConfiguration config = makeConfig();
            Delivery d1 = makePaidDelivery(1L, BigDecimal.valueOf(20), OffsetDateTime.now().minusHours(2));
            Delivery d2 = makePaidDelivery(2L, BigDecimal.valueOf(30), OffsetDateTime.now().minusHours(1));

            when(siteConfigurationRepository.findActiveConfiguration()).thenReturn(Optional.of(config));
            when(deliveryRepository.findByCourierIdAndStatus(courierId, Delivery.DeliveryStatus.COMPLETED))
                    .thenReturn(List.of(d1, d2));
            stubSplitCalculator(config, BigDecimal.valueOf(20), false);
            stubSplitCalculator(config, BigDecimal.valueOf(30), false);

            CourierEarningsResponse result = courierEarningsService.getCourierEarnings(courierId, null);

            assertThat(result.getTotalDeliveries()).isEqualTo(2);
            assertThat(result.getTotalEarnings()).isGreaterThan(BigDecimal.ZERO);
            // totalEarnings = sum of courierAmounts
            BigDecimal expectedTotal = result.getDeliveries().stream()
                    .map(CourierEarningsResponse.DeliveryEarningDetail::getCourierAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(result.getTotalEarnings()).isEqualByComparingTo(expectedTotal);
        }

        @Test
        @DisplayName("Deliveries ordenadas por data mais recente primeiro")
        void ordenadasPorDataRecente() {
            SiteConfiguration config = makeConfig();
            Delivery older = makePaidDelivery(1L, BigDecimal.valueOf(20), OffsetDateTime.now().minusDays(3));
            Delivery newer = makePaidDelivery(2L, BigDecimal.valueOf(30), OffsetDateTime.now().minusDays(1));

            when(siteConfigurationRepository.findActiveConfiguration()).thenReturn(Optional.of(config));
            when(deliveryRepository.findByCourierIdAndStatus(courierId, Delivery.DeliveryStatus.COMPLETED))
                    .thenReturn(List.of(older, newer));
            stubSplitCalculator(config, BigDecimal.valueOf(20), false);
            stubSplitCalculator(config, BigDecimal.valueOf(30), false);

            CourierEarningsResponse result = courierEarningsService.getCourierEarnings(courierId, null);

            // Mais recente primeiro
            assertThat(result.getDeliveries().get(0).getDeliveryId()).isEqualTo(2L);
            assertThat(result.getDeliveries().get(1).getDeliveryId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Lanca excecao quando SiteConfiguration nao encontrada")
        void lancaExcecaoSemConfig() {
            when(siteConfigurationRepository.findActiveConfiguration()).thenReturn(Optional.empty());

            assertThatThrownBy(() -> courierEarningsService.getCourierEarnings(courierId, null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("não encontrada");
        }
    }
}
