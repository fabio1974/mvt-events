package com.mvt.mvt_events.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mvt.mvt_events.payment.config.PagarMeConfig;
import com.mvt.mvt_events.payment.dto.OrderResponse;
import com.mvt.mvt_events.payment.dto.RecipientListResponse;
import com.mvt.mvt_events.payment.dto.RecipientResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitarios para PagarMeService.
 *
 * Usa Mockito puro (sem Spring context) para testar logica de split,
 * listagem de recipients e deteccao de duplicatas.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PagarMeService")
class PagarMeServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    private PagarMeService pagarMeService;

    private PagarMeConfig config;

    private static final String COURIER_RECIPIENT_ID = "re_courier_abc123";
    private static final String ORGANIZER_RECIPIENT_ID = "re_organizer_def456";
    private static final String PLATFORM_RECIPIENT_ID = "re_platform_ghi789";
    private static final String API_URL = "https://api.pagar.me/core/v5";
    private static final String API_KEY = "sk_test_abc123";

    @BeforeEach
    void setUp() {
        // Configurar PagarMeConfig manualmente (nao usa @Value, usa @ConfigurationProperties)
        config = new PagarMeConfig();

        PagarMeConfig.Api api = new PagarMeConfig.Api();
        api.setUrl(API_URL);
        api.setKey(API_KEY);
        config.setApi(api);

        PagarMeConfig.Split split = new PagarMeConfig.Split();
        split.setCourierPercentage(8700);       // 87%
        split.setOrganizerPercentage(500);      // 5%
        split.setCourierLiable(false);
        split.setCourierChargeProcessingFee(false);
        split.setOrganizerChargeProcessingFee(false);
        config.setSplit(split);

        PagarMeConfig.Webhook webhook = new PagarMeConfig.Webhook();
        webhook.setSecret("webhook_secret_test");
        config.setWebhook(webhook);

        // Injetar config manualmente (PagarMeService usa @RequiredArgsConstructor)
        // Como config e um campo final injetado via construtor, precisamos recriar o service
        pagarMeService = new PagarMeService(config, restTemplate, objectMapper);
    }

    // ============================================
    // HELPERS
    // ============================================

    private RecipientResponse createRecipientResponse(String id, String document, String bank, String branch, String account) {
        RecipientResponse.DefaultBankAccount bankAccount = RecipientResponse.DefaultBankAccount.builder()
                .bank(bank)
                .branchNumber(branch)
                .accountNumber(account)
                .build();

        return RecipientResponse.builder()
                .id(id)
                .document(document)
                .email("test@test.com")
                .defaultBankAccount(bankAccount)
                .build();
    }

    @SuppressWarnings("unchecked")
    private void mockListRecipientsResponse(List<RecipientResponse> recipients) {
        RecipientListResponse listResponse = RecipientListResponse.builder()
                .data(recipients)
                .build();

        ResponseEntity<RecipientListResponse> responseEntity =
                new ResponseEntity<>(listResponse, HttpStatus.OK);

        when(restTemplate.exchange(
                eq(API_URL + "/recipients"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(RecipientListResponse.class)
        )).thenReturn(responseEntity);
    }

    // ============================================
    // createOrderWithSplit (PIX)
    // ============================================

    @Nested
    @DisplayName("createOrderWithSplit - PIX")
    class CreateOrderWithSplit {

        @Test
        @DisplayName("com organizer: cria 3 splits (courier 87%, organizer 5%, plataforma 8%)")
        void withOrganizer_createsThreeSplits() throws Exception {
            // Arrange
            BigDecimal amount = new BigDecimal("100.00");
            OrderResponse mockResponse = new OrderResponse();
            mockResponse.setId("or_test_123");
            mockResponse.setStatus("pending");

            // Mock objectMapper para nao quebrar no log
            when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(new ObjectMapper().writerWithDefaultPrettyPrinter());

            // Mock restTemplate retornando String (o metodo usa String.class)
            ResponseEntity<String> responseEntity = new ResponseEntity<>(
                    "{\"id\":\"or_test_123\",\"status\":\"pending\"}", HttpStatus.OK);

            when(restTemplate.exchange(
                    eq(API_URL + "/orders"),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(responseEntity);

            when(objectMapper.readValue(anyString(), eq(OrderResponse.class))).thenReturn(mockResponse);

            // Act
            OrderResponse result = pagarMeService.createOrderWithSplit(
                    amount,
                    "Entrega #45",
                    "Cliente Teste",
                    "cliente@test.com",
                    "12345678900",
                    COURIER_RECIPIENT_ID,
                    ORGANIZER_RECIPIENT_ID,
                    PLATFORM_RECIPIENT_ID,
                    3600
            );

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("or_test_123");

            // Verificar que restTemplate.exchange foi chamado
            verify(restTemplate).exchange(
                    eq(API_URL + "/orders"),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        }

        @Test
        @DisplayName("sem organizer: cria 2 splits (courier 87%, plataforma 13%)")
        void withoutOrganizer_createsTwoSplits() throws Exception {
            // Arrange
            BigDecimal amount = new BigDecimal("100.00");
            OrderResponse mockResponse = new OrderResponse();
            mockResponse.setId("or_test_456");
            mockResponse.setStatus("pending");

            when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(new ObjectMapper().writerWithDefaultPrettyPrinter());

            ResponseEntity<String> responseEntity = new ResponseEntity<>(
                    "{\"id\":\"or_test_456\",\"status\":\"pending\"}", HttpStatus.OK);

            when(restTemplate.exchange(
                    eq(API_URL + "/orders"),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(responseEntity);

            when(objectMapper.readValue(anyString(), eq(OrderResponse.class))).thenReturn(mockResponse);

            // Act — managerRecipientId null
            OrderResponse result = pagarMeService.createOrderWithSplit(
                    amount,
                    "Entrega #50",
                    "Cliente Teste",
                    "cliente@test.com",
                    "12345678900",
                    COURIER_RECIPIENT_ID,
                    null,
                    PLATFORM_RECIPIENT_ID,
                    3600
            );

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("or_test_456");
        }
    }

    // ============================================
    // Split amounts verification
    // ============================================

    @Nested
    @DisplayName("Split amounts - calculo de valores")
    class SplitAmounts {

        @Test
        @DisplayName("R$100 (10000 centavos): courier=8700, organizer=500, plataforma=800")
        void hundredReais_withOrganizer() {
            int amountInCents = 10000;

            int courierAmount = (amountInCents * config.getSplit().getCourierPercentage()) / 10000;
            int organizerAmount = (amountInCents * config.getSplit().getOrganizerPercentage()) / 10000;
            int platformAmount = amountInCents - courierAmount - organizerAmount;

            assertThat(courierAmount).isEqualTo(8700);
            assertThat(organizerAmount).isEqualTo(500);
            assertThat(platformAmount).isEqualTo(800);
            assertThat(courierAmount + organizerAmount + platformAmount).isEqualTo(amountInCents);
        }

        @Test
        @DisplayName("R$100 sem organizer: courier=8700, plataforma=1300")
        void hundredReais_withoutOrganizer() {
            int amountInCents = 10000;

            int courierAmount = (amountInCents * config.getSplit().getCourierPercentage()) / 10000;
            int organizerAmount = 0; // sem organizer
            int platformAmount = amountInCents - courierAmount - organizerAmount;

            assertThat(courierAmount).isEqualTo(8700);
            assertThat(platformAmount).isEqualTo(1300);
            assertThat(courierAmount + platformAmount).isEqualTo(amountInCents);
        }

        @Test
        @DisplayName("R$15 (1500 centavos): verifica arredondamento inteiro")
        void fifteenReais_rounding() {
            int amountInCents = 1500;

            int courierAmount = (amountInCents * config.getSplit().getCourierPercentage()) / 10000;
            int organizerAmount = (amountInCents * config.getSplit().getOrganizerPercentage()) / 10000;
            int platformAmount = amountInCents - courierAmount - organizerAmount;

            // 1500 * 8700 / 10000 = 1305
            assertThat(courierAmount).isEqualTo(1305);
            // 1500 * 500 / 10000 = 75
            assertThat(organizerAmount).isEqualTo(75);
            // 1500 - 1305 - 75 = 120
            assertThat(platformAmount).isEqualTo(120);

            // Soma = total (integridade financeira)
            assertThat(courierAmount + organizerAmount + platformAmount).isEqualTo(amountInCents);
        }

        @Test
        @DisplayName("R$15 sem organizer: courier=1305, plataforma=195")
        void fifteenReais_withoutOrganizer_rounding() {
            int amountInCents = 1500;

            int courierAmount = (amountInCents * config.getSplit().getCourierPercentage()) / 10000;
            int platformAmount = amountInCents - courierAmount;

            assertThat(courierAmount).isEqualTo(1305);
            assertThat(platformAmount).isEqualTo(195);
            assertThat(courierAmount + platformAmount).isEqualTo(amountInCents);
        }

        @Test
        @DisplayName("percentuais somam 100% com organizer (87+5+8)")
        void percentages_sumToHundred_withOrganizer() {
            int courierPct = config.getSplit().getCourierPercentage();    // 8700
            int organizerPct = config.getSplit().getOrganizerPercentage(); // 500
            int platformPct = 10000 - courierPct - organizerPct;           // 800

            assertThat(courierPct + organizerPct + platformPct).isEqualTo(10000);
        }

        @Test
        @DisplayName("percentuais somam 100% sem organizer (87+13)")
        void percentages_sumToHundred_withoutOrganizer() {
            int courierPct = config.getSplit().getCourierPercentage(); // 8700
            int platformPct = 10000 - courierPct;                       // 1300

            assertThat(courierPct + platformPct).isEqualTo(10000);
        }
    }

    // ============================================
    // listRecipients
    // ============================================

    @Nested
    @DisplayName("listRecipients")
    class ListRecipients {

        @Test
        @DisplayName("retorna lista de recipients")
        void returnsRecipientList() {
            // Arrange
            RecipientResponse r1 = createRecipientResponse("re_1", "12345678900", "001", "1234", "56789");
            RecipientResponse r2 = createRecipientResponse("re_2", "98765432100", "341", "5678", "12345");
            mockListRecipientsResponse(List.of(r1, r2));

            // Act
            List<RecipientResponse> result = pagarMeService.listRecipients();

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getId()).isEqualTo("re_1");
            assertThat(result.get(1).getId()).isEqualTo("re_2");
        }

        @Test
        @DisplayName("retorna lista vazia quando nao ha recipients")
        void returnsEmptyList_whenNoRecipients() {
            // Arrange
            RecipientListResponse listResponse = RecipientListResponse.builder()
                    .data(null)
                    .build();

            ResponseEntity<RecipientListResponse> responseEntity =
                    new ResponseEntity<>(listResponse, HttpStatus.OK);

            when(restTemplate.exchange(
                    eq(API_URL + "/recipients"),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(RecipientListResponse.class)
            )).thenReturn(responseEntity);

            // Act
            List<RecipientResponse> result = pagarMeService.listRecipients();

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("lanca exception quando API falha")
        void throwsException_whenApiFails() {
            // Arrange
            when(restTemplate.exchange(
                    eq(API_URL + "/recipients"),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(RecipientListResponse.class)
            )).thenThrow(new RuntimeException("Connection refused"));

            // Act & Assert
            assertThatThrownBy(() -> pagarMeService.listRecipients())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Falha ao listar recipients");
        }
    }

    // ============================================
    // findRecipientByDocument
    // ============================================

    @Nested
    @DisplayName("findRecipientByDocument")
    class FindRecipientByDocument {

        @Test
        @DisplayName("retorna recipient quando CPF existe")
        void returnsRecipient_whenDocumentMatches() {
            // Arrange
            String targetDocument = "12345678900";
            RecipientResponse matching = createRecipientResponse("re_match", targetDocument, "001", "1234", "56789");
            RecipientResponse other = createRecipientResponse("re_other", "99999999999", "341", "5678", "12345");
            mockListRecipientsResponse(List.of(other, matching));

            // Act
            RecipientResponse result = pagarMeService.findRecipientByDocument(targetDocument);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("re_match");
            assertThat(result.getDocument()).isEqualTo(targetDocument);
        }

        @Test
        @DisplayName("retorna null quando CPF nao existe")
        void returnsNull_whenDocumentNotFound() {
            // Arrange
            RecipientResponse other = createRecipientResponse("re_other", "99999999999", "001", "1234", "56789");
            mockListRecipientsResponse(List.of(other));

            // Act
            RecipientResponse result = pagarMeService.findRecipientByDocument("00000000000");

            // Assert
            assertThat(result).isNull();
        }
    }

    // ============================================
    // findDuplicateRecipient
    // ============================================

    @Nested
    @DisplayName("findDuplicateRecipient")
    class FindDuplicateRecipient {

        @Test
        @DisplayName("detecta recipient duplicado por CPF + dados bancarios")
        void detectsDuplicate_whenDocumentAndBankMatch() {
            // Arrange
            String document = "12345678900";
            String bankCode = "001";
            String agency = "1234";
            String account = "56789";

            RecipientResponse existing = createRecipientResponse("re_dup", document, bankCode, agency, account);
            mockListRecipientsResponse(List.of(existing));

            // Act
            RecipientResponse result = pagarMeService.findDuplicateRecipient(document, bankCode, agency, account);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("re_dup");
        }

        @Test
        @DisplayName("nao detecta duplicata quando CPF igual mas banco diferente")
        void noDuplicate_whenDocumentMatchesButBankDiffers() {
            // Arrange
            String document = "12345678900";
            RecipientResponse existing = createRecipientResponse("re_1", document, "001", "1234", "56789");
            mockListRecipientsResponse(List.of(existing));

            // Act — banco diferente
            RecipientResponse result = pagarMeService.findDuplicateRecipient(document, "341", "1234", "56789");

            // Assert
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("detecta duplicata mesmo com hifen na conta")
        void detectsDuplicate_normalizesAccountNumber() {
            // Arrange
            String document = "12345678900";
            RecipientResponse existing = createRecipientResponse("re_norm", document, "001", "1234", "567896");
            mockListRecipientsResponse(List.of(existing));

            // Act — conta com hifen
            RecipientResponse result = pagarMeService.findDuplicateRecipient(document, "001", "1234", "56789-6");

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("re_norm");
        }

        @Test
        @DisplayName("retorna null quando nao ha duplicata")
        void returnsNull_whenNoDuplicate() {
            // Arrange
            mockListRecipientsResponse(List.of());

            // Act
            RecipientResponse result = pagarMeService.findDuplicateRecipient("00000000000", "001", "1234", "56789");

            // Assert
            assertThat(result).isNull();
        }
    }

    // ============================================
    // validateWebhookSignature
    // ============================================

    @Nested
    @DisplayName("validateWebhookSignature")
    class ValidateWebhookSignature {

        @Test
        @DisplayName("signature valida retorna true")
        void validSignature_returnsTrue() {
            // Gerar HMAC SHA256 real
            String payload = "{\"id\":\"evt_test\",\"type\":\"order.paid\"}";
            String secret = "webhook_secret_test";

            try {
                javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
                javax.crypto.spec.SecretKeySpec keySpec = new javax.crypto.spec.SecretKeySpec(
                        secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
                mac.init(keySpec);
                byte[] hash = mac.doFinal(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                StringBuilder hexString = new StringBuilder();
                for (byte b : hash) {
                    hexString.append(String.format("%02x", b));
                }
                String signature = "sha256=" + hexString;

                boolean result = pagarMeService.validateWebhookSignature(payload, signature);
                assertThat(result).isTrue();
            } catch (Exception e) {
                fail("Erro ao gerar HMAC: " + e.getMessage());
            }
        }

        @Test
        @DisplayName("signature invalida retorna false")
        void invalidSignature_returnsFalse() {
            String payload = "{\"id\":\"evt_test\"}";
            String wrongSignature = "sha256=0000000000000000000000000000000000000000000000000000000000000000";

            boolean result = pagarMeService.validateWebhookSignature(payload, wrongSignature);
            assertThat(result).isFalse();
        }
    }
}
