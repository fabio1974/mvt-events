package com.mvt.mvt_events.config;

import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.Security;

/**
 * Configuração para Web Push Notifications
 */
@Configuration
@Slf4j
public class WebPushConfig {

    @Value("${webpush.vapid.public-key}")
    private String vapidPublicKey;

    @Value("${webpush.vapid.private-key}")
    private String vapidPrivateKey;

    @Value("${webpush.vapid.subject}")
    private String vapidSubject;

    @Bean
    public PushService pushService() {
        try {
            // Registrar BouncyCastle provider se não estiver presente
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(new BouncyCastleProvider());
                log.info("BouncyCastle provider registrado para Web Push");
            } else {
                log.info("BouncyCastle provider já estava registrado");
            }

            // Criar e configurar PushService
            PushService pushService = new PushService();

            // Configurar VAPID apenas se as chaves estiverem disponíveis
            if (vapidPublicKey != null && !vapidPublicKey.isEmpty() &&
                    !vapidPublicKey.equals("your-public-key-here") &&
                    vapidPrivateKey != null && !vapidPrivateKey.isEmpty() &&
                    !vapidPrivateKey.equals("your-private-key-here")) {

                pushService.setPublicKey(vapidPublicKey);
                pushService.setPrivateKey(vapidPrivateKey);
                pushService.setSubject(vapidSubject);

                log.info("✅ Web Push Service configurado com VAPID keys");
                log.info("🔑 Public Key: {}...", vapidPublicKey.substring(0, Math.min(20, vapidPublicKey.length())));
                log.info("📧 Subject: {}", vapidSubject);
            } else {
                log.warn("⚠️ VAPID keys não configuradas - Web Push não funcionará totalmente");
            }

            return pushService;

        } catch (Exception e) {
            log.error("❌ Erro ao configurar Web Push Service: {}", e.getMessage(), e);
            // Retorna um service básico mesmo com erro
            return new PushService();
        }
    }
}