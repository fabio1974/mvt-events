package com.mvt.mvt_events.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mvt.mvt_events.dto.PaymentRequest;
import com.mvt.mvt_events.dto.PaymentResponse;
import com.mvt.mvt_events.jpa.*;
import com.mvt.mvt_events.jpa.CustomerCard.CardBrand;
import com.mvt.mvt_events.jpa.CustomerPaymentPreference.PreferredPaymentType;
import com.mvt.mvt_events.payment.dto.OrderRequest;
import com.mvt.mvt_events.payment.dto.OrderResponse;
import com.mvt.mvt_events.payment.service.PagarMeService;
import com.mvt.mvt_events.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitarios do PaymentService — cobre criacao de pagamentos PIX e cartao,
 * transicoes de status, webhooks (confirmacao, expiracao), validacoes de deliveries,
 * pagamentos duplicados, pagamento automatico e mensagens de falha.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PaymentService")
class PaymentServiceTest {

    @Mock private PagarMeService pagarMeService;
    @Mock private PaymentRepository paymentRepository;
    @Mock private DeliveryRepository deliveryRepository;
    @Mock private UserRepository userRepository;
    @Mock private SiteConfigurationRepository siteConfigurationRepository;
    @Mock private ObjectMapper objectMapper;
    @Mock private PaymentSplitCalculator splitCalculator;
    @Mock private CustomerPaymentPreferenceService preferenceService;
    @Mock private CustomerCardRepository cardRepository;
    @Mock private PushNotificationService pushNotificationService;
    @Mock private DeliveryNotificationService deliveryNotificationService;

    @InjectMocks
    private PaymentService paymentService;

    // UUIDs fixos
    private final UUID clientId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID courierId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private final UUID organizerId = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private final UUID customerId = UUID.fromString("00000000-0000-0000-0000-000000000004");

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

    private User makeCourier() {
        User courier = makeUser(courierId, "Pedro Moto", User.Role.COURIER);
        courier.setPagarmeRecipientId("re_courier_abc123");
        return courier;
    }

    private User makeOrganizer() {
        User organizer = makeUser(organizerId, "Ana Gerente", User.Role.ORGANIZER);
        organizer.setPagarmeRecipientId("re_organizer_def456");
        return organizer;
    }

    private Delivery makeDelivery(Long id, User client) {
        Delivery d = new Delivery();
        d.setId(id);
        d.setClient(client);
        d.setStatus(Delivery.DeliveryStatus.COMPLETED);
        d.setFromAddress("Rua A, 100");
        d.setToAddress("Rua B, 200");
        d.setDistanceKm(BigDecimal.valueOf(5.0));
        d.setShippingFee(BigDecimal.valueOf(15.00));
        d.setCourier(makeCourier());
        d.setCreatedAt(OffsetDateTime.now(ZoneId.of("America/Fortaleza")));
        return d;
    }

    private PaymentRequest makePaymentRequest(List<Long> deliveryIds, BigDecimal amount) {
        PaymentRequest req = new PaymentRequest();
        req.setDeliveryIds(deliveryIds);
        req.setAmount(amount);
        req.setClientEmail("cliente@zapi10.com");
        req.setMotoboyAccountId("re_courier_abc123");
        return req;
    }

    private OrderResponse makeOrderResponse(String id, String status) {
        return OrderResponse.builder()
                .id(id)
                .status(status)
                .build();
    }

    private OrderResponse makeOrderResponseWithPixQrCode(String id) {
        OrderResponse.LastTransaction tx = OrderResponse.LastTransaction.builder()
                .qrCode("00020126360014BR.GOV.BCB.PIX...")
                .qrCodeUrl("https://api.pagar.me/qr/123.png")
                .expiresAt("2026-04-10T23:59:59Z")
                .build();

        OrderResponse.Charge charge = OrderResponse.Charge.builder()
                .id("ch_test_123")
                .status("pending")
                .lastTransaction(tx)
                .build();

        return OrderResponse.builder()
                .id(id)
                .status("pending")
                .charges(List.of(charge))
                .build();
    }

    private Payment makePayment(Long id, PaymentStatus status, User payer) {
        Payment p = new Payment();
        p.setId(id);
        p.setStatus(status);
        p.setAmount(BigDecimal.valueOf(15.00));
        p.setCurrency(com.mvt.mvt_events.jpa.Currency.BRL);
        p.setPaymentMethod(PaymentMethod.PIX);
        p.setProvider(PaymentProvider.PAGARME);
        p.setProviderPaymentId("or_test_" + id);
        p.setPayer(payer);
        return p;
    }

    private SiteConfiguration defaultSiteConfig() {
        SiteConfiguration c = new SiteConfiguration();
        c.setPricePerKm(BigDecimal.valueOf(2.50));
        c.setMinimumShippingFee(BigDecimal.valueOf(5.00));
        c.setOrganizerPercentage(BigDecimal.valueOf(5));
        c.setPagarmeRecipientId("re_platform_ghi789");
        return c;
    }

    private CustomerCard makeCard() {
        CustomerCard card = new CustomerCard();
        card.setId(1L);
        card.setPagarmeCardId("card_test_123");
        card.setLastFourDigits("4242");
        card.setBrand(CardBrand.VISA);
        card.setIsActive(true);
        card.setIsVerified(true);
        return card;
    }

    private CustomerPaymentPreference makePixPreference() {
        return CustomerPaymentPreference.builder()
                .id(1L)
                .preferredPaymentType(PreferredPaymentType.PIX)
                .build();
    }

    private CustomerPaymentPreference makeCreditCardPreference(CustomerCard card) {
        return CustomerPaymentPreference.builder()
                .id(1L)
                .preferredPaymentType(PreferredPaymentType.CREDIT_CARD)
                .defaultCard(card)
                .build();
    }

    // ================================================================
    // createPaymentWithSplit — PIX
    // ================================================================

    @Nested
    @DisplayName("createPaymentWithSplit() - Pagamento PIX com split")
    class CreatePaymentWithSplitTests {

        @Test
        @DisplayName("Cria pagamento PIX com sucesso para 1 delivery")
        void criaPixComSucesso() throws Exception {
            User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            Delivery delivery = makeDelivery(1L, client);
            PaymentRequest request = makePaymentRequest(List.of(1L), BigDecimal.valueOf(15.00));

            when(deliveryRepository.findAllById(List.of(1L))).thenReturn(List.of(delivery));
            when(paymentRepository.findPendingOrCompletedPaymentsForDeliveries(anyList()))
                    .thenReturn(Collections.emptyList());
            when(siteConfigurationRepository.findActiveConfiguration())
                    .thenReturn(Optional.of(defaultSiteConfig()));
            when(splitCalculator.toCents(any(BigDecimal.class)))
                    .thenReturn(BigDecimal.valueOf(1500));
            when(splitCalculator.calculateCourierAmount(any(), any()))
                    .thenReturn(BigDecimal.valueOf(1305));
            when(splitCalculator.calculateOrganizerAmount(any(), any()))
                    .thenReturn(BigDecimal.valueOf(75));
            when(splitCalculator.calculatePlatformAmount(any(), any(), any()))
                    .thenReturn(BigDecimal.valueOf(120));
            when(pagarMeService.createOrderWithFullResponse(any(OrderRequest.class)))
                    .thenReturn(makeOrderResponseWithPixQrCode("or_test_001"));
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
                Payment p = inv.getArgument(0);
                p.setId(100L);
                return p;
            });

