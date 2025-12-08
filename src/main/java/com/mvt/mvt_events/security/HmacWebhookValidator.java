package com.mvt.mvt_events.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * Utilitário para validação de webhooks usando HMAC SHA256
 * 
 * <p>Valida a autenticidade de webhooks do Iugu comparando a assinatura
 * enviada no header com uma assinatura calculada localmente.
 * 
 * <p><strong>Fluxo de validação:</strong>
 * <ol>
 *   <li>Iugu envia webhook com header: X-Iugu-Signature: sha256=abc123...</li>
 *   <li>Sistema calcula HMAC do payload usando secret configurado</li>
 *   <li>Compara as duas assinaturas (comparação segura contra timing attacks)</li>
 *   <li>Se iguais: webhook válido; caso contrário: rejeitar</li>
 * </ol>
 * 
 * <p><strong>Configuração:</strong>
 * <pre>
 * # application.properties
 * iugu.webhook.secret=your-webhook-secret-from-iugu-dashboard
 * </pre>
 * 
 * <p><strong>Uso no WebhookController:</strong>
 * <pre>
 * @PostMapping
 * public ResponseEntity<?> handleWebhook(
 *     @RequestBody String payload,
 *     @RequestHeader("X-Iugu-Signature") String signature
 * ) {
 *     if (!hmacValidator.validateSignature(payload, signature)) {
 *         return ResponseEntity.status(401).body("Invalid signature");
 *     }
 *     // Processar webhook...
 * }
 * </pre>
 * 
 * @see <a href="https://dev.iugu.com/docs/webhooks">Iugu Webhooks Documentation</a>
 */
@Slf4j
@Component
public class HmacWebhookValidator {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";
    
    @Value("${iugu.webhook.secret:}")
    private String webhookSecret;

    /**
     * Valida a assinatura HMAC de um webhook
     * 
     * @param payload Corpo do webhook em formato JSON (string)
     * @param receivedSignature Assinatura recebida no header (ex: "sha256=abc123...")
     * @return true se assinatura é válida, false caso contrário
     */
    public boolean validateSignature(String payload, String receivedSignature) {
        // Verificar se webhook secret está configurado
        if (webhookSecret == null || webhookSecret.isEmpty()) {
            log.warn("⚠️ Webhook secret não configurado! Validação HMAC desabilitada.");
            return true; // Permitir em desenvolvimento (REMOVER EM PRODUÇÃO!)
        }
        
        // Verificar se assinatura foi enviada
        if (receivedSignature == null || receivedSignature.isEmpty()) {
            log.error("❌ Webhook recebido sem assinatura");
            return false;
        }
        
        try {
            // Calcular assinatura esperada
            String expectedSignature = calculateSignature(payload);
            
            // Remover prefixo "sha256=" se presente
            String cleanReceivedSignature = receivedSignature.startsWith(SIGNATURE_PREFIX) 
                ? receivedSignature.substring(SIGNATURE_PREFIX.length())
                : receivedSignature;
            
            String cleanExpectedSignature = expectedSignature.startsWith(SIGNATURE_PREFIX)
                ? expectedSignature.substring(SIGNATURE_PREFIX.length())
                : expectedSignature;
            
            // Comparação segura (constant-time comparison para prevenir timing attacks)
            boolean isValid = secureEquals(cleanExpectedSignature, cleanReceivedSignature);
            
            if (isValid) {
                log.info("✅ Webhook signature válida");
            } else {
                log.error("❌ Webhook signature inválida!");
                log.debug("Expected: {}", cleanExpectedSignature);
                log.debug("Received: {}", cleanReceivedSignature);
            }
            
            return isValid;
            
        } catch (Exception e) {
            log.error("❌ Erro ao validar webhook signature: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Calcula a assinatura HMAC SHA256 de um payload
     * 
     * @param payload Corpo do webhook em formato JSON
     * @return Assinatura no formato "sha256=hexadecimal"
     */
    public String calculateSignature(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKey = new SecretKeySpec(
                webhookSecret.getBytes(StandardCharsets.UTF_8), 
                HMAC_ALGORITHM
            );
            mac.init(secretKey);
            
            byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            
            // Converter para hexadecimal
            StringBuilder hex = new StringBuilder();
            for (byte b : hmacBytes) {
                hex.append(String.format("%02x", b));
            }
            
            return SIGNATURE_PREFIX + hex.toString();
            
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("❌ Erro ao calcular HMAC: {}", e.getMessage());
            throw new RuntimeException("Falha ao calcular HMAC", e);
        }
    }

    /**
     * Comparação segura de strings (constant-time) para prevenir timing attacks
     * 
     * @param expected String esperada
     * @param actual String recebida
     * @return true se strings são iguais
     */
    private boolean secureEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        
        if (expected.length() != actual.length()) {
            return false;
        }
        
        int result = 0;
        for (int i = 0; i < expected.length(); i++) {
            result |= expected.charAt(i) ^ actual.charAt(i);
        }
        
        return result == 0;
    }
}
