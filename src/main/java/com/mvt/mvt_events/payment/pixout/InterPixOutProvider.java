package com.mvt.mvt_events.payment.pixout;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mvt.mvt_events.jpa.User;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Map;

/**
 * Implementação de {@link PixOutProvider} via Banco Inter Empresas (CDPJ API Banking).
 *
 * Fluxo:
 *  1. OAuth2 client_credentials com mTLS → access_token (validade 60min, cacheado)
 *  2. POST /banking/v2/pix com destinatário tipo CHAVE → codigoSolicitacao
 *
 * Ativa via `pix.out.provider=inter`. Requer certificado digital (.p12/.pfx)
 * emitido pelo Inter + client_id/client_secret da integração criada em
 * contadigital.inter.co → Integrar → Nova Integração → API Banking.
 *
 * Se o Inter entregou cert em PEM (.crt + .key), converta para P12:
 *   openssl pkcs12 -export -in inter.crt -inkey inter.key -out inter.p12 -passout pass:SENHA
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "pix.out.provider", havingValue = "inter")
public class InterPixOutProvider implements PixOutProvider {

    @Value("${pix.out.inter.environment:sandbox}")
    private String environment;

    @Value("${pix.out.inter.client-id:}")
    private String clientId;

    @Value("${pix.out.inter.client-secret:}")
    private String clientSecret;

    @Value("${pix.out.inter.pkcs12-path:}")
    private String pkcs12Path;

    @Value("${pix.out.inter.pkcs12-password:}")
    private String pkcs12Password;

    @Value("${pix.out.inter.account:}")
    private String contaCorrente;

    private String baseUrl;
    private CloseableHttpClient http;
    private final ObjectMapper json = new ObjectMapper();

    private volatile String cachedToken;
    private volatile long tokenExpiresAtEpochMs;

    @PostConstruct
    void init() {
        this.baseUrl = "production".equalsIgnoreCase(environment)
                ? "https://cdpj.partners.bancointer.com.br"
                : "https://cdpj-sandbox.partners.uatinter.co";

        if (clientId.isBlank() || clientSecret.isBlank() || pkcs12Path.isBlank()) {
            log.warn("⚠️ Inter provider ativo mas credenciais incompletas — todas as chamadas vão falhar");
            return;
        }

        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (FileInputStream fis = new FileInputStream(pkcs12Path)) {
                keyStore.load(fis, pkcs12Password.toCharArray());
            }

            SSLContext sslContext = org.apache.hc.core5.ssl.SSLContexts.custom()
                    .loadKeyMaterial(keyStore, pkcs12Password.toCharArray())
                    .build();

            PoolingHttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
                    .setSSLSocketFactory(SSLConnectionSocketFactoryBuilder.create()
                            .setSslContext(sslContext)
                            .build())
                    .build();

            RequestConfig rc = RequestConfig.custom()
                    .setConnectTimeout(Timeout.ofSeconds(10))
                    .setResponseTimeout(Timeout.ofSeconds(30))
                    .build();

            this.http = HttpClients.custom()
                    .setConnectionManager(cm)
                    .setDefaultRequestConfig(rc)
                    .build();

            log.info("✅ InterPixOutProvider inicializado (env={}, baseUrl={}, conta={})",
                    environment, baseUrl, contaCorrente.isBlank() ? "(default)" : contaCorrente);
        } catch (Exception e) {
            log.error("❌ Falha ao inicializar Inter HTTP client (mTLS): {}", e.getMessage(), e);
            this.http = null;
        }
    }

    @Override
    public PixOutResult send(User to, long amountCents, String externalId) {
        if (http == null) {
            return PixOutResult.failed("Inter não configurado (credenciais/certificado ausentes)");
        }
        if (to == null || to.getPixKey() == null || to.getPixKey().isBlank()) {
            return PixOutResult.failed("Destinatário sem chave PIX");
        }
        if (amountCents <= 0) {
            return PixOutResult.failed("Valor inválido");
        }

        String token;
        try {
            token = getAccessToken();
        } catch (Exception e) {
            log.error("❌ Falha no OAuth2 Inter: {}", e.getMessage(), e);
            return PixOutResult.failed("Inter OAuth2 falhou: " + e.getMessage());
        }

        String formattedKey = StarkBankPixOutProvider.formatPixKey(to.getPixKey(), to.getPixKeyType());
        String valor = BigDecimal.valueOf(amountCents).movePointLeft(2).toPlainString();

        try {
            String body = json.writeValueAsString(Map.of(
                    "valor", valor,
                    "descricao", truncate("Zapi " + externalId, 140),
                    "destinatario", Map.of(
                            "tipo", "CHAVE",
                            "chave", formattedKey
                    )
            ));

            HttpPost post = new HttpPost(baseUrl + "/banking/v2/pix");
            post.setHeader("Authorization", "Bearer " + token);
            post.setHeader("Content-Type", "application/json");
            if (!contaCorrente.isBlank()) {
                post.setHeader("x-conta-corrente", contaCorrente);
            }
            post.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));

            return http.execute(post, resp -> {
                int status = resp.getCode();
                String respBody = resp.getEntity() == null ? "" : EntityUtils.toString(resp.getEntity());
                if (status < 200 || status >= 300) {
                    log.error("❌ Inter PIX falhou status={} body={} externalId={}", status, respBody, externalId);
                    return PixOutResult.failed("Inter HTTP " + status + ": " + truncate(respBody, 200));
                }
                JsonNode node = json.readTree(respBody);
                String codigoSolicitacao = node.path("codigoSolicitacao").asText(null);
                String tipoRetorno = node.path("tipoRetorno").asText("");
                if (codigoSolicitacao == null || codigoSolicitacao.isBlank()) {
                    return PixOutResult.failed("Inter resposta sem codigoSolicitacao: " + truncate(respBody, 200));
                }
                log.info("✅ Inter PIX aceito: codigoSolicitacao={} tipoRetorno={} externalId={}",
                        codigoSolicitacao, tipoRetorno, externalId);
                // tipoRetorno=EXECUCAO → processa direto; APROVACAO → aguarda aprovação manual
                // Em ambos casos, status final vem assíncrono (consultar /banking/v2/pix/{id})
                return PixOutResult.pending(codigoSolicitacao);
            });
        } catch (Exception e) {
            log.error("❌ Erro no POST Inter /banking/v2/pix (externalId={}): {}", externalId, e.getMessage(), e);
            return PixOutResult.failed("Inter erro: " + e.getMessage());
        }
    }

    private synchronized String getAccessToken() throws Exception {
        long now = System.currentTimeMillis();
        if (cachedToken != null && now < tokenExpiresAtEpochMs - 60_000) {
            return cachedToken;
        }
        String form = "client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                + "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8)
                + "&grant_type=client_credentials"
                + "&scope=" + URLEncoder.encode(
                        "pagamento-pix.write pagamento-pix.read extrato.read", StandardCharsets.UTF_8);

        HttpPost post = new HttpPost(baseUrl + "/oauth/v2/token");
        post.setHeader("Content-Type", "application/x-www-form-urlencoded");
        post.setEntity(new StringEntity(form, ContentType.APPLICATION_FORM_URLENCODED));

        return http.execute(post, resp -> {
            int status = resp.getCode();
            String respBody = resp.getEntity() == null ? "" : EntityUtils.toString(resp.getEntity());
            if (status < 200 || status >= 300) {
                throw new RuntimeException("Inter OAuth2 HTTP " + status + ": " + truncate(respBody, 300));
            }
            JsonNode node = json.readTree(respBody);
            String token = node.path("access_token").asText(null);
            long expiresIn = node.path("expires_in").asLong(3600);
            if (token == null || token.isBlank()) {
                throw new RuntimeException("Inter OAuth2 resposta sem access_token: " + truncate(respBody, 200));
            }
            this.cachedToken = token;
            this.tokenExpiresAtEpochMs = System.currentTimeMillis() + expiresIn * 1000;
            log.debug("🔑 Inter access_token renovado (expires_in={}s)", expiresIn);
            return token;
        });
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
