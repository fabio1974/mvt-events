package com.mvt.mvt_events.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes unitários para IuguConfig
 * 
 * Valida:
 * - Configurações obrigatórias (@NotBlank)
 * - Validação de percentuais (soma = 100%)
 * - Limites de valores (@DecimalMin, @DecimalMax)
 */
@DisplayName("IuguConfig - Testes de Validação")
class IuguConfigTest {

    private final Validator validator;

    public IuguConfigTest() {
        LocalValidatorFactoryBean validatorFactory = new LocalValidatorFactoryBean();
        validatorFactory.afterPropertiesSet();
        this.validator = validatorFactory;
    }

    @Test
    @DisplayName("Deve validar configuração válida com sucesso")
    void shouldValidateValidConfiguration() {
        // Given
        IuguConfig.SplitConfig splitConfig = new IuguConfig.SplitConfig();
        splitConfig.setMotoboyPercentage(BigDecimal.valueOf(87.0));
        splitConfig.setManagerPercentage(BigDecimal.valueOf(5.0));
        splitConfig.setPlatformPercentage(BigDecimal.valueOf(8.0));
        splitConfig.setTransactionFee(BigDecimal.valueOf(0.59));

        // When
        Set<ConstraintViolation<IuguConfig.SplitConfig>> violations = validator.validate(splitConfig);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Deve validar soma de percentuais = 100%")
    void shouldValidatePercentageSum() {
        // Given
        IuguConfig.SplitConfig splitConfig = new IuguConfig.SplitConfig();
        splitConfig.setMotoboyPercentage(BigDecimal.valueOf(87.0));
        splitConfig.setManagerPercentage(BigDecimal.valueOf(5.0));
        splitConfig.setPlatformPercentage(BigDecimal.valueOf(8.0));
        splitConfig.setTransactionFee(BigDecimal.valueOf(0.59));

        // When/Then
        assertThatNoException().isThrownBy(() -> splitConfig.validatePercentages());
    }

    @Test
    @DisplayName("Deve rejeitar soma de percentuais != 100%")
    void shouldRejectInvalidPercentageSum() {
        // Given - soma = 95% (inválido)
        IuguConfig.SplitConfig splitConfig = new IuguConfig.SplitConfig();
        splitConfig.setMotoboyPercentage(BigDecimal.valueOf(85.0));
        splitConfig.setManagerPercentage(BigDecimal.valueOf(5.0));
        splitConfig.setPlatformPercentage(BigDecimal.valueOf(5.0));
        splitConfig.setTransactionFee(BigDecimal.valueOf(0.59));

        // When/Then
        assertThatThrownBy(() -> splitConfig.validatePercentages())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("A soma dos percentuais deve ser 100%");
    }

    @Test
    @DisplayName("Deve rejeitar percentual negativo")
    void shouldRejectNegativePercentage() {
        // Given
        IuguConfig.SplitConfig splitConfig = new IuguConfig.SplitConfig();
        splitConfig.setMotoboyPercentage(BigDecimal.valueOf(-10.0)); // Inválido
        splitConfig.setManagerPercentage(BigDecimal.valueOf(5.0));
        splitConfig.setPlatformPercentage(BigDecimal.valueOf(105.0));
        splitConfig.setTransactionFee(BigDecimal.valueOf(0.59));

        // When
        Set<ConstraintViolation<IuguConfig.SplitConfig>> violations = validator.validate(splitConfig);

        // Then
        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.getPropertyPath().toString().equals("motoboyPercentage"));
    }

