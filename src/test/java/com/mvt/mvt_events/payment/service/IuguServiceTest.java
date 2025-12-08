package com.mvt.mvt_events.payment.service;

import com.mvt.mvt_events.config.IuguConfig;
import com.mvt.mvt_events.jpa.BankAccount;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.payment.dto.InvoiceResponse;
import com.mvt.mvt_events.payment.dto.SubAccountResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para IuguService
 * 
 * Usa Mockito para mockar RestTemplate e testar:
 * - Criação de subcontas
 * - Criação de invoices com split
 * - Validação de webhooks
 * - Tratamento de erros da API
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IuguService - Testes Unitários")
class IuguServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private IuguConfig iuguConfig;

    @Mock
    private IuguConfig.ApiConfig apiConfig;

    @Mock
    private IuguConfig.SplitConfig splitConfig;

    @Mock
    private IuguConfig.WebhookConfig webhookConfig;

    private IuguService iuguService;

    @BeforeEach
    void setUp() {
        // Mock das configurações
        when(iuguConfig.getApi()).thenReturn(apiConfig);
        when(iuguConfig.getSplit()).thenReturn(splitConfig);
        when(iuguConfig.getWebhook()).thenReturn(webhookConfig);

        when(apiConfig.getKey()).thenReturn("test_api_key");
        when(apiConfig.getUrl()).thenReturn("https://api.iugu.com/v1");
        when(apiConfig.getId()).thenReturn("master_account_id");

        when(splitConfig.getMotoboyPercentage()).thenReturn(BigDecimal.valueOf(87.0));
        when(splitConfig.getManagerPercentage()).thenReturn(BigDecimal.valueOf(5.0));
        when(splitConfig.getPlatformPercentage()).thenReturn(BigDecimal.valueOf(8.0));
        when(splitConfig.getTransactionFee()).thenReturn(BigDecimal.valueOf(0.59));

        when(webhookConfig.getToken()).thenReturn("webhook_secret_token");

        iuguService = new IuguService(restTemplate, iuguConfig);
    }

    // ========================================
    // createSubAccount Tests
    // ========================================

    @Test
    @DisplayName("Deve criar subconta com sucesso")
    void shouldCreateSubAccountSuccessfully() {
        // Given
        User user = new User();
        user.setUsername("joao_motoboy");
        user.setName("João da Silva");

        BankAccount bankAccount = new BankAccount();
        bankAccount.setBankCode("260");
        bankAccount.setBankName("Nubank");
        bankAccount.setAgency("0001");
        bankAccount.setAccountNumber("12345678-9");
        bankAccount.setAccountType(BankAccount.AccountType.CHECKING);
        bankAccount.markAsActive();

        // Mock response
        Map<String, Object> mockResponse = Map.of(
                "account_id", "acc_new_123",
                "name", "João da Silva",
                "email", "joao_motoboy@mvt.com",
                "is_active", true,
                "auto_withdraw", true,
                "verification_status", "pending"
        );

        when(restTemplate.postForEntity(
                anyString(),
                any(),
                eq(Map.class)
        )).thenReturn(ResponseEntity.ok(mockResponse));

        // When
        SubAccountResponse response = iuguService.createSubAccount(user, bankAccount);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.accountId()).isEqualTo("acc_new_123");
        assertThat(response.name()).isEqualTo("João da Silva");
        assertThat(response.isActive()).isTrue();
        assertThat(response.verificationStatus()).isEqualTo("pending");

        verify(restTemplate).postForEntity(
                contains("/marketplace/create_account"),
                any(),
                eq(Map.class)
        );
    }

    @Test
    @DisplayName("Deve lançar exceção quando usuário sem username")
    void shouldThrowExceptionWhenUserHasNoUsername() {
        // Given
        User user = new User();
        user.setUsername(null); // Sem username

        BankAccount bankAccount = new BankAccount();
        bankAccount.markAsActive();

        // When/Then
        assertThatThrownBy(() -> iuguService.createSubAccount(user, bankAccount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("username");
    }

    @Test
    @DisplayName("Deve lançar exceção quando BankAccount não está ativa")
    void shouldThrowExceptionWhenBankAccountNotActive() {
        // Given
        User user = new User();
        user.setUsername("joao_motoboy");

        BankAccount bankAccount = new BankAccount();
        bankAccount.setStatus(BankAccount.BankAccountStatus.PENDING_VALIDATION);

        // When/Then
        assertThatThrownBy(() -> iuguService.createSubAccount(user, bankAccount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ativa");
    }

    @Test
    @DisplayName("Deve tratar erro 401 da API Iugu")
    void shouldHandleUnauthorizedError() {
        // Given
        User user = new User();
        user.setUsername("joao_motoboy");

        BankAccount bankAccount = new BankAccount();
        bankAccount.markAsActive();
        bankAccount.setBankCode("260");
        bankAccount.setAgency("0001");
        bankAccount.setAccountNumber("12345678-9");
        bankAccount.setAccountType(BankAccount.AccountType.CHECKING);

        when(restTemplate.postForEntity(
                anyString(),
                any(),
                eq(Map.class)
        )).thenThrow(new HttpClientErrorException(
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                "{\"errors\":\"Unauthorized\"}".getBytes(),
                null
        ));

        // When/Then
        assertThatThrownBy(() -> iuguService.createSubAccount(user, bankAccount))
                .isInstanceOf(IuguService.IuguApiException.class)
                .hasMessageContaining("401");
    }

    // ========================================
    // createInvoiceWithSplit Tests
    // ========================================

    @Test
    @DisplayName("Deve criar invoice com split 87/5/8 com sucesso")
    void shouldCreateInvoiceWithSplitSuccessfully() {
        // Given
        String deliveryId = UUID.randomUUID().toString();
        BigDecimal amount = BigDecimal.valueOf(100.00);
        String clientEmail = "cliente@test.com";
        String motoboyAccountId = "acc_motoboy_123";
        String managerAccountId = "acc_manager_456";

        // Mock response
        Map<String, Object> mockResponse = Map.of(
                "id", "inv_new_789",
                "secure_url", "https://secure.iugu.com/...",
                "status", "pending",
                "total_cents", 10000,
                "pix", Map.of(
                        "qrcode", "00020101021243650016COM.MERCADOPAGO...",
                        "qrcode_url", "https://qr.iugu.com/..."
                )
        );

        when(restTemplate.postForEntity(
                anyString(),
                any(),
                eq(Map.class)
        )).thenReturn(ResponseEntity.ok(mockResponse));

        // When
        InvoiceResponse response = iuguService.createInvoiceWithSplit(
                deliveryId,
                amount,
                clientEmail,
                motoboyAccountId,
                managerAccountId
        );

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo("inv_new_789");
        assertThat(response.status()).isEqualTo("pending");
        assertThat(response.isPending()).isTrue();

        verify(restTemplate).postForEntity(
                contains("/invoices"),
                any(),
                eq(Map.class)
        );
    }

    @Test
    @DisplayName("Deve rejeitar valor menor que R$ 1,00")
    void shouldRejectAmountBelowMinimum() {
        // Given
        BigDecimal amount = BigDecimal.valueOf(0.50); // Inválido

        // When/Then
        assertThatThrownBy(() -> iuguService.createInvoiceWithSplit(
                "delivery_123",
                amount,
                "cliente@test.com",
                "acc_motoboy",
                "acc_manager"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("R$ 1,00");
    }

    @Test
    @DisplayName("Deve rejeitar quando motoboy sem account ID")
    void shouldRejectWhenMotoboyHasNoAccountId() {
        // Given
        String motoboyAccountId = null; // Inválido

        // When/Then
        assertThatThrownBy(() -> iuguService.createInvoiceWithSplit(
                "delivery_123",
                BigDecimal.valueOf(100.00),
                "cliente@test.com",
                motoboyAccountId,
                "acc_manager"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("motoboy");
    }

    // ========================================
    // getSubAccountStatus Tests
    // ========================================

    @Test
    @DisplayName("Deve consultar status de subconta com sucesso")
    void shouldGetSubAccountStatusSuccessfully() {
        // Given
        String accountId = "acc_123";

        Map<String, Object> mockResponse = Map.of(
                "account_id", accountId,
                "name", "João Motoboy",
                "email", "joao@test.com",
                "is_active", true,
                "auto_withdraw", true,
                "verification_status", "verified"
        );

        when(restTemplate.getForEntity(
                contains("/accounts/" + accountId),
                eq(Map.class)
        )).thenReturn(ResponseEntity.ok(mockResponse));

        // When
        SubAccountResponse response = iuguService.getSubAccountStatus(accountId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.accountId()).isEqualTo(accountId);
        assertThat(response.verificationStatus()).isEqualTo("verified");
        assertThat(response.canReceivePayments()).isTrue();

        verify(restTemplate).getForEntity(
                contains("/accounts/" + accountId),
                eq(Map.class)
        );
    }

    // ========================================
    // validateWebhookSignature Tests
    // ========================================

    @Test
    @DisplayName("Deve validar assinatura válida do webhook")
    void shouldValidateValidWebhookSignature() {
        // Given
        String validSignature = "webhook_secret_token";

        // When
        boolean isValid = iuguService.validateWebhookSignature(validSignature);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Deve rejeitar assinatura inválida do webhook")
    void shouldRejectInvalidWebhookSignature() {
        // Given
        String invalidSignature = "wrong_token";

        // When
        boolean isValid = iuguService.validateWebhookSignature(invalidSignature);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Deve rejeitar assinatura nula")
    void shouldRejectNullSignature() {
        // When
        boolean isValid = iuguService.validateWebhookSignature(null);

        // Then
        assertThat(isValid).isFalse();
    }
}
