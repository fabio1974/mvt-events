package com.mvt.mvt_events.schedule;

import com.mvt.mvt_events.service.ConsolidatedPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Scheduler para processamento automático de pagamentos consolidados.
 *
 * Criação: 4x/dia (08h, 12h, 16h, 20h). PIX gerado expira em 3h55 (235 min),
 * ou seja, sempre expira antes da próxima rodada (janela de 5 min entre expiração e novo PIX).
 *
 * Lembrete: tratado pelo ConsolidatedPaymentReminderScheduler (16:05).
 *
 * @see ConsolidatedPaymentService
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ConsolidatedPaymentScheduler {

    private final ConsolidatedPaymentService consolidatedPaymentService;

    /**
     * Cria PIX consolidado para todos os clientes com deliveries não pagas.
     * Roda 4x/dia: 08:00, 12:00, 16:00, 20:00. PIX expira em 3h55 (antes da próxima rodada).
     */
    @Scheduled(cron = "0 0 8,12,16,20 * * *")
    public void consolidatePayments() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║ 🕐 CRONJOB: Criação de PIX Consolidados                         ║");
        log.info("║ ⏰ Timestamp: {}                              ║", timestamp);
        log.info("╚════════════════════════════════════════════════════════════════╝");

        try {
            Map<String, Object> results = consolidatedPaymentService.processAllClientsConsolidatedPayments();
            logResults(results);
        } catch (Exception e) {
            log.error("❌ ERRO NO CRONJOB: Falha ao processar pagamentos consolidados", e);
        }
    }

    /**
     * Loga resultados do processamento de forma legível
     */
    private void logResults(Map<String, Object> results) {
        int processedClients = (Integer) results.getOrDefault("processedClients", 0);
        int createdPayments = (Integer) results.getOrDefault("createdPayments", 0);
        int includedDeliveries = (Integer) results.getOrDefault("includedDeliveries", 0);
        
        log.info("");
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("📊 RESULTADO DO PROCESSAMENTO");
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("  👥 Clientes processados: {}", processedClients);
        log.info("  💳 Pagamentos criados: {}", createdPayments);
        log.info("  📦 Deliveries incluídas: {}", includedDeliveries);

        @SuppressWarnings("unchecked")
        java.util.List<String> errors = (java.util.List<String>) results.get("errors");
        if (errors != null && !errors.isEmpty()) {
            log.warn("  ⚠️  Erros encontrados: {}", errors.size());
            errors.forEach(error -> log.warn("     - {}", error));
        }

        log.info("═══════════════════════════════════════════════════════════════");
        log.info("");
    }

    /**
     * Alternativa: Consolidar pagamentos para um cliente específico
     * Pode ser disparada manualmente via API se necessário
     * 
     * @param clientId UUID do cliente
     * @return Estatísticas do processamento
     */
    public Map<String, Object> triggerConsolidationForClient(java.util.UUID clientId) {
        log.info("🔔 Consolidação manual disparada para cliente: {}", clientId);
        return consolidatedPaymentService.processClientConsolidatedPayments(clientId, ConsolidatedPaymentService.DEFAULT_PIX_EXPIRY_MINUTES);
    }
}
