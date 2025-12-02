-- ============================================================================
-- Migration V54: Add constraints for organizationId rules by role
-- ============================================================================
-- Description: Adiciona constraints para garantir que:
--              - ADMIN não pode ter organization_id
--              - COURIER não pode ter organization_id (usa employment_contracts)
--              - ORGANIZER deve ter organization_id (obrigatório)
--              - CLIENT não deve ter organization_id
-- Author: System
-- Date: 2025-11-05
-- ============================================================================

-- ============================================================================
-- 1. Remover constraint antiga se existir
-- ============================================================================
ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_organization_only_for_organizer;

-- ============================================================================
-- 2. Constraint: ADMIN não pode ter organization_id
-- ============================================================================
ALTER TABLE users 
ADD CONSTRAINT chk_admin_no_organization
CHECK (
    role != 'ADMIN' OR organization_id IS NULL
);

COMMENT ON CONSTRAINT chk_admin_no_organization ON users IS 
'ADMIN não pode ter organization_id - tem acesso total ao sistema';

-- ============================================================================
-- 3. Constraint: COURIER não pode ter organization_id direto
-- ============================================================================
ALTER TABLE users 
ADD CONSTRAINT chk_courier_no_organization
CHECK (
    role != 'COURIER' OR organization_id IS NULL
);

COMMENT ON CONSTRAINT chk_courier_no_organization ON users IS 
'COURIER não pode ter organization_id - obtém organizações via employment_contracts';

-- ============================================================================
-- 4. Constraint: ORGANIZER deve ter organization_id (obrigatório)
-- ============================================================================
ALTER TABLE users 
ADD CONSTRAINT chk_organizer_must_have_organization
CHECK (
    role != 'ORGANIZER' OR organization_id IS NOT NULL
);

COMMENT ON CONSTRAINT chk_organizer_must_have_organization ON users IS 
'ORGANIZER deve ter organization_id - é o dono/gerente de uma empresa';

-- ============================================================================
-- 5. Constraint: CLIENT não deve ter organization_id
-- ============================================================================
ALTER TABLE users 
ADD CONSTRAINT chk_client_no_organization
CHECK (
    role != 'CLIENT' OR organization_id IS NULL
);

COMMENT ON CONSTRAINT chk_client_no_organization ON users IS 
'CLIENT não pode ter organization_id - solicita entregas como cliente final';

-- ============================================================================
-- 6. Validar dados existentes
-- ============================================================================

-- Limpar organization_id de ADMIN se houver
UPDATE users 
SET organization_id = NULL 
WHERE role = 'ADMIN' AND organization_id IS NOT NULL;

-- Limpar organization_id de COURIER se houver
UPDATE users 
SET organization_id = NULL 
WHERE role = 'COURIER' AND organization_id IS NOT NULL;

-- Limpar organization_id de CLIENT se houver
UPDATE users 
SET organization_id = NULL 
WHERE role = 'CLIENT' AND organization_id IS NOT NULL;

-- ============================================================================
-- Resumo
-- ============================================================================
DO $$
DECLARE
    admin_count INTEGER;
    courier_count INTEGER;
    organizer_count INTEGER;
    client_count INTEGER;
    organizer_without_org INTEGER;
BEGIN
    SELECT COUNT(*) INTO admin_count FROM users WHERE role = 'ADMIN';
    SELECT COUNT(*) INTO courier_count FROM users WHERE role = 'COURIER';
    SELECT COUNT(*) INTO organizer_count FROM users WHERE role = 'ORGANIZER';
    SELECT COUNT(*) INTO client_count FROM users WHERE role = 'CLIENT';
    SELECT COUNT(*) INTO organizer_without_org FROM users WHERE role = 'ORGANIZER' AND organization_id IS NULL;
    
    RAISE NOTICE '============================================';
    RAISE NOTICE 'CONSTRAINTS DE ORGANIZATIONID APLICADAS';
    RAISE NOTICE '============================================';
    RAISE NOTICE '';
    RAISE NOTICE 'Regras implementadas:';
    RAISE NOTICE '  ✓ ADMIN (%) não pode ter organization_id', admin_count;
    RAISE NOTICE '  ✓ COURIER (%) não pode ter organization_id', courier_count;
    RAISE NOTICE '  ✓ ORGANIZER (%) DEVE ter organization_id', organizer_count;
    RAISE NOTICE '  ✓ CLIENT (%) não pode ter organization_id', client_count;
    RAISE NOTICE '';
    
    IF organizer_without_org > 0 THEN
        RAISE WARNING '⚠️  ATENÇÃO: % ORGANIZER(s) sem organization_id!', organizer_without_org;
        RAISE WARNING '   Estes usuários precisam ser corrigidos manualmente.';
    END IF;
    
    RAISE NOTICE 'Constraints criadas:';
    RAISE NOTICE '  - chk_admin_no_organization';
    RAISE NOTICE '  - chk_courier_no_organization';
    RAISE NOTICE '  - chk_organizer_must_have_organization';
    RAISE NOTICE '  - chk_client_no_organization';
    RAISE NOTICE '============================================';
END $$;
