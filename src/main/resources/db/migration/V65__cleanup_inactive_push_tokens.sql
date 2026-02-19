-- V65: Adicionar índice para cleanup de tokens antigos e criar função de limpeza automática
-- Data: 2026-02-18
-- Descrição: Melhora performance de queries e adiciona suporte para limpeza automática de tokens inativos

-- =============================================================================
-- PARTE 1: Índices para Melhorar Performance
-- =============================================================================

-- Índice para queries por updated_at (usado na limpeza)
CREATE INDEX IF NOT EXISTS idx_user_push_tokens_updated_at 
ON user_push_tokens(updated_at) 
WHERE is_active = true;

-- Índice composto para queries de tokens ativos por usuário
CREATE INDEX IF NOT EXISTS idx_user_push_tokens_user_active 
ON user_push_tokens(user_id, is_active, updated_at DESC);

-- =============================================================================
-- PARTE 2: Função para Desativar Tokens Antigos
-- =============================================================================

-- Função que desativa tokens não atualizados há mais de X dias
CREATE OR REPLACE FUNCTION cleanup_inactive_push_tokens(days_threshold INTEGER DEFAULT 90)
RETURNS TABLE(
    deactivated_count INTEGER,
    oldest_token_days INTEGER
) AS $$
DECLARE
    deactivated_count INTEGER;
    oldest_token_days INTEGER;
BEGIN
    -- Calcular quantos tokens serão desativados
    SELECT COUNT(*) INTO deactivated_count
    FROM user_push_tokens
    WHERE is_active = true
    AND updated_at < NOW() - (days_threshold || ' days')::INTERVAL;
    
    -- Encontrar o token ativo mais antigo (em dias)
    SELECT EXTRACT(DAY FROM NOW() - MIN(updated_at))::INTEGER INTO oldest_token_days
    FROM user_push_tokens
    WHERE is_active = true;
    
    -- Desativar tokens antigos
    UPDATE user_push_tokens
    SET is_active = false,
        updated_at = NOW()
    WHERE is_active = true
    AND updated_at < NOW() - (days_threshold || ' days')::INTERVAL;
    
    -- Retornar estatísticas
    RETURN QUERY SELECT deactivated_count, COALESCE(oldest_token_days, 0);
END;
$$ LANGUAGE plpgsql;

-- =============================================================================
-- PARTE 3: Comentários e Documentação
-- =============================================================================

COMMENT ON FUNCTION cleanup_inactive_push_tokens(INTEGER) IS 
'Desativa push tokens que não foram atualizados há mais de X dias (padrão: 90).
Retorna quantidade de tokens desativados e idade do token ativo mais antigo.

Exemplo de uso:
  SELECT * FROM cleanup_inactive_push_tokens(90);  -- 90 dias
  SELECT * FROM cleanup_inactive_push_tokens(30);  -- 30 dias
';

COMMENT ON INDEX idx_user_push_tokens_updated_at IS 
'Índice para otimizar limpeza de tokens antigos por updated_at';

COMMENT ON INDEX idx_user_push_tokens_user_active IS 
'Índice composto para queries de tokens ativos por usuário, ordenados do mais recente';

-- =============================================================================
-- PARTE 4: Executar Limpeza Inicial
-- =============================================================================

-- Executar limpeza inicial de tokens > 90 dias
DO $$
DECLARE
    result RECORD;
BEGIN
    SELECT * INTO result FROM cleanup_inactive_push_tokens(90);
    
    RAISE NOTICE '🧹 Limpeza inicial executada:';
    RAISE NOTICE '   - Tokens desativados: %', result.deactivated_count;
    RAISE NOTICE '   - Token ativo mais antigo: % dias', result.oldest_token_days;
END $$;

-- =============================================================================
-- NOTAS DE USO:
-- =============================================================================

-- Para executar manualmente a limpeza:
-- SELECT * FROM cleanup_inactive_push_tokens(90);  -- Desativa tokens > 90 dias

-- Para verificar estatísticas de tokens:
-- SELECT 
--     COUNT(*) FILTER (WHERE is_active = true) as tokens_ativos,
--     COUNT(*) FILTER (WHERE is_active = false) as tokens_inativos,
--     COUNT(DISTINCT user_id) FILTER (WHERE is_active = true) as usuarios_com_token,
--     MAX(updated_at) FILTER (WHERE is_active = true) as ultimo_update,
--     MIN(updated_at) FILTER (WHERE is_active = true) as token_mais_antigo,
--     EXTRACT(DAY FROM NOW() - MIN(updated_at) FILTER (WHERE is_active = true)) as dias_token_mais_antigo
-- FROM user_push_tokens;

-- Para criar job agendado (via aplicação Spring Boot):
-- @Scheduled(cron = "0 0 3 * * *")  // Diariamente às 3h da manhã
-- public void cleanupInactivePushTokens() {
--     jdbcTemplate.queryForObject("SELECT cleanup_inactive_push_tokens(90)", ...);
-- }
