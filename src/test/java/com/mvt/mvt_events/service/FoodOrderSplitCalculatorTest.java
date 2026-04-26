package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.SiteConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cobre a lógica de cálculo de splits e transfers do FoodOrder (Zapi-Food).
 *
 * <p>Regras de negócio:
 * <ul>
 *   <li>Estabelecimento: 87% × comida (via Pagar.me split direto no checkout)</li>
 *   <li>Courier: 87% × frete (via pagarme_transfer pós-accept)</li>
 *   <li>Organizer: 5% × (comida + frete) (via pagarme_transfer pós-accept)</li>
 *   <li>Plataforma: o resto</li>
 * </ul>
 */
class FoodOrderSplitCalculatorTest {

    private final FoodOrderSplitCalculator calc = new FoodOrderSplitCalculator();

    @Nested
    @DisplayName("calculateCourierTransferAmount — 87% do frete")
    class CourierTransferTests {

        @Test
        @DisplayName("R$5,50 frete → R$4,78 (478 cents) — caso do pedido #10")
        void deliveryFee5_50() {
            BigDecimal deliveryCents = new BigDecimal("550");
            BigDecimal amount = calc.calculateCourierTransferAmount(deliveryCents);
            assertThat(amount).isEqualByComparingTo("478");
        }

        @Test
        @DisplayName("R$10 frete → R$8,70 (870 cents)")
        void deliveryFee10() {
            assertThat(calc.calculateCourierTransferAmount(new BigDecimal("1000")))
                    .isEqualByComparingTo("870");
        }

        @Test
        @DisplayName("Arredondamento pra baixo (87% de 7 cents = 6.09 → 6)")
        void roundDown() {
            assertThat(calc.calculateCourierTransferAmount(new BigDecimal("7")))
                    .isEqualByComparingTo("6");
        }
    }

    @Nested
    @DisplayName("calculateOrganizerTransferAmount — 5% do total")
    class OrganizerTransferTests {

        @Test
        @DisplayName("R$23,50 total → R$1,17 (117 cents)")
        void total23_50() {
            // 5% de 2350 = 117.5 → arredonda para baixo → 117
            BigDecimal totalCents = new BigDecimal("2350");
            BigDecimal amount = calc.calculateOrganizerTransferAmount(totalCents);
            assertThat(amount).isEqualByComparingTo("117");
        }

        @Test
        @DisplayName("R$100 total → R$5,00 (500 cents)")
        void total100() {
            assertThat(calc.calculateOrganizerTransferAmount(new BigDecimal("10000")))
                    .isEqualByComparingTo("500");
        }

        @Test
        @DisplayName("Arredondamento pra baixo (5% de 19 cents = 0.95 → 0)")
        void roundDown() {
            assertThat(calc.calculateOrganizerTransferAmount(new BigDecimal("19")))
                    .isEqualByComparingTo("0");
        }
    }

    @Nested
    @DisplayName("INVARIANTE: soma das partes em cents = total pago em cents")
    class SumIntegrityTests {

        /**
         * Para cada cenário, verifica a regra fundamental:
         * client + courier + organizer + plataforma == total (em cents, sem perda).
         */
        @ParameterizedTest(name = "comida={0}¢ frete={1}¢ total={2}¢")
        @CsvSource({
                // comida, frete, total esperado
                "1800,  550,  2350",   // pedido #10 real
                "1000,  500,  1500",   // valores redondos
                "1234,  567,  1801",   // valores quebrados
                "100,   100,   200",   // mínimos
                "9999,  999,  10998",  // grandes
                "1,     1,      2",    // 1 cent cada
                "10000, 0,    10000",  // sem frete
                "0,     1000,  1000",  // hipotético: só frete (não realista, mas borda)
                "777,   333,   1110",  // primos pra forçar arredondamento
                "1500,  299,   1799",  // outro caso
        })
        @DisplayName("client + courier + organizer + plataforma = total")
        void reconciliationInvariant(int food, int delivery, int total) {
            BigDecimal foodCents = BigDecimal.valueOf(food);
            BigDecimal deliveryCents = BigDecimal.valueOf(delivery);
            BigDecimal totalCents = BigDecimal.valueOf(total);
            assertThat(foodCents.add(deliveryCents)).isEqualByComparingTo(totalCents);

            BigDecimal clientAmount = foodCents
                    .multiply(new BigDecimal("87"))
                    .divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.DOWN);
            BigDecimal courierAmount = calc.calculateCourierTransferAmount(deliveryCents);
            BigDecimal organizerAmount = calc.calculateOrganizerTransferAmount(totalCents);
            BigDecimal platformAmount = totalCents
                    .subtract(clientAmount)
                    .subtract(courierAmount)
                    .subtract(organizerAmount);

