-- Migration V16: Remove deprecated phone column from users table
-- 
-- Description:
--   Removes the old 'phone' column from users table since we now use
--   phone_ddd + phone_number fields (added in V15).
--
-- Dependencies:
--   - V15__refactor_phone_and_cleanup_bank_accounts.sql must have run successfully
--   - All phone data must have been migrated to phone_ddd + phone_number
--
-- Date: 2025-12-12
-- ============================================================================

-- ============================================================================
-- 1. VERIFY DATA MIGRATION (optional check)
-- ============================================================================

-- This query helps verify that all phone data has been migrated:
-- SELECT COUNT(*) FROM users WHERE phone IS NOT NULL AND (phone_ddd IS NULL OR phone_number IS NULL);
-- Expected result: 0 rows

-- ============================================================================
-- 2. DROP DEPRECATED PHONE COLUMN (CASCADE to drop dependent views)
-- ============================================================================

ALTER TABLE users DROP COLUMN IF EXISTS phone CASCADE;

-- ============================================================================
-- 3. RECREATE VIEW available_on_demand_deliveries without phone column
-- ============================================================================

CREATE OR REPLACE VIEW public.available_on_demand_deliveries AS
 SELECT d.id,
    d.client_id,
    d.from_address,
    d.from_lat,
    d.from_lng,
    d.to_address,
    d.to_lat,
    d.to_lng,
    d.distance_km,
    d.total_amount,
    d.item_description,
    d.created_at,
    u.username AS client_email,
    u.name AS client_name,
    CASE 
        WHEN u.phone_ddd IS NOT NULL AND u.phone_number IS NOT NULL THEN
            CASE 
                WHEN LENGTH(u.phone_number) = 9 THEN 
                    CONCAT('(', u.phone_ddd, ') ', SUBSTRING(u.phone_number, 1, 5), '-', SUBSTRING(u.phone_number, 6))
                WHEN LENGTH(u.phone_number) = 8 THEN 
                    CONCAT('(', u.phone_ddd, ') ', SUBSTRING(u.phone_number, 1, 4), '-', SUBSTRING(u.phone_number, 5))
                ELSE 
                    CONCAT('(', u.phone_ddd, ') ', u.phone_number)
            END
        ELSE NULL
    END AS client_phone
   FROM public.deliveries d
     JOIN public.users u ON u.id = d.client_id
  WHERE d.delivery_type::text = 'ON_DEMAND'::text 
    AND d.status::text = 'PENDING'::text 
    AND d.courier_id IS NULL
  ORDER BY d.created_at;

COMMENT ON VIEW public.available_on_demand_deliveries IS 'Entregas ON_DEMAND disponíveis para COURIERs aceitarem';

-- ============================================================================
-- ROLLBACK INSTRUCTIONS (if needed)
-- ============================================================================

-- To rollback this migration:
-- 
-- ALTER TABLE users ADD COLUMN phone VARCHAR(255);
-- 
-- -- Restore phone data from phone_ddd + phone_number:
-- UPDATE users
-- SET phone = CONCAT('(', phone_ddd, ') ', phone_number)
-- WHERE phone_ddd IS NOT NULL AND phone_number IS NOT NULL;
