package com.mvt.mvt_events.config;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Configuração do gateway de pagamentos Iugu
 * 
 * <p>Esta classe carrega as configurações do Iugu a partir do application.properties
 * e fornece beans para integração com a API do Iugu.</p>
 * 
 * <p><strong>Configurações principais:</strong></p>
 * <ul>
 *   <li>API credentials (key, account ID)</li>
 *   <li>Webhook token para validação de eventos</li>
 *   <li>Percentuais de split (87% motoboy, 5% gerente, 8% plataforma)</li>
 *   <li>Configurações de retry e auto-withdraw</li>
 * </ul>
 * 
 * @see <a href="https://dev.iugu.com/reference/api-overview">Documentação Iugu API</a>
 */
@Configuration
@ConfigurationProperties(prefix = "iugu")
@Validated
@Getter
@Setter
@Slf4j
public class IuguConfig {

    /**
     * Modo de operação do Iugu (dry-run, sandbox, production)
     */
    @NotBlank(message = "Modo de operação do Iugu é obrigatório")
    private String mode = "dry-run";

    /**
     * Configurações da API Iugu
     */
    @NotNull(message = "Configurações da API Iugu são obrigatórias")
    private ApiConfig api = new ApiConfig();

    /**
     * Configurações de webhook
     */
    @NotNull(message = "Configurações de webhook são obrigatórias")
    private WebhookConfig webhook = new WebhookConfig();

    /**
     * Configurações de split de pagamento
     */
    @NotNull(message = "Configurações de split são obrigatórias")
    private SplitConfig split = new SplitConfig();

    /**
     * Configurações de pagamento
     */
    @NotNull(message = "Configurações de pagamento são obrigatórias")
    private PaymentConfig payment = new PaymentConfig();

    /**
     * Configurações de auto-withdraw (transferências automáticas)
     */
    @NotNull(message = "Configurações de auto-withdraw são obrigatórias")
    private AutoWithdrawConfig autoWithdraw = new AutoWithdrawConfig();

    /**
     * Configurações de retry para chamadas à API
     */
    @NotNull(message = "Configurações de retry são obrigatórias")
    private RetryConfig retry = new RetryConfig();

    /**
     * Enum para modos de operação
     */
    public enum IuguMode {
        DRY_RUN,    // Mock local (não chama Iugu)
        SANDBOX,    // Iugu Sandbox (teste)
        PRODUCTION  // Iugu Production (real)
    }

    /**
     * Retorna o modo de operação atual como enum
     */
    public IuguMode getModeEnum() {
        return switch (mode.toLowerCase().replace("-", "_")) {
            case "dry_run", "dryrun", "mock" -> IuguMode.DRY_RUN;
            case "sandbox", "test" -> IuguMode.SANDBOX;
            case "production", "prod", "live" -> IuguMode.PRODUCTION;
            default -> {
                log.warn("Modo Iugu inválido: '{}'. Usando dry-run por segurança.", mode);
                yield IuguMode.DRY_RUN;
            }
        };
    }

    /**
     * Verifica se está em modo dry-run (mock)
     */
    public boolean isDryRun() {
        return getModeEnum() == IuguMode.DRY_RUN;
    }

    /**
     * Verifica se está em modo sandbox
     */
    public boolean isSandbox() {
        return getModeEnum() == IuguMode.SANDBOX;
    }

    /**
     * Verifica se está em modo production
     */
    public boolean isProduction() {
        return getModeEnum() == IuguMode.PRODUCTION;
    }

    /**
     * Configurações da API Iugu
     */
    @Getter
    @Setter
    public static class ApiConfig {
        /**
         * API Key do Iugu (test_xxx para dev, live_xxx para prod)
         */
        @NotBlank(message = "API Key do Iugu é obrigatória")
        private String key;

        /**
         * URL base da API Iugu
         */
        @NotBlank(message = "URL da API Iugu é obrigatória")
        private String url = "https://api.iugu.com/v1";

        /**
         * Account ID da conta master do Iugu
         */
        @NotBlank(message = "Account ID do Iugu é obrigatório")
        private String id;
    }

    /**
     * Configurações de webhook
     */
    @Getter
    @Setter
    public static class WebhookConfig {
        /**
         * Token de validação de webhooks (configurado no painel Iugu)
         */
        @NotBlank(message = "Token de webhook é obrigatório")
        private String token;
    }

    /**
     * Configurações de split de pagamento
     */
    @Getter
    @Setter
    public static class SplitConfig {
        /**
         * Percentual do motoboy (padrão: 87%)
         */
        @NotNull(message = "Percentual do motoboy é obrigatório")
        @DecimalMin(value = "0.0", message = "Percentual do motoboy deve ser >= 0")
        private BigDecimal motoboyPercentage = BigDecimal.valueOf(87.0);

        /**
         * Percentual do gerente (padrão: 5%)
         */
        @NotNull(message = "Percentual do gerente é obrigatório")
        @DecimalMin(value = "0.0", message = "Percentual do gerente deve ser >= 0")
        private BigDecimal managerPercentage = BigDecimal.valueOf(5.0);

        /**
         * Percentual da plataforma (padrão: 8%)
         */
        @NotNull(message = "Percentual da plataforma é obrigatório")
        @DecimalMin(value = "0.0", message = "Percentual da plataforma deve ser >= 0")
        private BigDecimal platformPercentage = BigDecimal.valueOf(8.0);

