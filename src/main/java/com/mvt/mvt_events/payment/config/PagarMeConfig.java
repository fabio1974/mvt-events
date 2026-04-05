package com.mvt.mvt_events.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.PostConstruct;

/**
 * Configurações do gateway de pagamento Pagar.me
 * 
 * Carrega as configurações do application.properties com prefixo "pagarme"
 * 
 * @see <a href="https://docs.pagar.me/reference/API-overview">Documentação API Pagar.me</a>
 */
@Configuration
@ConfigurationProperties(prefix = "pagarme")
@Getter
@Setter
public class PagarMeConfig {

    private static final Logger logger = LoggerFactory.getLogger(PagarMeConfig.class);

    /**
     * Configurações da API
     */
    private Api api = new Api();

    /**
     * Configurações de webhook
     */
    private Webhook webhook = new Webhook();

    /**
     * Configurações de split de pagamento
     */
    private Split split = new Split();

    /**
     * Configurações de retry
     */
    private Retry retry = new Retry();

    @PostConstruct
    public void init() {
        String apiKey = api.getKey();
        if (apiKey != null) {
            // Mascarar a chave para segurança, mostrando apenas os últimos 8 caracteres
            String maskedKey = "***" + apiKey.substring(Math.max(0, apiKey.length() - 8));
            boolean isProduction = apiKey.startsWith("sk_");
            boolean isSandbox = apiKey.startsWith("sk_test_");
            
            logger.info("🔐 Pagar.me Configuration Loaded:");
            logger.info("   API Key: {} ({})", maskedKey, isSandbox ? "SANDBOX" : isProduction ? "PRODUCTION" : "UNKNOWN");
            logger.info("   API URL: {}", api.getUrl());
        } else {
            logger.warn("⚠️  Pagar.me API Key not configured!");
        }
    }

    @Getter
    @Setter
    public static class Api {
        /**
         * URL base da API Pagar.me
         * Exemplo: https://api.pagar.me/core/v5
         */
        private String url;

        /**
         * API Key (Secret Key) do Pagar.me
         * Formato: sk_test_... (sandbox) ou sk_... (produção)
         */
        private String key;
    }

    @Getter
    @Setter
    public static class Webhook {
        /**
         * Secret usado para validar webhooks via HMAC SHA256
         */
        private String secret;
    }

    @Getter
    @Setter
    public static class Split {
        /**
         * Percentual do entregador (em centavos)
         * Exemplo: 8700 = 87%
         */
        private Integer courierPercentage;

        /**
         * Percentual do gerente (em centavos)
         * Exemplo: 500 = 5%
         */
        private Integer organizerPercentage;

        /**
         * Se o entregador é liable (responsável por chargebacks)
         */
        private Boolean courierLiable;

        /**
         * Se o entregador paga taxa de processamento
         */
        private Boolean courierChargeProcessingFee;

        /**
         * Se o gerente paga taxa de processamento
         */
        private Boolean organizerChargeProcessingFee;
    }

    @Getter
    @Setter
    public static class Retry {
        /**
         * Número máximo de tentativas em caso de falha
         */
        private Integer maxAttempts;

        /**
         * Backoff inicial em milissegundos
         */
        private Long initialBackoffMs;
    }
}

