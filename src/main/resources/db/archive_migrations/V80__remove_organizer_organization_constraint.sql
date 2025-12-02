-- ============================================================================
-- Migration V80: Remove constraint that forces ORGANIZER to have organization
-- ============================================================================
-- Description: Remove a constraint chk_organizer_must_have_organization
--              Permite que ORGANIZER seja criado sem organization_id
-- Author: System
-- Date: 2025-11-24
-- ============================================================================

-- Remove a constraint que obriga ORGANIZER a ter organization_id
ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_organizer_must_have_organization;

-- Log da alteração
DO $$
BEGIN
    RAISE NOTICE '============================================';
    RAISE NOTICE 'CONSTRAINT REMOVIDA';
    RAISE NOTICE '============================================';
    RAISE NOTICE '';
    RAISE NOTICE 'Constraint removida:';
    RAISE NOTICE '  ✓ chk_organizer_must_have_organization';
    RAISE NOTICE '';
    RAISE NOTICE 'ORGANIZER agora pode ser criado sem organization_id';
    RAISE NOTICE '============================================';
END $$;
