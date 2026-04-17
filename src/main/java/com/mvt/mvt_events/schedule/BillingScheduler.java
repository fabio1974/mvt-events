package com.mvt.mvt_events.schedule;

import com.mvt.mvt_events.jpa.ClientSubscription;
import com.mvt.mvt_events.repository.ClientSubscriptionRepository;
import com.mvt.mvt_events.service.BillingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Scheduler de billing para serviços recorrentes.
 *
 * Roda todo dia às 06:00. Verifica se hoje é dia de geração de fatura
 * para alguma subscription ativa (5 dias antes do vencimento).
 *
 * Tabela de geração:
 *   billing_due_day → gera no dia
 *   1  → 25 (mês anterior)
 *   5  → 1
 *   10 → 5
 *   15 → 10
 *   20 → 15
 *   25 → 20
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class BillingScheduler {

    private static final ZoneId TZ = ZoneId.of("America/Fortaleza");

    /**
     * Mapa: dia de geração → billing_due_day correspondente.
     * "Se hoje é dia X, gero faturas de quem vence no dia Y."
     */
    private static final Map<Integer, Integer> GENERATION_TO_DUE = Map.of(
            25, 1,
            1, 5,
            5, 10,
            10, 15,
            15, 20,
            20, 25
    );

    private final ClientSubscriptionRepository subscriptionRepository;
    private final BillingService billingService;

    /**
     * Roda todo dia às 06:00. Verifica se hoje é dia de geração.
     */
    @Scheduled(cron = "0 0 6 * * *")
    public void generateBillingInvoices() {
        int today = LocalDate.now(TZ).getDayOfMonth();
        Integer dueDayToGenerate = GENERATION_TO_DUE.get(today);

        if (dueDayToGenerate == null) {
            log.debug("📅 Billing: dia {} não é dia de geração de faturas", today);
            return;
        }

        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║ 💰 BILLING: Geração de faturas mensais                        ║");
        log.info("║ 📅 Hoje: {} — gerando para vencimento dia {}                  ║",
                LocalDate.now(TZ).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), dueDayToGenerate);
        log.info("╚════════════════════════════════════════════════════════════════╝");

        List<ClientSubscription> subscriptions = subscriptionRepository
                .findByActiveTrueAndBillingDueDay(dueDayToGenerate);

        if (subscriptions.isEmpty()) {
            log.info("📋 Nenhuma subscription ativa com vencimento dia {}", dueDayToGenerate);
            return;
        }

        int generated = 0;
        int skipped = 0;
        int errors = 0;

        for (ClientSubscription sub : subscriptions) {
            try {
                billingService.generateMonthlyInvoice(sub);
                generated++;
            } catch (Exception e) {
                errors++;
                log.error("❌ Erro ao gerar fatura para subscription {}: {}", sub.getId(), e.getMessage(), e);
            }
        }

        log.info("═══════════════════════════════════════════════════════════════");
        log.info("📊 RESULTADO DO BILLING");
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("  📋 Subscriptions processadas: {}", subscriptions.size());
        log.info("  ✅ Faturas geradas: {}", generated);
        log.info("  ❌ Erros: {}", errors);
        log.info("═══════════════════════════════════════════════════════════════");
    }
}
