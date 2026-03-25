package com.mvt.mvt_events.maintenance;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.mvt.mvt_events.service.DeliveryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * One-shot: regrava {@code actual_route} no banco com o mesmo algoritmo de billing de
 * {@link DeliveryService#complete} (origem → primeiro GPS após pickup → … → último GPS → destino).
 * <p>
 * Uso (ex.: corrida 80):
 * <pre>
 * export SPRING_PROFILES_ACTIVE=billing-retrim
 * export APP_BILLING_RETRIM_IDS=80
 * ./gradlew bootRun
 * </pre>
 * Várias IDs: {@code APP_BILLING_RETRIM_IDS=80,81,82}. O processo encerra após aplicar.
 */
@Component
@Profile("billing-retrim")
@RequiredArgsConstructor
@Slf4j
public class BillingActualRouteRetrimRunner implements ApplicationRunner {

    private final DeliveryService deliveryService;
    private final ConfigurableApplicationContext applicationContext;

    @Value("${app.billing-retrim-ids:}")
    private String ids;

    @Override
    public void run(ApplicationArguments args) {
        AtomicInteger status = new AtomicInteger(0);
        try {
            if (ids == null || ids.isBlank()) {
                log.error("Profile billing-retrim: defina app.billing-retrim-ids (ex.: 80 ou 80,81)");
                status.set(1);
                return;
            }
            for (String raw : ids.split(",")) {
                String t = raw.trim();
                if (t.isEmpty()) {
                    continue;
                }
                long id = Long.parseLong(t);
                log.info("Normalizando actual_route (billing) da delivery #{}...", id);
                deliveryService.trimActualRouteInDatabase(id);
            }
            log.info("billing-retrim concluído para: {}", ids);
        } catch (Exception e) {
            log.error("billing-retrim falhou: {}", e.getMessage(), e);
            status.set(1);
        } finally {
            int code = SpringApplication.exit(applicationContext, status::get);
            System.exit(code);
        }
    }
}