            // Plataforma sempre absorve o arredondamento — nunca pode ficar negativa
            // (significaria que pagamos a mais do que recebemos).
            assertThat(platformAmount.signum())
                    .as("plataforma não pode ficar negativa — comida=%d frete=%d", food, delivery)
                    .isGreaterThanOrEqualTo(0);

            // INVARIANTE: soma das 4 partes == total pago.
            BigDecimal sum = clientAmount
                    .add(courierAmount)
                    .add(organizerAmount)
                    .add(platformAmount);
            assertThat(sum)
                    .as("soma das partes deve bater com o total pago em cents")
                    .isEqualByComparingTo(totalCents);
        }

        @Test
        @DisplayName("Pedido #10 detalhado: 1566 + 478 + 117 + 189 = 2350 (R$23,50)")
        void order10Detailed() {
            BigDecimal foodCents = new BigDecimal("1800");
            BigDecimal deliveryCents = new BigDecimal("550");
            BigDecimal totalCents = foodCents.add(deliveryCents);

            BigDecimal clientAmount = foodCents
                    .multiply(new BigDecimal("87"))
                    .divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.DOWN);
            assertThat(clientAmount).isEqualByComparingTo("1566");

            BigDecimal courierAmount = calc.calculateCourierTransferAmount(deliveryCents);
            assertThat(courierAmount).isEqualByComparingTo("478");

            BigDecimal organizerAmount = calc.calculateOrganizerTransferAmount(totalCents);
            assertThat(organizerAmount).isEqualByComparingTo("117");

            BigDecimal platformAmount = totalCents
                    .subtract(clientAmount).subtract(courierAmount).subtract(organizerAmount);
            assertThat(platformAmount).isEqualByComparingTo("189");

            assertThat(clientAmount.add(courierAmount).add(organizerAmount).add(platformAmount))
                    .isEqualByComparingTo(totalCents);
        }
    }

    @Nested
    @DisplayName("calculateCheckoutSplit — split do PIX no checkout (2-way)")
    class CheckoutSplitTests {

        private SiteConfiguration siteConfig() {
            // Config de produção atual: organizer 5%
            SiteConfiguration c = new SiteConfiguration();
            c.setOrganizerPercentage(new BigDecimal("5"));
            return c;
        }

        @ParameterizedTest(name = "comida={0}¢ frete={1}¢")
        @CsvSource({
                "1800,  550",
                "1000,  500",
                "1234,  567",
                "9999,  999",
                "100,   100",
                "10000, 0",
        })
        @DisplayName("INVARIANTE: client + organizer + platform = total (sem organizer no checkout)")
        void checkoutSplitWithoutOrganizer(int food, int delivery) {
            BigDecimal foodCents = BigDecimal.valueOf(food);
            BigDecimal deliveryCents = BigDecimal.valueOf(delivery);
            BigDecimal totalCents = foodCents.add(deliveryCents);

            FoodOrderSplitCalculator.CheckoutSplit split = calc.calculateCheckoutSplit(
                    foodCents, deliveryCents, siteConfig(), false);

            assertThat(split.getOrganizerAmountCents()).isEqualByComparingTo("0");
            assertThat(split.getClientAmountCents()
                    .add(split.getOrganizerAmountCents())
                    .add(split.getPlatformAmountCents()))
                    .as("soma do split = total pago")
                    .isEqualByComparingTo(totalCents);
        }
    }
}
