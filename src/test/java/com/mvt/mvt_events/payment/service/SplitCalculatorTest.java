package com.mvt.mvt_events.payment.service;

import com.mvt.mvt_events.jpa.Delivery;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.payment.dto.PagarMeSplitRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SplitCalculator")
class SplitCalculatorTest {

    @InjectMocks
    private SplitCalculator calculator;

    private static final String COURIER_RECIPIENT_ID = "re_courier_123";
    private static final String ORGANIZER_RECIPIENT_ID = "re_organizer_456";

    @BeforeEach
    void setUp() {
        // Simula os @Value do application.properties
        ReflectionTestUtils.setField(calculator, "courierPercentage", 8700);
        ReflectionTestUtils.setField(calculator, "organizerPercentage", 500);
        ReflectionTestUtils.setField(calculator, "courierLiable", false);
        ReflectionTestUtils.setField(calculator, "courierChargeFee", false);
        ReflectionTestUtils.setField(calculator, "organizerChargeFee", false);
    }

    // ============================================
    // HELPERS
    // ============================================

    private User createCourier(String recipientId) {
        User courier = new User();
        courier.setName("Motoboy Teste");
        courier.setRole(User.Role.COURIER);
        courier.setPagarmeRecipientId(recipientId);
        return courier;
    }

    private User createOrganizer(String recipientId) {
        User organizer = new User();
        organizer.setName("Gerente Teste");
        organizer.setRole(User.Role.ORGANIZER);
        organizer.setPagarmeRecipientId(recipientId);
        return organizer;
    }

    private Delivery createDelivery(User courier, User organizer) {
        Delivery delivery = new Delivery();
        delivery.setCourier(courier);
        delivery.setOrganizer(organizer);
        return delivery;
    }

    // ============================================
    // calculatePagarmeSplit
    // ============================================

    @Nested
    @DisplayName("calculatePagarmeSplit")
    class CalculatePagarmeSplit {

        @Test
        @DisplayName("com organizer: retorna 2 splits (courier 87% + organizer 5%)")
        void withOrganizer_returnsTwoSplits() {
            Delivery delivery = createDelivery(
                createCourier(COURIER_RECIPIENT_ID),
                createOrganizer(ORGANIZER_RECIPIENT_ID)
            );

            List<PagarMeSplitRequest> splits = calculator.calculatePagarmeSplit(delivery);

            assertThat(splits).hasSize(2);

            // Courier split
            PagarMeSplitRequest courierSplit = splits.get(0);
            assertThat(courierSplit.getAmount()).isEqualTo(8700);
            assertThat(courierSplit.getRecipientId()).isEqualTo(COURIER_RECIPIENT_ID);
            assertThat(courierSplit.getType()).isEqualTo("percentage");
            assertThat(courierSplit.getOptions().getLiable()).isFalse();
            assertThat(courierSplit.getOptions().getChargeProcessingFee()).isFalse();

            // Organizer split
            PagarMeSplitRequest organizerSplit = splits.get(1);
            assertThat(organizerSplit.getAmount()).isEqualTo(500);
            assertThat(organizerSplit.getRecipientId()).isEqualTo(ORGANIZER_RECIPIENT_ID);
            assertThat(organizerSplit.getType()).isEqualTo("percentage");
            assertThat(organizerSplit.getOptions().getLiable()).isFalse();
            assertThat(organizerSplit.getOptions().getChargeProcessingFee()).isFalse();
        }

        @Test
        @DisplayName("sem organizer: retorna 1 split (courier 87%, plataforma fica com 13%)")
        void withoutOrganizer_returnsOneSplit() {
            Delivery delivery = createDelivery(
                createCourier(COURIER_RECIPIENT_ID),
                null
            );

            List<PagarMeSplitRequest> splits = calculator.calculatePagarmeSplit(delivery);

            assertThat(splits).hasSize(1);
            assertThat(splits.get(0).getAmount()).isEqualTo(8700);
            assertThat(splits.get(0).getRecipientId()).isEqualTo(COURIER_RECIPIENT_ID);
        }

        @Test
        @DisplayName("organizer sem recipientId: trata como sem organizer")
        void organizerWithoutRecipientId_treatedAsNoOrganizer() {
            Delivery delivery = createDelivery(
                createCourier(COURIER_RECIPIENT_ID),
                createOrganizer(null) // sem recipientId
            );

            List<PagarMeSplitRequest> splits = calculator.calculatePagarmeSplit(delivery);

            assertThat(splits).hasSize(1); // Apenas courier
        }

        @Test
        @DisplayName("organizer com recipientId vazio: trata como sem organizer")
        void organizerWithEmptyRecipientId_treatedAsNoOrganizer() {
            Delivery delivery = createDelivery(
                createCourier(COURIER_RECIPIENT_ID),
                createOrganizer("") // vazio
            );

            List<PagarMeSplitRequest> splits = calculator.calculatePagarmeSplit(delivery);

            assertThat(splits).hasSize(1);
        }

