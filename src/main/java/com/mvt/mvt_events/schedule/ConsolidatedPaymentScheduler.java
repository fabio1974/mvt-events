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
 * Scheduler para processamento automático de pagamentos consolidados
 * 
 * Execução: A cada 4 horas (0h, 4h, 8h, 12h, 16h, 20h)
 * 
 * Fluxo:
 * 1. Busca todos os clientes com deliveries COMPLETED não pagas
 * 2. Para cada cliente:
 *    a. Filtra deliveries que NÃO têm payment PAID
 *    b. Filtra apenas as com payment NULL, FAILED ou EXPIRED
 *    c. Cria pagamento consolidado
 *    d. Cria order no Pagar.me
 * 
 * Logging: Completo com timestamps e estatísticas
 * 
 * @see ConsolidatedPaymentService
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ConsolidatedPaymentScheduler {

    private final ConsolidatedPaymentService consolidatedPaymentService;

    /**
     * Executa consolidação de pagamentos a cada 4 horas
     * 
     * Cron: 0 0 0,4,8,12,16,20 * * * (cada 4 horas: 0h, 4h, 8h, 12h, 16h, 20h)
     * Timezone: America/Fortaleza (horário de Fortaleza - CE)
     * 
     * Sincronização: lockProvider necessário para ambientes com múltiplas instâncias
     * 
     * NOTA: Cron job temporariamente desabilitado. Descomente @Scheduled para reativar.
     */
    // @Scheduled(
    //     cron = "0 0 0,4,8,12,16,20 * * *",
    //     zone = "America/Fortaleza"
    // )
    public void consolidatePaymentsEvery4Hours() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║ 🕐 CRONJOB: Consolidação de Pagamentos                        ║");
        log.info("║ ⏰ Timestamp: {}                              ║", timestamp);
        log.info("╚════════════════════════════════════════════════════════════════╝");

        try {
            // Executar processamento
            Map<String, Object> results = consolidatedPaymentService.processAllClientsConsolidatedPayments();

            // Log de resultados
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
        return consolidatedPaymentService.processClientConsolidatedPayments(clientId);
    }
}
