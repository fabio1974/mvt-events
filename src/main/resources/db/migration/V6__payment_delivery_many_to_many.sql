-- Migration V6: Alterar relacionamento Payment-Delivery para Many-to-Many
-- 
-- OBJETIVO:
-- Um pagamento pode incluir múltiplas entregas completadas do mesmo cliente
-- Permite que cliente pague várias entregas de uma vez via PIX
--
-- MUDANÇAS:
-- 1. Criar tabela payment_deliveries (N:N)
-- 2. Migrar dados existentes (delivery_id → payment_deliveries)
-- 3. Remover FK antiga (delivery_id)
--
-- IMPACTO:
-- - Payment passa de ManyToOne para ManyToMany com Delivery
-- - Permite agrupamento de entregas em um único pagamento
-- - Facilita relatórios detalhados por entrega

-- ============================================================================
-- STEP 1: Criar tabela associativa payment_deliveries
-- ============================================================================

CREATE TABLE IF NOT EXISTS payment_deliveries (
    payment_id BIGINT NOT NULL,
    delivery_id BIGINT NOT NULL,
    
    -- Composite PK (um payment pode ter várias deliveries, mas não duplicadas)
    PRIMARY KEY (payment_id, delivery_id),
    
    -- FK para payments
    CONSTRAINT fk_payment_deliveries_payment
        FOREIGN KEY (payment_id) 
        REFERENCES payments(id) 
        ON DELETE CASCADE,
    
    -- FK para deliveries
    CONSTRAINT fk_payment_deliveries_delivery
        FOREIGN KEY (delivery_id) 
        REFERENCES deliveries(id) 
        ON DELETE CASCADE
);

-- ============================================================================
-- STEP 2: Criar índices para performance
-- ============================================================================

-- Buscar deliveries de um payment (relatórios)
CREATE INDEX idx_payment_deliveries_payment_id 
    ON payment_deliveries(payment_id);

-- Buscar payment de uma delivery (verificar se já foi paga)
CREATE INDEX idx_payment_deliveries_delivery_id 
    ON payment_deliveries(delivery_id);

-- ============================================================================
-- STEP 3: Migrar dados existentes (delivery_id → payment_deliveries)
-- ============================================================================

-- Copiar relacionamentos 1:1 antigos para N:N
INSERT INTO payment_deliveries (payment_id, delivery_id)
SELECT id, delivery_id
FROM payments
WHERE delivery_id IS NOT NULL;

-- ============================================================================
-- STEP 4: Remover FK e coluna antiga delivery_id
-- ============================================================================

-- Drop constraint FK (se existir)
ALTER TABLE payments 
    DROP CONSTRAINT IF EXISTS fk_payments_delivery;

-- Drop coluna delivery_id
ALTER TABLE payments 
    DROP COLUMN IF EXISTS delivery_id;

-- ============================================================================
-- STEP 5: Validação e sumário
-- ============================================================================

DO $$
DECLARE
    payment_count INT;
    pd_count INT;
BEGIN
    SELECT COUNT(*) INTO payment_count FROM payments;
    SELECT COUNT(*) INTO pd_count FROM payment_deliveries;
    
    RAISE NOTICE '📊 SUMÁRIO DA MIGRAÇÃO V6:';
    RAISE NOTICE '   ├─ Tabela payment_deliveries criada';
    RAISE NOTICE '   ├─ Índices criados (payment_id, delivery_id)';
    RAISE NOTICE '   ├─ Dados migrados: % payments → % payment_deliveries', payment_count, pd_count;
    RAISE NOTICE '   ├─ Coluna delivery_id removida de payments';
    RAISE NOTICE '   └─ Relacionamento: Payment (1) ←→ (N) Delivery';
END $$;

-- ============================================================================
-- NOTAS IMPORTANTES:
-- ============================================================================
-- 
-- 1. **Backwards Compatibility:**
--    - Dados antigos (1:1) foram migrados para N:N
--    - Nenhum pagamento perdido
-- 
-- 2. **Novo Comportamento:**
--    - Payment.deliveries = Set<Delivery> (ManyToMany)
--    - Permite: 1 payment → múltiplas deliveries
--    - Use case: Cliente paga 5 entregas de uma vez
-- 
-- 3. **Relatórios:**
--    - SELECT p.*, d.* FROM payments p
--      JOIN payment_deliveries pd ON pd.payment_id = p.id
--      JOIN deliveries d ON d.id = pd.delivery_id
--      WHERE ...
-- 
-- 4. **Rollback (se necessário):**
--    - Adicionar coluna delivery_id novamente
--    - UPDATE payments SET delivery_id = (SELECT delivery_id FROM payment_deliveries WHERE payment_id = payments.id LIMIT 1)
--    - DROP TABLE payment_deliveries
-- 
-- ============================================================================
