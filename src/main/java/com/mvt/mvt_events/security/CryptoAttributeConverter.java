package com.mvt.mvt_events.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * JPA AttributeConverter para criptografar/descriptografar dados sensíveis
 * usando AES-256-GCM (Galois/Counter Mode)
 * 
 * <p>Este converter é aplicado automaticamente aos campos anotados com:
 * <pre>
 * @Convert(converter = CryptoAttributeConverter.class)
 * private String accountNumber;
 * </pre>
 * 
 * <p><strong>Algoritmo:</strong> AES-256-GCM
 * <p><strong>Segurança:</strong>
 * <ul>
 *   <li>Chave de 256 bits (32 bytes)</li>
 *   <li>IV (Initialization Vector) aleatório de 12 bytes para cada criptografia</li>
 *   <li>Tag de autenticação GCM de 128 bits</li>
 *   <li>Armazenamento: Base64(IV + CipherText + Tag)</li>
 * </ul>
 * 
 * <p><strong>Configuração:</strong>
 * <pre>
 * # application.properties
 * app.security.encryption.key=your-32-byte-base64-encoded-key-here
 * </pre>
 * 
 * <p><strong>Gerar chave:</strong>
 * <pre>
 * openssl rand -base64 32
 * </pre>
 * 
 * @see javax.persistence.AttributeConverter
 * @see javax.crypto.Cipher
 */
@Slf4j
@Component
@Converter
public class CryptoAttributeConverter implements AttributeConverter<String, String> {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 128; // bits
    
    @Value("${app.security.encryption.key:}")
    private String encryptionKey;
    
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Criptografa o valor antes de salvar no banco de dados
     * 
     * @param attribute Valor em texto claro (ex: "12345678-9")
     * @return Valor criptografado em Base64 ou null se attribute for null
     */
    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        
        try {
            // Validar se a chave está configurada
            if (encryptionKey == null || encryptionKey.isEmpty()) {
                log.warn("⚠️ Chave de criptografia não configurada! Dados NÃO serão criptografados.");
                return attribute; // Retornar texto claro se não houver chave
            }
            
            // Gerar IV aleatório (deve ser único para cada criptografia)
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            
            // Preparar chave AES
            byte[] keyBytes = Base64.getDecoder().decode(encryptionKey);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            
            // Configurar cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
            
            // Criptografar
            byte[] plaintext = attribute.getBytes(StandardCharsets.UTF_8);
            byte[] ciphertext = cipher.doFinal(plaintext);
            
            // Combinar IV + Ciphertext em um único array
            byte[] combined = ByteBuffer.allocate(iv.length + ciphertext.length)
                    .put(iv)
                    .put(ciphertext)
                    .array();
            
            // Retornar em Base64
            return Base64.getEncoder().encodeToString(combined);
            
        } catch (Exception e) {
            log.error("❌ Erro ao criptografar dado: {}", e.getMessage());
            throw new RuntimeException("Falha na criptografia", e);
        }
    }

    /**
     * Descriptografa o valor ao ler do banco de dados
     * 
     * @param dbData Valor criptografado em Base64
     * @return Valor descriptografado em texto claro ou null se dbData for null
     */
    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        
        try {
            // Se não há chave configurada, assumir que dado está em texto claro
            if (encryptionKey == null || encryptionKey.isEmpty()) {
                log.warn("⚠️ Chave de criptografia não configurada! Lendo dado como texto claro.");
                return dbData;
            }
            
            // Decodificar Base64
            byte[] combined = Base64.getDecoder().decode(dbData);
            
            // Separar IV e Ciphertext
            ByteBuffer buffer = ByteBuffer.wrap(combined);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);
            
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);
            
            // Preparar chave AES
            byte[] keyBytes = Base64.getDecoder().decode(encryptionKey);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            
            // Configurar cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
            
            // Descriptografar
            byte[] plaintext = cipher.doFinal(ciphertext);
            
            return new String(plaintext, StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            log.error("❌ Erro ao descriptografar dado: {}", e.getMessage());
            throw new RuntimeException("Falha na descriptografia", e);
        }
    }
}