            PaymentResponse result = paymentService.createPaymentWithSplit(request);

            assertThat(result).isNotNull();
            assertThat(result.getPixQrCode()).isNotNull();
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.PENDING);
            verify(pagarMeService).createOrderWithFullResponse(any(OrderRequest.class));
            verify(paymentRepository).save(any(Payment.class));
        }

        @Test
        @DisplayName("Cria pagamento PIX para multiplas deliveries")
        void criaPixMultiplasDeliveries() throws Exception {
            User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            Delivery d1 = makeDelivery(1L, client);
            Delivery d2 = makeDelivery(2L, client);
            d2.setShippingFee(BigDecimal.valueOf(20.00));

            PaymentRequest request = makePaymentRequest(List.of(1L, 2L), BigDecimal.valueOf(35.00));

            when(deliveryRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(d1, d2));
            when(paymentRepository.findPendingOrCompletedPaymentsForDeliveries(anyList()))
                    .thenReturn(Collections.emptyList());
            when(siteConfigurationRepository.findActiveConfiguration())
                    .thenReturn(Optional.of(defaultSiteConfig()));
            when(splitCalculator.toCents(any(BigDecimal.class))).thenReturn(BigDecimal.valueOf(3500));
            when(splitCalculator.calculateCourierAmount(any(), any())).thenReturn(BigDecimal.valueOf(3045));
            when(splitCalculator.calculatePlatformAmount(any(), any(), any())).thenReturn(BigDecimal.valueOf(455));
            when(pagarMeService.createOrderWithFullResponse(any(OrderRequest.class)))
                    .thenReturn(makeOrderResponseWithPixQrCode("or_test_002"));
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
                Payment p = inv.getArgument(0);
                p.setId(101L);
                return p;
            });

            PaymentResponse result = paymentService.createPaymentWithSplit(request);

            assertThat(result).isNotNull();

            // Verificar que o Payment salvo tem 2 deliveries associadas
            ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(paymentCaptor.capture());
            Payment saved = paymentCaptor.getValue();
            assertThat(saved.getDeliveries()).hasSize(2);
        }

        @Test
        @DisplayName("Lanca excecao quando nenhuma delivery encontrada")
        void nenhumaDeliveryEncontrada() {
            PaymentRequest request = makePaymentRequest(List.of(999L), BigDecimal.valueOf(15.00));

            when(deliveryRepository.findAllById(List.of(999L))).thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> paymentService.createPaymentWithSplit(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Nenhuma delivery encontrada");
        }

        @Test
        @DisplayName("Lanca excecao quando algumas deliveries nao encontradas")
        void algumasDeliveriesNaoEncontradas() {
            User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            Delivery d1 = makeDelivery(1L, client);

            PaymentRequest request = makePaymentRequest(List.of(1L, 999L), BigDecimal.valueOf(30.00));

            when(deliveryRepository.findAllById(List.of(1L, 999L))).thenReturn(List.of(d1));

            assertThatThrownBy(() -> paymentService.createPaymentWithSplit(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Algumas deliveries não foram encontradas");
        }

        @Test
        @DisplayName("Salva Payment FAILED quando Pagar.me lanca excecao")
        void salvaPaymentFalhadoQuandoErroNoPagarme() throws Exception {
            User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            Delivery delivery = makeDelivery(1L, client);
            PaymentRequest request = makePaymentRequest(List.of(1L), BigDecimal.valueOf(15.00));

            when(deliveryRepository.findAllById(List.of(1L))).thenReturn(List.of(delivery));
            when(paymentRepository.findPendingOrCompletedPaymentsForDeliveries(anyList()))
                    .thenReturn(Collections.emptyList());
            when(siteConfigurationRepository.findActiveConfiguration())
                    .thenReturn(Optional.of(defaultSiteConfig()));
            when(splitCalculator.toCents(any(BigDecimal.class))).thenReturn(BigDecimal.valueOf(1500));
            when(splitCalculator.calculateCourierAmount(any(), any())).thenReturn(BigDecimal.valueOf(1305));
            when(splitCalculator.calculatePlatformAmount(any(), any(), any())).thenReturn(BigDecimal.valueOf(195));
            when(pagarMeService.createOrderWithFullResponse(any(OrderRequest.class)))
                    .thenThrow(new RuntimeException("Connection refused"));

            // saveFailedPayment e chamado internamente — mock para nao quebrar
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
                Payment p = inv.getArgument(0);
                p.setId(200L);
                return p;
            });

            assertThatThrownBy(() -> paymentService.createPaymentWithSplit(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Erro ao processar pagamento PIX");
        }
    }

    // ================================================================
    // Validacao de deliveries para pagamento
    // ================================================================

    @Nested
    @DisplayName("validateDeliveriesForPayment() - Validacoes")
    class ValidateDeliveriesTests {

        @Test
        @DisplayName("Rejeita delivery com status diferente de COMPLETED")
        void rejeitaDeliveryNaoCompleted() {
            User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            Delivery delivery = makeDelivery(1L, client);
            delivery.setStatus(Delivery.DeliveryStatus.PENDING);
            PaymentRequest request = makePaymentRequest(List.of(1L), BigDecimal.valueOf(15.00));

            when(deliveryRepository.findAllById(List.of(1L))).thenReturn(List.of(delivery));
            when(paymentRepository.findPendingOrCompletedPaymentsForDeliveries(anyList()))
                    .thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> paymentService.createPaymentWithSplit(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("não está COMPLETED");
        }

        @Test
        @DisplayName("Rejeita deliveries de clientes diferentes")
        void rejeitaClientesDiferentes() {
            User client1 = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            UUID client2Id = UUID.fromString("00000000-0000-0000-0000-000000000099");
            User client2 = makeUser(client2Id, "Restaurante Y", User.Role.CLIENT);

            Delivery d1 = makeDelivery(1L, client1);
            Delivery d2 = makeDelivery(2L, client2);
            PaymentRequest request = makePaymentRequest(List.of(1L, 2L), BigDecimal.valueOf(30.00));

            when(deliveryRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(d1, d2));
            when(paymentRepository.findPendingOrCompletedPaymentsForDeliveries(anyList()))
                    .thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> paymentService.createPaymentWithSplit(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("pertence a outro cliente");
        }

        @Test
        @DisplayName("Rejeita quando ja existe pagamento ativo para as deliveries")
        void rejeitaPagamentoDuplicado() {
            User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            Delivery delivery = makeDelivery(1L, client);
            PaymentRequest request = makePaymentRequest(List.of(1L), BigDecimal.valueOf(15.00));

            Payment existingPayment = makePayment(50L, PaymentStatus.PENDING, client);
            existingPayment.addDelivery(delivery);

            when(deliveryRepository.findAllById(List.of(1L))).thenReturn(List.of(delivery));
            when(paymentRepository.findPendingOrCompletedPaymentsForDeliveries(anyList()))
                    .thenReturn(List.of(existingPayment));

            assertThatThrownBy(() -> paymentService.createPaymentWithSplit(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Já existe um pagamento");
        }

        @Test
        @DisplayName("Rejeita quando ja existe pagamento PAID para a delivery")
        void rejeitaDeliveryJaPaga() {
            User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            Delivery delivery = makeDelivery(1L, client);
            PaymentRequest request = makePaymentRequest(List.of(1L), BigDecimal.valueOf(15.00));

            Payment paidPayment = makePayment(51L, PaymentStatus.PAID, client);
            paidPayment.addDelivery(delivery);

            when(deliveryRepository.findAllById(List.of(1L))).thenReturn(List.of(delivery));
            when(paymentRepository.findPendingOrCompletedPaymentsForDeliveries(anyList()))
                    .thenReturn(List.of(paidPayment));

            assertThatThrownBy(() -> paymentService.createPaymentWithSplit(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("PAGO");
        }
    }

    // ================================================================
    // processPaymentConfirmation — Webhook de pagamento confirmado
    // ================================================================

    @Nested
    @DisplayName("processPaymentConfirmation() - Webhook pagamento confirmado")
    class ProcessPaymentConfirmationTests {

        @Test
        @DisplayName("Confirma pagamento PENDING com sucesso")
        void confirmaPagamentoPending() {
            User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            Payment payment = makePayment(1L, PaymentStatus.PENDING, client);
            Delivery delivery = makeDelivery(10L, client);
            delivery.setStatus(Delivery.DeliveryStatus.WAITING_PAYMENT);
            payment.addDelivery(delivery);

            when(paymentRepository.findByProviderPaymentId("or_test_1")).thenReturn(Optional.of(payment));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
            when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));

            paymentService.processPaymentConfirmation("or_test_1");

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(payment.getPaymentDate()).isNotNull();
            verify(paymentRepository).save(payment);
        }

        @Test
        @DisplayName("Delivery WAITING_PAYMENT transiciona para ACCEPTED apos confirmacao")
        void deliveryTransicionaParaAccepted() {
            User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            Payment payment = makePayment(1L, PaymentStatus.PENDING, client);
            Delivery delivery = makeDelivery(10L, client);
            delivery.setStatus(Delivery.DeliveryStatus.WAITING_PAYMENT);
            payment.addDelivery(delivery);

            when(paymentRepository.findByProviderPaymentId("or_test_1")).thenReturn(Optional.of(payment));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
            when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));

            paymentService.processPaymentConfirmation("or_test_1");

            assertThat(delivery.getStatus()).isEqualTo(Delivery.DeliveryStatus.ACCEPTED);
            assertThat(delivery.getPaymentCompleted()).isTrue();
            assertThat(delivery.getPaymentCaptured()).isTrue();
            verify(deliveryRepository).save(delivery);
        }

        @Test
        @DisplayName("Ignora se payment ja esta PAID (idempotente)")
        void ignoraSeJaPaid() {
            User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            Payment payment = makePayment(1L, PaymentStatus.PAID, client);

            when(paymentRepository.findByProviderPaymentId("or_test_1")).thenReturn(Optional.of(payment));

            paymentService.processPaymentConfirmation("or_test_1");

            // Nao deve salvar novamente
            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Lanca excecao quando order nao encontrada")
        void lancaExcecaoQuandoOrderNaoEncontrada() {
            when(paymentRepository.findByProviderPaymentId("or_inexistente"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.processPaymentConfirmation("or_inexistente"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Payment não encontrado");
        }

        @Test
        @DisplayName("Delivery que nao esta em WAITING_PAYMENT nao e alterada")
        void deliveryNaoWaitingNaoAlterada() {
            User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            Payment payment = makePayment(1L, PaymentStatus.PENDING, client);
            Delivery delivery = makeDelivery(10L, client);
            delivery.setStatus(Delivery.DeliveryStatus.COMPLETED);
            payment.addDelivery(delivery);

            when(paymentRepository.findByProviderPaymentId("or_test_1")).thenReturn(Optional.of(payment));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

            paymentService.processPaymentConfirmation("or_test_1");

            // Payment e marcado como PAID, mas delivery nao muda
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(delivery.getStatus()).isEqualTo(Delivery.DeliveryStatus.COMPLETED);
            verify(deliveryRepository, never()).save(any());
        }
    }

    // ================================================================
    // processPaymentExpiration — Webhook de pagamento expirado
    // ================================================================

    @Nested
    @DisplayName("processPaymentExpiration() - Webhook pagamento expirado")
    class ProcessPaymentExpirationTests {

        @Test
        @DisplayName("Ignora quando payment nao encontrado")
        void ignoraQuandoNaoEncontrado() {
            when(paymentRepository.findByProviderPaymentId("or_inexistente"))
                    .thenReturn(Optional.empty());

            paymentService.processPaymentExpiration("or_inexistente");

            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Ignora webhook duplicado se payment ja EXPIRED")
        void ignoraWebhookDuplicadoExpired() {
            User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            Payment payment = makePayment(1L, PaymentStatus.EXPIRED, client);

            when(paymentRepository.findByProviderPaymentId("or_test_1")).thenReturn(Optional.of(payment));

            paymentService.processPaymentExpiration("or_test_1");

            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Ignora expiracao tardia se payment ja PAID")
        void ignoraExpiracaoTardiaQuandoPaid() {
            User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            Payment payment = makePayment(1L, PaymentStatus.PAID, client);

            when(paymentRepository.findByProviderPaymentId("or_test_1")).thenReturn(Optional.of(payment));

            paymentService.processPaymentExpiration("or_test_1");

            verify(paymentRepository, never()).save(any());
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        }

        @Test
        @DisplayName("Payer nao-CUSTOMER: apenas marca como EXPIRED sem reverter deliveries")
        void payerNaoCustomerApenasExpira() {
            User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            Payment payment = makePayment(1L, PaymentStatus.PENDING, client);

            when(paymentRepository.findByProviderPaymentId("or_test_1")).thenReturn(Optional.of(payment));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

            paymentService.processPaymentExpiration("or_test_1");

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.EXPIRED);
            verify(paymentRepository).save(payment);
            verify(deliveryRepository, never()).save(any());
        }

        @Test
        @DisplayName("Payer CUSTOMER: marca EXPIRED e reverte deliveries WAITING_PAYMENT para PENDING")
        void payerCustomerReverteDeliveries() {
            User customer = makeUser(customerId, "Joao Cliente", User.Role.CUSTOMER);
            Payment payment = makePayment(1L, PaymentStatus.PENDING, customer);
            Delivery delivery = makeDelivery(10L, customer);
            delivery.setStatus(Delivery.DeliveryStatus.WAITING_PAYMENT);
            delivery.setCourier(makeCourier());
            payment.addDelivery(delivery);

            when(paymentRepository.findByProviderPaymentId("or_test_1")).thenReturn(Optional.of(payment));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
            when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));

            paymentService.processPaymentExpiration("or_test_1");

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.EXPIRED);
            assertThat(delivery.getStatus()).isEqualTo(Delivery.DeliveryStatus.PENDING);
            assertThat(delivery.getCourier()).isNull();
            assertThat(delivery.getAcceptedAt()).isNull();
            assertThat(delivery.getVehicle()).isNull();
            assertThat(delivery.getPaymentCompleted()).isFalse();
            assertThat(delivery.getPaymentCaptured()).isFalse();
            verify(deliveryNotificationService).notifyAvailableDrivers(delivery);
        }

        @Test
        @DisplayName("Nao reverte delivery que nao esta em WAITING_PAYMENT")
        void naoReverteDeliveryCompletada() {
            User customer = makeUser(customerId, "Joao Cliente", User.Role.CUSTOMER);
            Payment payment = makePayment(1L, PaymentStatus.PENDING, customer);
            Delivery delivery = makeDelivery(10L, customer);
            delivery.setStatus(Delivery.DeliveryStatus.COMPLETED);
            payment.addDelivery(delivery);

            when(paymentRepository.findByProviderPaymentId("or_test_1")).thenReturn(Optional.of(payment));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

            paymentService.processPaymentExpiration("or_test_1");

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.EXPIRED);
            assertThat(delivery.getStatus()).isEqualTo(Delivery.DeliveryStatus.COMPLETED);
            verify(deliveryRepository, never()).save(any());
        }

        @Test
        @DisplayName("Payer null: apenas marca como EXPIRED")
        void payerNullApenasExpira() {
            Payment payment = makePayment(1L, PaymentStatus.PENDING, null);

            when(paymentRepository.findByProviderPaymentId("or_test_1")).thenReturn(Optional.of(payment));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

            paymentService.processPaymentExpiration("or_test_1");

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.EXPIRED);
        }
    }

    // ================================================================
    // processAutoPayment — Pagamento automatico
    // ================================================================

    @Nested
    @DisplayName("processAutoPayment() - Pagamento automatico por preferencia")
    class ProcessAutoPaymentTests {

        @Test
        @DisplayName("Lanca excecao quando delivery nao encontrada")
        void deliveryNaoEncontrada() {
            when(deliveryRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.processAutoPayment(999L, clientId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Delivery não encontrada");
        }

        @Test
        @DisplayName("Lanca excecao quando delivery nao pertence ao cliente")
        void deliveryNaoPertenteAoCliente() {
            User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            Delivery delivery = makeDelivery(1L, client);

            UUID outroClientId = UUID.randomUUID();
            when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));

            assertThatThrownBy(() -> paymentService.processAutoPayment(1L, outroClientId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("não pertence ao cliente");
        }

        @Test
        @DisplayName("Rejeita delivery com status PENDING")
        void rejeitaStatusPending() {
            User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            Delivery delivery = makeDelivery(1L, client);
            delivery.setStatus(Delivery.DeliveryStatus.PENDING);

            when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));

            assertThatThrownBy(() -> paymentService.processAutoPayment(1L, clientId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("não pode ser paga no status atual");
        }

        @Test
        @DisplayName("Rejeita delivery ja paga")
        void rejeitaDeliveryJaPaga() {
            User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            Delivery delivery = makeDelivery(1L, client);
            delivery.setPaymentCompleted(true);
            delivery.setPaymentCaptured(true);

            when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));

            assertThatThrownBy(() -> paymentService.processAutoPayment(1L, clientId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("já foi paga");
        }

        @Test
        @DisplayName("Lanca excecao quando cliente sem preferencia")
        void semPreferencia() {
            User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            Delivery delivery = makeDelivery(1L, client);

            when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));
            when(preferenceService.getPreference(clientId)).thenReturn(null);

            assertThatThrownBy(() -> paymentService.processAutoPayment(1L, clientId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("preferência de pagamento");
        }

        @Test
        @DisplayName("Aceita delivery em status WAITING_PAYMENT — nao lanca excecao de status")
        void aceitaStatusWaitingPayment() {
            User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            Delivery delivery = makeDelivery(1L, client);
            delivery.setStatus(Delivery.DeliveryStatus.WAITING_PAYMENT);
            delivery.setPaymentCompleted(false);
            delivery.setPaymentCaptured(false);

            when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));
            when(preferenceService.getPreference(clientId)).thenReturn(makePixPreference());
            when(paymentRepository.existsPendingPaymentForDelivery(1L)).thenReturn(false);

            // processPixPayment chama createPaymentWithSplit que valida status COMPLETED
            // A chamada de processAutoPayment nao deve falhar no check de allowedStatuses
            // mas pode falhar depois em validateDeliveriesForPayment (status != COMPLETED)
            // Verificamos que a mensagem NAO e sobre status invalido para pagamento automatico
            try {
                // Mock para createPaymentWithSplit
                when(deliveryRepository.findAllById(anyList())).thenReturn(List.of(delivery));
                when(paymentRepository.findPendingOrCompletedPaymentsForDeliveries(anyList()))
                        .thenReturn(Collections.emptyList());

                paymentService.processAutoPayment(1L, clientId);
            } catch (Exception e) {
                // Se falhar, deve ser por outro motivo (nao por status invalido no processAutoPayment)
                assertThat(e.getMessage()).doesNotContain("não pode ser paga no status atual");
            }
        }

        @Test
        @DisplayName("Aceita delivery em status ACCEPTED, IN_TRANSIT e COMPLETED")
        void aceitaStatusesPermitidos() {
            User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);

            for (Delivery.DeliveryStatus status : List.of(
                    Delivery.DeliveryStatus.ACCEPTED,
                    Delivery.DeliveryStatus.IN_TRANSIT,
                    Delivery.DeliveryStatus.COMPLETED)) {

                Delivery delivery = makeDelivery(1L, client);
                delivery.setStatus(status);
                delivery.setPaymentCompleted(false);
                delivery.setPaymentCaptured(false);

                when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));
                when(preferenceService.getPreference(clientId)).thenReturn(makePixPreference());
                // Valida que nao lanca excecao ate chegar ao processamento PIX
                when(paymentRepository.existsPendingPaymentForDelivery(1L)).thenReturn(false);
                when(deliveryRepository.findAllById(anyList())).thenReturn(List.of(delivery));
                when(paymentRepository.findPendingOrCompletedPaymentsForDeliveries(anyList()))
                        .thenReturn(Collections.emptyList());
                when(siteConfigurationRepository.findActiveConfiguration())
                        .thenReturn(Optional.of(defaultSiteConfig()));
                when(splitCalculator.toCents(any(BigDecimal.class))).thenReturn(BigDecimal.valueOf(1500));
                when(splitCalculator.calculateCourierAmount(any(), any())).thenReturn(BigDecimal.valueOf(1305));
                when(splitCalculator.calculatePlatformAmount(any(), any(), any())).thenReturn(BigDecimal.valueOf(195));

                try {
                    when(pagarMeService.createOrderWithFullResponse(any(OrderRequest.class)))
                            .thenReturn(makeOrderResponseWithPixQrCode("or_test_" + status));
                    when(objectMapper.writeValueAsString(any())).thenReturn("{}");
                    when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
                        Payment p = inv.getArgument(0);
                        p.setId(400L);
                        return p;
                    });

                    PaymentResponse result = paymentService.processAutoPayment(1L, clientId);
                    assertThat(result).isNotNull();
                } catch (Exception e) {
                    // Se falhar por outro motivo (nao por status invalido), esta OK
                    assertThat(e.getMessage()).doesNotContain("não pode ser paga no status atual");
                }
            }
        }
    }

    // ================================================================
    // Pagamento PIX — Duplicidade
    // ================================================================

    @Nested
    @DisplayName("processPixPayment() - Prevencao de duplicidade")
    class PixDuplicidadeTests {

        @Test
        @DisplayName("Rejeita quando ja existe pagamento PENDING para a delivery")
        void rejeitaPagamentoPendingExistente() {
            User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            Delivery delivery = makeDelivery(1L, client);
            delivery.setStatus(Delivery.DeliveryStatus.COMPLETED);
            delivery.setPaymentCompleted(false);
            delivery.setPaymentCaptured(false);

            when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));
            when(preferenceService.getPreference(clientId)).thenReturn(makePixPreference());
            when(paymentRepository.existsPendingPaymentForDelivery(1L)).thenReturn(true);

            assertThatThrownBy(() -> paymentService.processAutoPayment(1L, clientId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("pagamento pendente");
        }
    }

    // ================================================================
    // Pagamento Cartao — Duplicidade
    // ================================================================

    @Nested
    @DisplayName("processCreditCardPayment() - Prevencao de duplicidade")
    class CartaoDuplicidadeTests {

        @Test
        @DisplayName("Rejeita cartao quando ja existe pagamento PENDING para a delivery")
        void rejeitaPagamentoPendingExistente() {
            User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            Delivery delivery = makeDelivery(1L, client);
            delivery.setStatus(Delivery.DeliveryStatus.COMPLETED);
            delivery.setPaymentCompleted(false);
            delivery.setPaymentCaptured(false);

            CustomerCard card = makeCard();
            CustomerPaymentPreference pref = makeCreditCardPreference(card);

            when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));
            when(preferenceService.getPreference(clientId)).thenReturn(pref);
            when(paymentRepository.existsPendingPaymentForDelivery(1L)).thenReturn(true);

            assertThatThrownBy(() -> paymentService.processAutoPayment(1L, clientId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("pagamento pendente");
        }

        @Test
        @DisplayName("Rejeita quando cartao padrao e nulo")
        void rejeitaCartaoNulo() {
            User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            Delivery delivery = makeDelivery(1L, client);
            delivery.setStatus(Delivery.DeliveryStatus.COMPLETED);
            delivery.setPaymentCompleted(false);
            delivery.setPaymentCaptured(false);

            CustomerPaymentPreference pref = CustomerPaymentPreference.builder()
                    .id(1L)
                    .preferredPaymentType(PreferredPaymentType.CREDIT_CARD)
                    .defaultCard(null)
                    .build();

            when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));
            when(preferenceService.getPreference(clientId)).thenReturn(pref);
            when(paymentRepository.existsPendingPaymentForDelivery(1L)).thenReturn(false);

            assertThatThrownBy(() -> paymentService.processAutoPayment(1L, clientId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("cartão padrão");
        }

        @Test
        @DisplayName("Rejeita quando cartao padrao esta inativo")
        void rejeitaCartaoInativo() {
            User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            Delivery delivery = makeDelivery(1L, client);
            delivery.setStatus(Delivery.DeliveryStatus.COMPLETED);
            delivery.setPaymentCompleted(false);
            delivery.setPaymentCaptured(false);

            CustomerCard card = makeCard();
            card.setIsActive(false);
            CustomerPaymentPreference pref = makeCreditCardPreference(card);

            when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));
            when(preferenceService.getPreference(clientId)).thenReturn(pref);
            when(paymentRepository.existsPendingPaymentForDelivery(1L)).thenReturn(false);

            assertThatThrownBy(() -> paymentService.processAutoPayment(1L, clientId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("inativo");
        }

        @Test
        @DisplayName("Rejeita quando courier nao tem recipient Pagar.me")
        void rejeitaCourierSemRecipient() {
            User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            Delivery delivery = makeDelivery(1L, client);
            delivery.setStatus(Delivery.DeliveryStatus.COMPLETED);
            delivery.setPaymentCompleted(false);
            delivery.setPaymentCaptured(false);

            User courierSemRecipient = makeUser(courierId, "Pedro", User.Role.COURIER);
            courierSemRecipient.setPagarmeRecipientId(null);
            delivery.setCourier(courierSemRecipient);

            CustomerCard card = makeCard();
            CustomerPaymentPreference pref = makeCreditCardPreference(card);

            when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));
            when(preferenceService.getPreference(clientId)).thenReturn(pref);
            when(paymentRepository.existsPendingPaymentForDelivery(1L)).thenReturn(false);

            assertThatThrownBy(() -> paymentService.processAutoPayment(1L, clientId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("conta Pagar.me");
        }
    }

    // ================================================================
    // saveFailedPayment
    // ================================================================

    @Nested
    @DisplayName("saveFailedPayment() - Persistencia de pagamentos falhados")
    class SaveFailedPaymentTests {

        @Test
        @DisplayName("Salva payment com status FAILED")
        void salvaPaymentFailed() {
            User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            Delivery delivery = makeDelivery(1L, client);

            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
                Payment p = inv.getArgument(0);
                p.setId(500L);
                return p;
            });

            Payment result = paymentService.saveFailedPayment(
                    BigDecimal.valueOf(15.00),
                    PaymentMethod.PIX,
                    client,
                    delivery,
                    "Connection refused"
            );

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(result.getNotes()).isEqualTo("Connection refused");
            assertThat(result.getAmount()).isEqualByComparingTo("15.00");
            assertThat(result.getPaymentMethod()).isEqualTo(PaymentMethod.PIX);
            assertThat(result.getProvider()).isEqualTo(PaymentProvider.PAGARME);
            assertThat(result.getCurrency()).isEqualTo(com.mvt.mvt_events.jpa.Currency.BRL);
            assertThat(result.getPayer()).isEqualTo(client);

            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(captor.capture());
            assertThat(captor.getValue().getDeliveries()).contains(delivery);
        }

        @Test
        @DisplayName("Salva payment FAILED para cartao de credito")
        void salvaPaymentFailedCartao() {
            User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            Delivery delivery = makeDelivery(1L, client);

            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
                Payment p = inv.getArgument(0);
                p.setId(501L);
                return p;
            });

            Payment result = paymentService.saveFailedPayment(
                    BigDecimal.valueOf(25.00),
                    PaymentMethod.CREDIT_CARD,
                    client,
                    delivery,
                    "Card declined"
            );

            assertThat(result.getPaymentMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.FAILED);
        }
    }

    // ================================================================
    // determinePaymentStatusFromOrder (via extractPaymentFailureMessage)
    // ================================================================

    @Nested
    @DisplayName("extractPaymentFailureMessage() - Mensagens de falha amigaveis")
    class ExtractPaymentFailureMessageTests {

        @Test
        @DisplayName("OrderResponse null: retorna mensagem padrao")
        void orderResponseNull() {
            String msg = paymentService.extractPaymentFailureMessage(null);
            assertThat(msg).isEqualTo("Pagamento não processado");
        }

        @Test
        @DisplayName("Transacao not_authorized: mensagem em portugues")
        void transacaoNaoAutorizada() {
            OrderResponse.LastTransaction tx = OrderResponse.LastTransaction.builder()
                    .status("not_authorized")
                    .build();
            OrderResponse.Charge charge = OrderResponse.Charge.builder()
                    .lastTransaction(tx)
                    .build();
            OrderResponse response = OrderResponse.builder()
                    .status("failed")
                    .charges(List.of(charge))
                    .build();

            String msg = paymentService.extractPaymentFailureMessage(response);
            assertThat(msg).isEqualTo("Transação não autorizada");
        }

        @Test
        @DisplayName("Transacao refused: mensagem em portugues")
        void transacaoRecusada() {
            OrderResponse.LastTransaction tx = OrderResponse.LastTransaction.builder()
                    .status("refused")
                    .build();
            OrderResponse.Charge charge = OrderResponse.Charge.builder()
                    .lastTransaction(tx)
                    .build();
            OrderResponse response = OrderResponse.builder()
                    .charges(List.of(charge))
                    .build();

            String msg = paymentService.extractPaymentFailureMessage(response);
            assertThat(msg).isEqualTo("Transação recusada");
        }

        @Test
        @DisplayName("Transacao failed: mensagem em portugues")
        void transacaoFalhou() {
            OrderResponse.LastTransaction tx = OrderResponse.LastTransaction.builder()
                    .status("failed")
                    .build();
            OrderResponse.Charge charge = OrderResponse.Charge.builder()
                    .lastTransaction(tx)
                    .build();
            OrderResponse response = OrderResponse.builder()
                    .charges(List.of(charge))
                    .build();

            String msg = paymentService.extractPaymentFailureMessage(response);
            assertThat(msg).isEqualTo("Transação falhou");
        }

        @Test
        @DisplayName("Transacao authorized/paid: mensagem positiva")
        void transacaoAprovada() {
            OrderResponse.LastTransaction tx = OrderResponse.LastTransaction.builder()
                    .status("authorized")
                    .build();
            OrderResponse.Charge charge = OrderResponse.Charge.builder()
                    .lastTransaction(tx)
                    .build();
            OrderResponse response = OrderResponse.builder()
                    .charges(List.of(charge))
                    .build();

            String msg = paymentService.extractPaymentFailureMessage(response);
            assertThat(msg).isEqualTo("Transação aprovada");
        }

        @Test
        @DisplayName("Antifraude reproved: mensagem de seguranca")
        void antifraudeReproved() {
            Map<String, Object> antifraud = new HashMap<>();
            antifraud.put("status", "reproved");

            OrderResponse.LastTransaction tx = OrderResponse.LastTransaction.builder()
                    .status("not_authorized")
                    .antifraudResponse(antifraud)
                    .build();
            OrderResponse.Charge charge = OrderResponse.Charge.builder()
                    .lastTransaction(tx)
                    .build();
            OrderResponse response = OrderResponse.builder()
                    .charges(List.of(charge))
                    .build();

            String msg = paymentService.extractPaymentFailureMessage(response);
            assertThat(msg).isEqualTo("Transação bloqueada por segurança");
        }

        @Test
        @DisplayName("Order status failed sem charges: mensagem padrao")
        void orderFailedSemCharges() {
            OrderResponse response = OrderResponse.builder()
                    .status("failed")
                    .build();

            String msg = paymentService.extractPaymentFailureMessage(response);
            assertThat(msg).isEqualTo("Pagamento não processado");
        }

        @Test
        @DisplayName("Order com status desconhecido: exibe status generico")
        void orderStatusDesconhecido() {
            OrderResponse response = OrderResponse.builder()
                    .status("unknown_status")
                    .build();

            String msg = paymentService.extractPaymentFailureMessage(response);
            assertThat(msg).contains("unknown_status");
        }

        @Test
        @DisplayName("Transacao com status desconhecido: exibe status generico")
        void transacaoStatusDesconhecido() {
            OrderResponse.LastTransaction tx = OrderResponse.LastTransaction.builder()
                    .status("some_new_status")
                    .build();
            OrderResponse.Charge charge = OrderResponse.Charge.builder()
                    .lastTransaction(tx)
                    .build();
            OrderResponse response = OrderResponse.builder()
                    .charges(List.of(charge))
                    .build();

            String msg = paymentService.extractPaymentFailureMessage(response);
            assertThat(msg).contains("some_new_status");
        }
    }

    // ================================================================
    // generatePaymentReport
    // ================================================================

    @Nested
    @DisplayName("generatePaymentReport() - Relatorio de pagamento")
    class GeneratePaymentReportTests {

        @Test
        @DisplayName("Lanca excecao quando payment nao encontrado")
        void paymentNaoEncontrado() {
            when(paymentRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.generatePaymentReport(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Payment não encontrado");
        }

        @Test
        @DisplayName("Lanca excecao quando configuracao nao encontrada")
        void configuracaoNaoEncontrada() {
            User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            Payment payment = makePayment(1L, PaymentStatus.PAID, client);
            Delivery delivery = makeDelivery(10L, client);
            payment.addDelivery(delivery);

            when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
            when(siteConfigurationRepository.findActiveConfiguration()).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.generatePaymentReport(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("configuração ativa");
        }

        @Test
        @DisplayName("Gera relatorio com splits para delivery com organizer")
        void geraRelatorioComOrganizer() {
            User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            User courier = makeCourier();
            User organizer = makeOrganizer();

            Payment payment = makePayment(1L, PaymentStatus.PAID, client);
            Delivery delivery = makeDelivery(10L, client);
            delivery.setCourier(courier);
            delivery.setOrganizer(organizer);
            delivery.setCompletedAt(OffsetDateTime.now(ZoneId.of("America/Fortaleza")));
            payment.addDelivery(delivery);

            SiteConfiguration config = defaultSiteConfig();

            when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
            when(siteConfigurationRepository.findActiveConfiguration()).thenReturn(Optional.of(config));
            when(splitCalculator.toCents(any(BigDecimal.class))).thenReturn(BigDecimal.valueOf(1500));
            when(splitCalculator.hasValidOrganizer(any(Delivery.class))).thenReturn(true);
            when(splitCalculator.calculateCourierAmount(any(), any())).thenReturn(BigDecimal.valueOf(1305));
            when(splitCalculator.calculateOrganizerAmount(any(), any())).thenReturn(BigDecimal.valueOf(75));
            when(splitCalculator.calculatePlatformAmount(any(), any(), any())).thenReturn(BigDecimal.valueOf(120));
            when(splitCalculator.calculateCourierPercentage(any())).thenReturn(BigDecimal.valueOf(87));
            when(splitCalculator.calculatePlatformPercentage(any(), eq(true))).thenReturn(BigDecimal.valueOf(8));
            when(splitCalculator.toReais(any(BigDecimal.class), anyInt())).thenAnswer(inv -> {
                BigDecimal cents = inv.getArgument(0);
                return cents.divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            });

            var report = paymentService.generatePaymentReport(1L);

            assertThat(report).isNotNull();
            assertThat(report.getPaymentId()).isEqualTo(1L);
            assertThat(report.getStatus()).isEqualTo("PAID");
            assertThat(report.getDeliveries()).hasSize(1);

            // Deve ter 3 splits consolidados: COURIER, ORGANIZER, PLATFORM
            assertThat(report.getConsolidatedSplits()).hasSize(3);
        }

        @Test
        @DisplayName("Gera relatorio sem organizer — 2 splits (COURIER + PLATFORM)")
        void geraRelatorioSemOrganizer() {
            User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            User courier = makeCourier();

            Payment payment = makePayment(1L, PaymentStatus.PAID, client);
            Delivery delivery = makeDelivery(10L, client);
            delivery.setCourier(courier);
            delivery.setOrganizer(null);
            delivery.setCompletedAt(OffsetDateTime.now(ZoneId.of("America/Fortaleza")));
            payment.addDelivery(delivery);

            SiteConfiguration config = defaultSiteConfig();

            when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
            when(siteConfigurationRepository.findActiveConfiguration()).thenReturn(Optional.of(config));
            when(splitCalculator.toCents(any(BigDecimal.class))).thenReturn(BigDecimal.valueOf(1500));
            when(splitCalculator.hasValidOrganizer(any(Delivery.class))).thenReturn(false);
            when(splitCalculator.calculateCourierAmount(any(), any())).thenReturn(BigDecimal.valueOf(1305));
            when(splitCalculator.calculatePlatformAmount(any(), any(), any())).thenReturn(BigDecimal.valueOf(195));
            when(splitCalculator.calculateCourierPercentage(any())).thenReturn(BigDecimal.valueOf(87));
            when(splitCalculator.calculatePlatformPercentage(any(), eq(false))).thenReturn(BigDecimal.valueOf(13));
            when(splitCalculator.toReais(any(BigDecimal.class), anyInt())).thenAnswer(inv -> {
                BigDecimal cents = inv.getArgument(0);
                return cents.divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            });

            var report = paymentService.generatePaymentReport(1L);

            assertThat(report).isNotNull();
            assertThat(report.getDeliveries()).hasSize(1);
            // Deve ter 2 splits consolidados: COURIER + PLATFORM
            assertThat(report.getConsolidatedSplits()).hasSize(2);
        }
    }

    // ================================================================
    // Edge cases
    // ================================================================

    @Nested
    @DisplayName("Edge cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Payment PIX salva QR Code e URL do Pagar.me")
        void salvaQrCodeEUrl() throws Exception {
            User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            Delivery delivery = makeDelivery(1L, client);
            PaymentRequest request = makePaymentRequest(List.of(1L), BigDecimal.valueOf(15.00));

            when(deliveryRepository.findAllById(List.of(1L))).thenReturn(List.of(delivery));
            when(paymentRepository.findPendingOrCompletedPaymentsForDeliveries(anyList()))
                    .thenReturn(Collections.emptyList());
            when(siteConfigurationRepository.findActiveConfiguration())
                    .thenReturn(Optional.of(defaultSiteConfig()));
            when(splitCalculator.toCents(any(BigDecimal.class))).thenReturn(BigDecimal.valueOf(1500));
            when(splitCalculator.calculateCourierAmount(any(), any())).thenReturn(BigDecimal.valueOf(1305));
            when(splitCalculator.calculatePlatformAmount(any(), any(), any())).thenReturn(BigDecimal.valueOf(195));
            when(pagarMeService.createOrderWithFullResponse(any(OrderRequest.class)))
                    .thenReturn(makeOrderResponseWithPixQrCode("or_qr_test"));
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
                Payment p = inv.getArgument(0);
                p.setId(600L);
                return p;
            });

            paymentService.createPaymentWithSplit(request);

            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(captor.capture());
            Payment saved = captor.getValue();

            assertThat(saved.getPixQrCode()).isEqualTo("00020126360014BR.GOV.BCB.PIX...");
            assertThat(saved.getPixQrCodeUrl()).isEqualTo("https://api.pagar.me/qr/123.png");
            assertThat(saved.getExpiresAt()).isNotNull();
        }

        @Test
        @DisplayName("Payment sem charges no response: nao quebra (QR Code null)")
        void semChargesNoResponse() throws Exception {
            User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            Delivery delivery = makeDelivery(1L, client);
            PaymentRequest request = makePaymentRequest(List.of(1L), BigDecimal.valueOf(15.00));

            OrderResponse responseNoCharges = OrderResponse.builder()
                    .id("or_no_charges")
                    .status("pending")
                    .charges(null)
                    .build();

            when(deliveryRepository.findAllById(List.of(1L))).thenReturn(List.of(delivery));
            when(paymentRepository.findPendingOrCompletedPaymentsForDeliveries(anyList()))
                    .thenReturn(Collections.emptyList());
            when(siteConfigurationRepository.findActiveConfiguration())
                    .thenReturn(Optional.of(defaultSiteConfig()));
            when(splitCalculator.toCents(any(BigDecimal.class))).thenReturn(BigDecimal.valueOf(1500));
            when(splitCalculator.calculateCourierAmount(any(), any())).thenReturn(BigDecimal.valueOf(1305));
            when(splitCalculator.calculatePlatformAmount(any(), any(), any())).thenReturn(BigDecimal.valueOf(195));
            when(pagarMeService.createOrderWithFullResponse(any(OrderRequest.class)))
                    .thenReturn(responseNoCharges);
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
                Payment p = inv.getArgument(0);
                p.setId(601L);
                return p;
            });

            PaymentResponse result = paymentService.createPaymentWithSplit(request);

            assertThat(result).isNotNull();

            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(captor.capture());
            Payment saved = captor.getValue();
            assertThat(saved.getPixQrCode()).isNull();
            assertThat(saved.getPixQrCodeUrl()).isNull();
        }

        @Test
        @DisplayName("PIX courier sem recipient lanca excecao no processAutoPayment")
        void courierSemRecipientPixAutoPayment() {
            User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            Delivery delivery = makeDelivery(1L, client);
            delivery.setStatus(Delivery.DeliveryStatus.COMPLETED);
            delivery.setPaymentCompleted(false);
            delivery.setPaymentCaptured(false);

            User courierSem = makeUser(courierId, "Pedro", User.Role.COURIER);
            courierSem.setPagarmeRecipientId(null);
            delivery.setCourier(courierSem);

            when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));
            when(preferenceService.getPreference(clientId)).thenReturn(makePixPreference());
            when(paymentRepository.existsPendingPaymentForDelivery(1L)).thenReturn(false);

            assertThatThrownBy(() -> paymentService.processAutoPayment(1L, clientId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("conta Pagar.me");
        }

        @Test
        @DisplayName("Expiracao com multiplas deliveries: reverte apenas WAITING_PAYMENT")
        void expiracaoMultiplasDeliveriesReverteApenas() {
            User customer = makeUser(customerId, "Joao", User.Role.CUSTOMER);
            Payment payment = makePayment(1L, PaymentStatus.PENDING, customer);

            Delivery d1 = makeDelivery(10L, customer);
            d1.setStatus(Delivery.DeliveryStatus.WAITING_PAYMENT);
            d1.setCourier(makeCourier());

            Delivery d2 = makeDelivery(11L, customer);
            d2.setStatus(Delivery.DeliveryStatus.COMPLETED);

            Delivery d3 = makeDelivery(12L, customer);
            d3.setStatus(Delivery.DeliveryStatus.WAITING_PAYMENT);
            d3.setCourier(makeCourier());

            payment.addDelivery(d1);
            payment.addDelivery(d2);
            payment.addDelivery(d3);

            when(paymentRepository.findByProviderPaymentId("or_test_1")).thenReturn(Optional.of(payment));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
            when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));

            paymentService.processPaymentExpiration("or_test_1");

            // d1 e d3 devem ser revertidas, d2 nao
            assertThat(d1.getStatus()).isEqualTo(Delivery.DeliveryStatus.PENDING);
            assertThat(d2.getStatus()).isEqualTo(Delivery.DeliveryStatus.COMPLETED);
            assertThat(d3.getStatus()).isEqualTo(Delivery.DeliveryStatus.PENDING);

            // deliveryRepository.save chamado 2x (d1 e d3)
            verify(deliveryRepository, times(2)).save(any(Delivery.class));
            // notifyAvailableDrivers chamado 2x
            verify(deliveryNotificationService, times(2)).notifyAvailableDrivers(any());
        }

        @Test
        @DisplayName("Confirmacao com multiplas deliveries WAITING_PAYMENT: todas transitam")
        void confirmacaoMultiplasWaitingPayment() {
            User client = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            Payment payment = makePayment(1L, PaymentStatus.PENDING, client);

            Delivery d1 = makeDelivery(10L, client);
            d1.setStatus(Delivery.DeliveryStatus.WAITING_PAYMENT);

            Delivery d2 = makeDelivery(11L, client);
            d2.setStatus(Delivery.DeliveryStatus.WAITING_PAYMENT);

            payment.addDelivery(d1);
            payment.addDelivery(d2);

            when(paymentRepository.findByProviderPaymentId("or_test_1")).thenReturn(Optional.of(payment));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
            when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));

            paymentService.processPaymentConfirmation("or_test_1");

            assertThat(d1.getStatus()).isEqualTo(Delivery.DeliveryStatus.ACCEPTED);
            assertThat(d1.getPaymentCompleted()).isTrue();
            assertThat(d2.getStatus()).isEqualTo(Delivery.DeliveryStatus.ACCEPTED);
            assertThat(d2.getPaymentCompleted()).isTrue();
            verify(deliveryRepository, times(2)).save(any(Delivery.class));
        }

        @Test
        @DisplayName("Expiracao tolerante a falha na notificacao push")
        void expiracaoToleranteAFalhaPush() {
            User customer = makeUser(customerId, "Joao", User.Role.CUSTOMER);
            Payment payment = makePayment(1L, PaymentStatus.PENDING, customer);
            Delivery delivery = makeDelivery(10L, customer);
            delivery.setStatus(Delivery.DeliveryStatus.WAITING_PAYMENT);
            delivery.setCourier(makeCourier());
            payment.addDelivery(delivery);

            when(paymentRepository.findByProviderPaymentId("or_test_1")).thenReturn(Optional.of(payment));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
            when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));
            doThrow(new RuntimeException("Push failed"))
                    .when(deliveryNotificationService).notifyAvailableDrivers(any());

            // Nao deve propagar a excecao da notificacao
            assertThatCode(() -> paymentService.processPaymentExpiration("or_test_1"))
                    .doesNotThrowAnyException();

            // Delivery ainda deve ser revertida
            assertThat(delivery.getStatus()).isEqualTo(Delivery.DeliveryStatus.PENDING);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.EXPIRED);
        }
    }
}