        @Test
        @DisplayName("delivery null: lanca IllegalArgumentException")
        void nullDelivery_throwsException() {
            assertThatThrownBy(() -> calculator.calculatePagarmeSplit(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null");
        }

        @Test
        @DisplayName("courier sem recipientId: lanca IllegalStateException")
        void courierWithoutRecipientId_throwsException() {
            Delivery delivery = createDelivery(
                createCourier(null),
                createOrganizer(ORGANIZER_RECIPIENT_ID)
            );

            assertThatThrownBy(() -> calculator.calculatePagarmeSplit(delivery))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("recipient ID");
        }

        @Test
        @DisplayName("sem courier: lanca IllegalStateException")
        void noCourier_throwsException() {
            Delivery delivery = createDelivery(null, createOrganizer(ORGANIZER_RECIPIENT_ID));

            assertThatThrownBy(() -> calculator.calculatePagarmeSplit(delivery))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Courier is missing");
        }
    }

    // ============================================
    // calculatePagarmeSplitWithoutOrganizer
    // ============================================

    @Nested
    @DisplayName("calculatePagarmeSplitWithoutOrganizer")
    class CalculatePagarmeSplitWithoutOrganizer {

        @Test
        @DisplayName("retorna apenas split do courier (87%)")
        void returnsOnlyCourierSplit() {
            Delivery delivery = createDelivery(createCourier(COURIER_RECIPIENT_ID), null);

            List<PagarMeSplitRequest> splits = calculator.calculatePagarmeSplitWithoutOrganizer(delivery);

            assertThat(splits).hasSize(1);
            assertThat(splits.get(0).getAmount()).isEqualTo(8700);
            assertThat(splits.get(0).getRecipientId()).isEqualTo(COURIER_RECIPIENT_ID);
        }

        @Test
        @DisplayName("delivery null: lanca exception")
        void nullDelivery_throwsException() {
            assertThatThrownBy(() -> calculator.calculatePagarmeSplitWithoutOrganizer(null))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ============================================
    // isReadyForSplit
    // ============================================

    @Nested
    @DisplayName("isReadyForSplit")
    class IsReadyForSplit {

        @Test
        @DisplayName("courier com recipientId: retorna true")
        void courierWithRecipientId_returnsTrue() {
            Delivery delivery = createDelivery(createCourier(COURIER_RECIPIENT_ID), null);
            assertThat(calculator.isReadyForSplit(delivery)).isTrue();
        }

        @Test
        @DisplayName("courier sem recipientId: retorna false")
        void courierWithoutRecipientId_returnsFalse() {
            Delivery delivery = createDelivery(createCourier(null), null);
            assertThat(calculator.isReadyForSplit(delivery)).isFalse();
        }

        @Test
        @DisplayName("sem courier: retorna false")
        void noCourier_returnsFalse() {
            Delivery delivery = createDelivery(null, null);
            assertThat(calculator.isReadyForSplit(delivery)).isFalse();
        }

        @Test
        @DisplayName("delivery null: retorna false")
        void nullDelivery_returnsFalse() {
            assertThat(calculator.isReadyForSplit(null)).isFalse();
        }

        @Test
        @DisplayName("organizer nao e obrigatorio para split")
        void organizerNotRequired() {
            Delivery delivery = createDelivery(createCourier(COURIER_RECIPIENT_ID), null);
            assertThat(calculator.isReadyForSplit(delivery)).isTrue();
        }
    }

    // ============================================
    // hasValidOrganizer
    // ============================================

    @Nested
    @DisplayName("hasValidOrganizer")
    class HasValidOrganizer {

        @Test
        @DisplayName("organizer com recipientId: retorna true")
        void organizerWithRecipientId_returnsTrue() {
            Delivery delivery = createDelivery(
                createCourier(COURIER_RECIPIENT_ID),
                createOrganizer(ORGANIZER_RECIPIENT_ID)
            );
            assertThat(calculator.hasValidOrganizer(delivery)).isTrue();
        }

        @Test
        @DisplayName("organizer sem recipientId: retorna false")
        void organizerWithoutRecipientId_returnsFalse() {
            Delivery delivery = createDelivery(
                createCourier(COURIER_RECIPIENT_ID),
                createOrganizer(null)
            );
            assertThat(calculator.hasValidOrganizer(delivery)).isFalse();
        }

        @Test
        @DisplayName("sem organizer: retorna false")
        void noOrganizer_returnsFalse() {
            Delivery delivery = createDelivery(createCourier(COURIER_RECIPIENT_ID), null);
            assertThat(calculator.hasValidOrganizer(delivery)).isFalse();
        }
    }

    // ============================================
    // calculateSplitBreakdown
    // ============================================

    @Nested
    @DisplayName("calculateSplitBreakdown")
    class CalculateSplitBreakdown {

        @Test
        @DisplayName("R$100 com organizer: 87 + 5 + 8 = 100")
        void hundredReaisWithOrganizer() {
            Delivery delivery = createDelivery(
                createCourier(COURIER_RECIPIENT_ID),
                createOrganizer(ORGANIZER_RECIPIENT_ID)
            );

            SplitCalculator.SplitBreakdown breakdown = calculator.calculateSplitBreakdown(10000L, delivery);

            assertThat(breakdown.getCourierAmount()).isEqualTo(8700);   // R$ 87,00
            assertThat(breakdown.getOrganizerAmount()).isEqualTo(500);  // R$ 5,00
            assertThat(breakdown.getPlatformAmount()).isEqualTo(800);   // R$ 8,00
            assertThat(breakdown.getHasOrganizer()).isTrue();

            // Soma = total
            assertThat(breakdown.getCourierAmount() + breakdown.getOrganizerAmount() + breakdown.getPlatformAmount())
                .isEqualTo(10000L);
        }

        @Test
        @DisplayName("R$100 sem organizer: 87 + 0 + 13 = 100")
        void hundredReaisWithoutOrganizer() {
            Delivery delivery = createDelivery(createCourier(COURIER_RECIPIENT_ID), null);

            SplitCalculator.SplitBreakdown breakdown = calculator.calculateSplitBreakdown(10000L, delivery);

            assertThat(breakdown.getCourierAmount()).isEqualTo(8700);
            assertThat(breakdown.getOrganizerAmount()).isEqualTo(0);
            assertThat(breakdown.getPlatformAmount()).isEqualTo(1300); // 13%
            assertThat(breakdown.getHasOrganizer()).isFalse();

            assertThat(breakdown.getCourierAmount() + breakdown.getOrganizerAmount() + breakdown.getPlatformAmount())
                .isEqualTo(10000L);
        }

        @Test
        @DisplayName("R$15 com organizer: verifica arredondamento")
        void fifteenReaisWithOrganizer_rounding() {
            Delivery delivery = createDelivery(
                createCourier(COURIER_RECIPIENT_ID),
                createOrganizer(ORGANIZER_RECIPIENT_ID)
            );

            SplitCalculator.SplitBreakdown breakdown = calculator.calculateSplitBreakdown(1500L, delivery);

            // 1500 * 8700 / 10000 = 1305
            assertThat(breakdown.getCourierAmount()).isEqualTo(1305);
            // 1500 * 500 / 10000 = 75
            assertThat(breakdown.getOrganizerAmount()).isEqualTo(75);
            // Resto: 1500 - 1305 - 75 = 120
            assertThat(breakdown.getPlatformAmount()).isEqualTo(120);

            // Soma = total
            assertThat(breakdown.getCourierAmount() + breakdown.getOrganizerAmount() + breakdown.getPlatformAmount())
                .isEqualTo(1500L);
        }

        @Test
        @DisplayName("percentuais corretos no breakdown")
        void percentagesAreCorrect() {
            Delivery delivery = createDelivery(
                createCourier(COURIER_RECIPIENT_ID),
                createOrganizer(ORGANIZER_RECIPIENT_ID)
            );

            SplitCalculator.SplitBreakdown breakdown = calculator.calculateSplitBreakdown(10000L, delivery);

            assertThat(breakdown.getCourierPercentage()).isEqualByComparingTo(new BigDecimal("87"));
            assertThat(breakdown.getOrganizerPercentage()).isEqualByComparingTo(new BigDecimal("5"));
            assertThat(breakdown.getPlatformPercentage()).isEqualByComparingTo(new BigDecimal("8"));
        }

        @Test
        @DisplayName("percentuais sem organizer: plataforma fica com 13%")
        void percentagesWithoutOrganizer() {
            Delivery delivery = createDelivery(createCourier(COURIER_RECIPIENT_ID), null);

            SplitCalculator.SplitBreakdown breakdown = calculator.calculateSplitBreakdown(10000L, delivery);

            assertThat(breakdown.getCourierPercentage()).isEqualByComparingTo(new BigDecimal("87"));
            assertThat(breakdown.getOrganizerPercentage()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(breakdown.getPlatformPercentage()).isEqualByComparingTo(new BigDecimal("13"));
        }

        @Test
        @DisplayName("valor zero ou negativo: lanca exception")
        void invalidAmount_throwsException() {
            Delivery delivery = createDelivery(createCourier(COURIER_RECIPIENT_ID), null);

            assertThatThrownBy(() -> calculator.calculateSplitBreakdown(0L, delivery))
                .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> calculator.calculateSplitBreakdown(-100L, delivery))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("valor null: lanca exception")
        void nullAmount_throwsException() {
            Delivery delivery = createDelivery(createCourier(COURIER_RECIPIENT_ID), null);

            assertThatThrownBy(() -> calculator.calculateSplitBreakdown(null, delivery))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ============================================
    // Consistencia de Enums
    // ============================================

    @Nested
    @DisplayName("Consistencia de enums")
    class EnumConsistency {

        @Test
        @DisplayName("User.Role contem ORGANIZER (nao MANAGER)")
        void roleEnumHasOrganizer() {
            assertThat(User.Role.valueOf("ORGANIZER")).isNotNull();
            assertThatThrownBy(() -> User.Role.valueOf("MANAGER"))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("User.Role contem todos os roles esperados")
        void roleEnumHasAllExpectedValues() {
            assertThat(User.Role.values())
                .extracting(Enum::name)
                .contains("CUSTOMER", "CLIENT", "COURIER", "ORGANIZER", "ADMIN");
        }
    }
}