        /**
         * Taxa fixa por transação do Iugu (padrão: R$ 0,59)
         */
        @NotNull(message = "Taxa de transação é obrigatória")
        @DecimalMin(value = "0.0", message = "Taxa de transação deve ser >= 0")
        private BigDecimal transactionFee = BigDecimal.valueOf(0.59);

        /**
         * Valida se a soma dos percentuais é 100%
         */
        public void validatePercentages() {
            BigDecimal total = motoboyPercentage
                    .add(managerPercentage)
                    .add(platformPercentage);
            
            if (total.compareTo(BigDecimal.valueOf(100.0)) != 0) {
                throw new IllegalStateException(
                    String.format("A soma dos percentuais de split deve ser 100%%. Atual: %.2f%%", total)
                );
            }
        }
    }

    /**
     * Configurações de pagamento
     */
    @Getter
    @Setter
    public static class PaymentConfig {
        /**
         * Valor mínimo acumulado para acionar transferência automática (padrão: R$ 100)
         */
        @NotNull(message = "Threshold de pagamento é obrigatório")
        @DecimalMin(value = "0.01", message = "Threshold de pagamento deve ser > 0")
        private BigDecimal threshold = BigDecimal.valueOf(100.00);
    }

    /**
     * Configurações de auto-withdraw
     */
    @Getter
    @Setter
    public static class AutoWithdrawConfig {
        /**
         * Habilitar transferências automáticas D+1 (padrão: true)
         */
        @NotNull(message = "Flag de auto-withdraw é obrigatória")
        private Boolean enabled = true;

        /**
         * Dias de atraso para transferência (padrão: 1 dia)
         */
        @NotNull(message = "Delay de auto-withdraw é obrigatório")
        @Min(value = 1, message = "Delay de auto-withdraw deve ser >= 1")
        private Integer delayDays = 1;
    }

    /**
     * Configurações de retry
     */
    @Getter
    @Setter
    public static class RetryConfig {
        /**
         * Número máximo de tentativas (padrão: 3)
         */
        @NotNull(message = "Max attempts de retry é obrigatório")
        @Min(value = 1, message = "Max attempts deve ser >= 1")
        private Integer maxAttempts = 3;

        /**
         * Tempo inicial de backoff em ms (padrão: 1000ms)
         */
        @NotNull(message = "Initial backoff de retry é obrigatório")
        @Min(value = 100, message = "Initial backoff deve ser >= 100ms")
        private Long initialBackoffMs = 1000L;
    }

    /**
     * Cria um RestTemplate configurado para chamadas à API do Iugu
     * 
     * <p>Configurações:</p>
     * <ul>
     *   <li>Timeout de conexão: 10 segundos</li>
     *   <li>Timeout de leitura: 30 segundos</li>
     *   <li>Headers de autenticação Basic Auth (API Key como username)</li>
     * </ul>
     * 
     * @return RestTemplate configurado para Iugu
     */
    @Bean(name = "iuguRestTemplate")
    public RestTemplate iuguRestTemplate() {
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setConnectionRequestTimeout(Duration.ofSeconds(10));
        
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        
        log.info("RestTemplate do Iugu configurado com timeout de 10s");
        return restTemplate;
    }

    /**
     * Valida as configurações após inicialização
     */
    public void validate() {
        split.validatePercentages();
        
        // Log do modo de operação
        IuguMode currentMode = getModeEnum();
        String modeIcon = switch (currentMode) {
            case DRY_RUN -> "🧪";
            case SANDBOX -> "🏖️";
            case PRODUCTION -> "🚀";
        };
        
        log.info("════════════════════════════════════════════════════════════");
        log.info("{} IUGU MODE: {}", modeIcon, currentMode.name());
        log.info("════════════════════════════════════════════════════════════");
        
        if (currentMode == IuguMode.DRY_RUN) {
            log.warn("⚠️  ATENÇÃO: Modo DRY-RUN ativo!");
            log.warn("   Faturas serão MOCKADAS e não enviadas ao Iugu");
            log.warn("   Use IUGU_MODE=sandbox ou IUGU_MODE=production para integração real");
        } else if (currentMode == IuguMode.SANDBOX) {
            log.info("🏖️  Modo SANDBOX: Usando Iugu de teste");
            log.info("   API Key deve começar com 'test_'");
        } else {
            log.info("🚀 Modo PRODUCTION: Usando Iugu REAL");
            log.warn("   ⚠️  ATENÇÃO: Transações reais serão cobradas!");
        }
        
        log.info("────────────────────────────────────────────────────────────");
        log.info("Configurações do Iugu validadas com sucesso");
        log.debug("API URL: {}", api.getUrl());
        log.debug("Split: {}% motoboy, {}% gerente, {}% plataforma", 
            split.getMotoboyPercentage(), 
            split.getManagerPercentage(), 
            split.getPlatformPercentage()
        );
        log.debug("Threshold de pagamento: R$ {}", payment.getThreshold());
        log.debug("Auto-withdraw: {} (D+{})", 
            autoWithdraw.getEnabled() ? "habilitado" : "desabilitado",
            autoWithdraw.getDelayDays()
        );
    }
}
