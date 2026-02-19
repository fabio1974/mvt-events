package com.mvt.mvt_events.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Serviço para limpeza automática de push tokens inativos
 * 
 * Executa diariamente às 3h da manhã para desativar tokens que não foram 
 * atualizados há mais de 90 dias.
 */
@Service
@Slf4j
public class PushTokenCleanupService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Limpa tokens inativos há mais de 90 dias
     * 
     * Roda diariamente às 3h da manhã (horário do servidor)
     * Cron expression: "0 0 3 * * *"
     * - Segundo: 0
     * - Minuto: 0
     * - Hora: 3
     * - Dia do mês: * (todos)
     * - Mês: * (todos)
     * - Dia da semana: * (todos)
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupInactivePushTokens() {
        cleanupTokensOlderThan(90);
    }

    /**
     * Executa limpeza para tokens mais antigos que X dias
     * 
     * @param daysThreshold Número de dias de inatividade para considerar token como inativo
     */
    public void cleanupTokensOlderThan(int daysThreshold) {
        try {
            log.info("🧹 Iniciando limpeza automática de push tokens antigos (threshold: {} dias)...", daysThreshold);
            
            long startTime = System.currentTimeMillis();
            
            // Chamar função PostgreSQL que faz a limpeza
            Map<String, Object> result = jdbcTemplate.queryForMap(
                "SELECT * FROM cleanup_inactive_push_tokens(?)",
                daysThreshold
            );
            
            Integer deactivatedCount = (Integer) result.get("deactivated_count");
            Integer oldestTokenDays = (Integer) result.get("oldest_token_days");
            
            long duration = System.currentTimeMillis() - startTime;
            
            if (deactivatedCount > 0) {
                log.info("✅ Limpeza concluída em {}ms:", duration);
                log.info("   ├─ {} tokens desativados", deactivatedCount);
                log.info("   └─ Token ativo mais antigo: {} dias", oldestTokenDays);
            } else {
                log.info("✅ Limpeza concluída em {}ms: Nenhum token para desativar", duration);
                log.info("   └─ Token ativo mais antigo: {} dias", oldestTokenDays);
            }
            
        } catch (Exception e) {
            log.error("❌ Erro ao limpar tokens antigos: {}", e.getMessage(), e);
        }
    }

    /**
     * Método para executar limpeza manualmente via API (se necessário)
     * Pode ser chamado por um endpoint admin
     */
    public Map<String, Object> executeManualCleanup(int daysThreshold) {
        log.info("🔧 Limpeza manual iniciada com threshold de {} dias", daysThreshold);
        
        Map<String, Object> result = jdbcTemplate.queryForMap(
            "SELECT * FROM cleanup_inactive_push_tokens(?)",
            daysThreshold
        );
        
        Integer deactivatedCount = (Integer) result.get("deactivated_count");
        Integer oldestTokenDays = (Integer) result.get("oldest_token_days");
        
        log.info("✅ Limpeza manual concluída: {} tokens desativados, token mais antigo: {} dias",
            deactivatedCount, oldestTokenDays);
        
        return result;
    }

    /**
     * Retorna estatísticas sobre os push tokens
     */
    public Map<String, Object> getTokenStatistics() {
        try {
            String query = """
                SELECT 
                    COUNT(*) FILTER (WHERE is_active = true) as tokens_ativos,
                    COUNT(*) FILTER (WHERE is_active = false) as tokens_inativos,
                    COUNT(DISTINCT user_id) FILTER (WHERE is_active = true) as usuarios_com_token,
                    MAX(updated_at) FILTER (WHERE is_active = true) as ultimo_update,
                    MIN(updated_at) FILTER (WHERE is_active = true) as token_mais_antigo,
                    EXTRACT(DAY FROM NOW() - MIN(updated_at) FILTER (WHERE is_active = true))::INTEGER as dias_token_mais_antigo
                FROM user_push_tokens
                """;
            
            return jdbcTemplate.queryForMap(query);
            
        } catch (Exception e) {
            log.error("❌ Erro ao obter estatísticas de tokens: {}", e.getMessage(), e);
            return Map.of("error", e.getMessage());
        }
    }
}
