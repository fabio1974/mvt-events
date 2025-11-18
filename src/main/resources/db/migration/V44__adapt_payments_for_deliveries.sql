-- ============================================================================
-- Migration V44: Adapt payments table for deliveries
-- ============================================================================
-- Description: Adapts existing payments table to work with deliveries instead of registrations
-- Author: System  
-- Date: 2025-11-05
-- ============================================================================

-- Add delivery_id column to payments if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'payments' AND column_name = 'delivery_id') THEN
        ALTER TABLE payments ADD COLUMN delivery_id BIGINT;
        
        -- Add foreign key constraint
        ALTER TABLE payments ADD CONSTRAINT fk_payment_delivery 
            FOREIGN KEY (delivery_id) REFERENCES deliveries(id) ON DELETE CASCADE;
            
        -- Add index
        CREATE INDEX idx_payment_delivery ON payments(delivery_id);
        
        -- Add comment
        COMMENT ON COLUMN payments.delivery_id IS 'ID da entrega relacionada (novo sistema)';
    END IF;
END $$;

-- Add payer_id column if it doesn't exist (for user who is paying)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'payments' AND column_name = 'payer_id') THEN
        ALTER TABLE payments ADD COLUMN payer_id UUID;
        
        -- Add foreign key constraint
        ALTER TABLE payments ADD CONSTRAINT fk_payment_payer 
            FOREIGN KEY (payer_id) REFERENCES users(id) ON DELETE RESTRICT;
            
        -- Add index
        CREATE INDEX idx_payment_payer ON payments(payer_id);
        
        -- Add comment
        COMMENT ON COLUMN payments.payer_id IS 'ID do usuário que está pagando';
    END IF;
END $$;

-- Add organization_id column if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'payments' AND column_name = 'organization_id') THEN
        ALTER TABLE payments ADD COLUMN organization_id BIGINT;
        
        -- Add foreign key constraint
        ALTER TABLE payments ADD CONSTRAINT fk_payment_organization 
            FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE SET NULL;
            
        -- Add index
        CREATE INDEX idx_payment_organization ON payments(organization_id);
        
        -- Add comment
        COMMENT ON COLUMN payments.organization_id IS 'ID da organização que receberá o pagamento';
    END IF;
END $$;

-- Add transaction_id column if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'payments' AND column_name = 'transaction_id') THEN
        ALTER TABLE payments ADD COLUMN transaction_id VARCHAR(100) UNIQUE;
        
        -- Add index
        CREATE INDEX idx_payment_transaction ON payments(transaction_id);
        
        -- Add comment
        COMMENT ON COLUMN payments.transaction_id IS 'ID único da transação (gerado internamente)';
    END IF;
END $$;

-- Add notes column if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'payments' AND column_name = 'notes') THEN
        ALTER TABLE payments ADD COLUMN notes TEXT;
        
        -- Add comment
        COMMENT ON COLUMN payments.notes IS 'Observações sobre o pagamento';
    END IF;
END $$;

-- Add metadata column if it doesn't exist (the table already has gateway_response JSONB)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'payments' AND column_name = 'metadata') THEN
        ALTER TABLE payments ADD COLUMN metadata JSONB;
        
        -- Add comment
        COMMENT ON COLUMN payments.metadata IS 'Dados adicionais em formato JSON';
    END IF;
END $$;

-- Rename payment_status to status if needed
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'payments' AND column_name = 'payment_status') 
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns 
                       WHERE table_name = 'payments' AND column_name = 'status') THEN
        ALTER TABLE payments RENAME COLUMN payment_status TO status;
    END IF;
END $$;

-- Add payment_date column if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'payments' AND column_name = 'payment_date') THEN
        ALTER TABLE payments ADD COLUMN payment_date TIMESTAMP;
        
        -- Add index
        CREATE INDEX idx_payment_date ON payments(payment_date);
        
        -- Add comment
        COMMENT ON COLUMN payments.payment_date IS 'Data/hora em que o pagamento foi concluído';
    END IF;
END $$;

-- Rename gateway_provider to provider if needed
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'payments' AND column_name = 'gateway_provider') 
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns 
                       WHERE table_name = 'payments' AND column_name = 'provider') THEN
        ALTER TABLE payments RENAME COLUMN gateway_provider TO provider;
        
        -- Add index
        CREATE INDEX IF NOT EXISTS idx_payment_provider ON payments(provider);
    END IF;
END $$;

-- Rename gateway_payment_id to provider_payment_id if needed
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'payments' AND column_name = 'gateway_payment_id') 
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns 
                       WHERE table_name = 'payments' AND column_name = 'provider_payment_id') THEN
        ALTER TABLE payments RENAME COLUMN gateway_payment_id TO provider_payment_id;
    END IF;
END $$;

-- Update table comment
COMMENT ON TABLE payments IS 'Pagamentos de entregas e registros (sistema unificado)';

-- Print migration summary
DO $$
BEGIN
    RAISE NOTICE '============================================';
    RAISE NOTICE 'MIGRAÇÃO V44 APLICADA COM SUCESSO';
    RAISE NOTICE '============================================';
    RAISE NOTICE 'Tabela payments adaptada para entregas';
    RAISE NOTICE 'Colunas adicionadas:';
    RAISE NOTICE '  - delivery_id (relacionamento com entregas)';
    RAISE NOTICE '  - payer_id (usuário pagador)';
    RAISE NOTICE '  - organization_id (organização receptora)';
    RAISE NOTICE '  - transaction_id (ID único da transação)';
    RAISE NOTICE '  - notes (observações)';
    RAISE NOTICE '  - metadata (dados adicionais)';
    RAISE NOTICE '  - payment_date (data do pagamento)';
    RAISE NOTICE 'Colunas renomeadas:';
    RAISE NOTICE '  - payment_status -> status';
    RAISE NOTICE '  - gateway_provider -> provider';
    RAISE NOTICE '  - gateway_payment_id -> provider_payment_id';
    RAISE NOTICE '============================================';
END $$;