    @Test
    @DisplayName("Deve rejeitar percentual > 100%")
    void shouldRejectPercentageOver100() {
        // Given
        IuguConfig.SplitConfig splitConfig = new IuguConfig.SplitConfig();
        splitConfig.setMotoboyPercentage(BigDecimal.valueOf(150.0)); // Inválido
        splitConfig.setManagerPercentage(BigDecimal.valueOf(5.0));
        splitConfig.setPlatformPercentage(BigDecimal.valueOf(8.0));
        splitConfig.setTransactionFee(BigDecimal.valueOf(0.59));

        // When
        Set<ConstraintViolation<IuguConfig.SplitConfig>> violations = validator.validate(splitConfig);

        // Then
        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.getPropertyPath().toString().equals("motoboyPercentage"));
    }

    @Test
    @DisplayName("Deve validar ApiConfig com campos obrigatórios")
    void shouldValidateApiConfigRequiredFields() {
        // Given
        IuguConfig.ApiConfig apiConfig = new IuguConfig.ApiConfig();
        apiConfig.setKey("test_api_key_123");
        apiConfig.setUrl("https://api.iugu.com/v1");
        apiConfig.setId("master_account_id");

        // When
        Set<ConstraintViolation<IuguConfig.ApiConfig>> violations = validator.validate(apiConfig);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Deve rejeitar ApiConfig sem API Key")
    void shouldRejectApiConfigWithoutKey() {
        // Given
        IuguConfig.ApiConfig apiConfig = new IuguConfig.ApiConfig();
        apiConfig.setKey(""); // Vazio - inválido
        apiConfig.setUrl("https://api.iugu.com/v1");
        apiConfig.setId("master_account_id");

        // When
        Set<ConstraintViolation<IuguConfig.ApiConfig>> violations = validator.validate(apiConfig);

        // Then
        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.getPropertyPath().toString().equals("key"));
    }

    @Test
    @DisplayName("Deve validar PaymentConfig com threshold válido")
    void shouldValidatePaymentConfigThreshold() {
        // Given
        IuguConfig.PaymentConfig paymentConfig = new IuguConfig.PaymentConfig();
        paymentConfig.setThreshold(BigDecimal.valueOf(100.00));

        // When
        Set<ConstraintViolation<IuguConfig.PaymentConfig>> violations = validator.validate(paymentConfig);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Deve rejeitar threshold negativo")
    void shouldRejectNegativeThreshold() {
        // Given
        IuguConfig.PaymentConfig paymentConfig = new IuguConfig.PaymentConfig();
        paymentConfig.setThreshold(BigDecimal.valueOf(-50.00)); // Inválido

        // When
        Set<ConstraintViolation<IuguConfig.PaymentConfig>> violations = validator.validate(paymentConfig);

        // Then
        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.getPropertyPath().toString().equals("threshold"));
    }

    @Test
    @DisplayName("Deve validar AutoWithdrawConfig com delay válido")
    void shouldValidateAutoWithdrawConfig() {
        // Given
        IuguConfig.AutoWithdrawConfig autoWithdrawConfig = new IuguConfig.AutoWithdrawConfig();
        autoWithdrawConfig.setEnabled(true);
        autoWithdrawConfig.setDelayDays(1);

        // When
        Set<ConstraintViolation<IuguConfig.AutoWithdrawConfig>> violations = validator.validate(autoWithdrawConfig);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Deve rejeitar delay negativo")
    void shouldRejectNegativeDelay() {
        // Given
        IuguConfig.AutoWithdrawConfig autoWithdrawConfig = new IuguConfig.AutoWithdrawConfig();
        autoWithdrawConfig.setEnabled(true);
        autoWithdrawConfig.setDelayDays(-1); // Inválido

        // When
        Set<ConstraintViolation<IuguConfig.AutoWithdrawConfig>> violations = validator.validate(autoWithdrawConfig);

        // Then
        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.getPropertyPath().toString().equals("delayDays"));
    }

    @Test
    @DisplayName("Deve validar RetryConfig com valores válidos")
    void shouldValidateRetryConfig() {
        // Given
        IuguConfig.RetryConfig retryConfig = new IuguConfig.RetryConfig();
        retryConfig.setMaxAttempts(3);
        retryConfig.setInitialBackoffMs(1000L);

        // When
        Set<ConstraintViolation<IuguConfig.RetryConfig>> violations = validator.validate(retryConfig);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Deve rejeitar maxAttempts = 0")
    void shouldRejectZeroMaxAttempts() {
        // Given
        IuguConfig.RetryConfig retryConfig = new IuguConfig.RetryConfig();
        retryConfig.setMaxAttempts(0); // Inválido
        retryConfig.setInitialBackoffMs(1000L);

        // When
        Set<ConstraintViolation<IuguConfig.RetryConfig>> violations = validator.validate(retryConfig);

        // Then
        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.getPropertyPath().toString().equals("maxAttempts"));
    }

    @Test
    @DisplayName("Deve aceitar split válido 87/5/8")
    void shouldAcceptValidSplit() {
        // Given - Split padrão do sistema
        IuguConfig.SplitConfig splitConfig = new IuguConfig.SplitConfig();
        splitConfig.setMotoboyPercentage(BigDecimal.valueOf(87.0));
        splitConfig.setManagerPercentage(BigDecimal.valueOf(5.0));
        splitConfig.setPlatformPercentage(BigDecimal.valueOf(8.0));
        splitConfig.setTransactionFee(BigDecimal.valueOf(0.59));

        // When
        BigDecimal sum = splitConfig.getMotoboyPercentage()
                .add(splitConfig.getManagerPercentage())
                .add(splitConfig.getPlatformPercentage());

        // Then
        assertThat(sum).isEqualByComparingTo(BigDecimal.valueOf(100.0));
        assertThatNoException().isThrownBy(() -> splitConfig.validatePercentages());
    }

    @Test
    @DisplayName("Deve calcular corretamente os valores de split para R$ 100,00")
    void shouldCalculateSplitValuesCorrectly() {
        // Given
        IuguConfig.SplitConfig splitConfig = new IuguConfig.SplitConfig();
        splitConfig.setMotoboyPercentage(BigDecimal.valueOf(87.0));
        splitConfig.setManagerPercentage(BigDecimal.valueOf(5.0));
        splitConfig.setPlatformPercentage(BigDecimal.valueOf(8.0));
        splitConfig.setTransactionFee(BigDecimal.valueOf(0.59));

        BigDecimal totalAmount = BigDecimal.valueOf(100.00);

        // When - Calcula valores
        BigDecimal motoboyAmount = totalAmount
                .multiply(splitConfig.getMotoboyPercentage())
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        
        BigDecimal managerAmount = totalAmount
                .multiply(splitConfig.getManagerPercentage())
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        
        BigDecimal platformAmount = totalAmount
                .multiply(splitConfig.getPlatformPercentage())
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);

        // Then
        assertThat(motoboyAmount).isEqualByComparingTo(BigDecimal.valueOf(87.00));
        assertThat(managerAmount).isEqualByComparingTo(BigDecimal.valueOf(5.00));
        assertThat(platformAmount).isEqualByComparingTo(BigDecimal.valueOf(8.00));
        
        // Soma deve ser igual ao total
        BigDecimal sum = motoboyAmount.add(managerAmount).add(platformAmount);
        assertThat(sum).isEqualByComparingTo(totalAmount);
    }
}
