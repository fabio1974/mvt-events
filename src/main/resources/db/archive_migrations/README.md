# Archived Migrations

This directory contains the **original incremental migrations (V01-V81)** that were consolidated into a single baseline migration **V1__baseline_initial_schema.sql** on **December 1, 2025**.

## Why Were These Archived?

1. **Simplification**: 75+ migrations were consolidated into a single baseline for easier maintenance
2. **Performance**: Faster initial database setup for new environments
3. **Clarity**: Single source of truth for current schema state
4. **Organization refactoring**: Final migration included removal of User.organization field in favor of Organization.owner

## Important Notes

⚠️ **DO NOT DELETE**: These files are kept for historical reference and audit purposes.

⚠️ **DO NOT USE**: New environments should use V1__baseline_initial_schema.sql instead.

⚠️ **Existing Environments**: Production databases that already ran these migrations are NOT affected. Flyway will recognize they've already been applied.

## Migration History Summary

- **V01-V20**: Initial system setup, events, registrations
- **V21-V40**: Tenancy improvements, delivery system core
- **V41-V60**: Contract system, RLS policies, organization features
- **V61-V70**: Owner field addition, organizer role changes
- **V71-V81**: Final refinements, organization_id removal from users

## Key Schema Changes in Final State

### User ↔ Organization Relationship
- **OLD**: `users.organization_id` → `organizations.id` (Many-to-One)
- **NEW**: `organizations.owner_id` → `users.id` (Reverse relationship)

### Tenant Filtering
- Users are now linked to organizations via:
  - `EmploymentContract` (for COURIERs)
  - `ClientContract` (for CLIENTs)  
  - `Organization.owner` (for ORGANIZERs)

### Core Tables in Final Schema
1. `users` - All system users (ADMIN, ORGANIZER, COURIER, CLIENT)
2. `organizations` - Tenant organizations with owner_id
3. `deliveries` - Core delivery entities
4. `employment_contracts` - COURIER ↔ Organization links
5. `client_contracts` - CLIENT ↔ Organization links
6. `cities` - Brazilian cities reference data
7. `evaluations` - Delivery ratings
8. `payments` - Payment tracking
9. `push_subscriptions` - Web push notifications

## Restoration Process (If Needed)

If you need to recreate the incremental migration path:

1. Copy all files from this directory back to parent
2. Delete V1__baseline_initial_schema.sql
3. Restart with fresh database
4. Let Flyway run all migrations sequentially

## Contact

For questions about migration history, consult:
- Git history: `git log src/main/resources/db/migration/`
- Database schema docs: `docs/` directory
- Application logs: Migration V81 included the final organization refactoring

---
**Generated**: 2025-12-01  
**Consolidation Version**: V1 (replaces V01-V81)
