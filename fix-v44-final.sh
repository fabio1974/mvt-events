#!/bin/bash

echo "=========================================="
echo "🔧 Fixing Migration V44 - Complete Fix"
echo "=========================================="
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Navigate to project directory
cd "$(dirname "$0")"

echo "📋 Step 1: Stopping application..."
echo ""
pkill -f "gradle.*bootRun" 2>/dev/null
pkill -f "mvt.*events" 2>/dev/null
sleep 2
echo "✅ Application stopped"

echo ""
echo "📋 Step 2: Cleaning database..."
echo ""

# Remove payments table and V44 from flyway history
docker exec -i mvt-events-db psql -U mvt -d mvt-events << 'EOF'
-- Drop payments table if exists
DROP TABLE IF EXISTS payments CASCADE;

-- Remove V44 from flyway history
DELETE FROM flyway_schema_history WHERE version = '44';

-- Verify
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;
EOF

if [ $? -eq 0 ]; then
    echo ""
    echo -e "${GREEN}✅ Database cleaned successfully${NC}"
else
    echo ""
    echo -e "${RED}❌ Failed to clean database${NC}"
    echo ""
    echo "Trying alternative container name..."
    docker exec -i mvt_events-db-1 psql -U mvt -d mvt-events << 'EOF'
DROP TABLE IF EXISTS payments CASCADE;
DELETE FROM flyway_schema_history WHERE version = '44';
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;
EOF
fi

echo ""
echo "📋 Step 3: Fixing migration V44 SQL..."
echo ""

# Backup original migration
cp src/main/resources/db/migration/V44__create_payments_table.sql src/main/resources/db/migration/V44__create_payments_table.sql.backup 2>/dev/null

# Create fixed migration
cat > src/main/resources/db/migration/V44__create_payments_table.sql << 'SQLEOF'
-- ============================================================================
-- Migration V44: Create payments table for deliveries
-- ============================================================================
-- Description: Creates payments table to track delivery payments
-- Author: System
-- Date: 2025-10-23
-- ============================================================================

-- ============================================================================
-- CREATE TABLE: payments
-- ============================================================================
CREATE TABLE IF NOT EXISTS payments (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Relationships
    delivery_id BIGINT NOT NULL,
    payer_id UUID NOT NULL,
    organization_id BIGINT,

    -- Payment Info
    transaction_id VARCHAR(100) UNIQUE,
    amount DECIMAL(10, 2) NOT NULL,
    payment_method VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_date TIMESTAMP,

    -- Provider Info
    provider VARCHAR(50),
    provider_payment_id VARCHAR(100),

    -- Metadata
    notes TEXT,
    metadata JSONB,

    -- Foreign Keys
    CONSTRAINT fk_payment_delivery FOREIGN KEY (delivery_id)
        REFERENCES deliveries(id) ON DELETE CASCADE,
    CONSTRAINT fk_payment_payer FOREIGN KEY (payer_id)
        REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_payment_organization FOREIGN KEY (organization_id)
        REFERENCES organizations(id) ON DELETE SET NULL,

    -- Constraints
    CONSTRAINT chk_payment_amount CHECK (amount > 0),
    CONSTRAINT chk_payment_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'REFUNDED', 'CANCELLED')),
    CONSTRAINT chk_payment_method CHECK (payment_method IN ('CREDIT_CARD', 'DEBIT_CARD', 'PIX', 'BANK_SLIP', 'CASH', 'WALLET'))
);

-- ============================================================================
-- CREATE INDEXES
-- ============================================================================
CREATE INDEX IF NOT EXISTS idx_payment_delivery ON payments(delivery_id);
CREATE INDEX IF NOT EXISTS idx_payment_payer ON payments(payer_id);
CREATE INDEX IF NOT EXISTS idx_payment_organization ON payments(organization_id);
CREATE INDEX IF NOT EXISTS idx_payment_status ON payments(status);
CREATE INDEX IF NOT EXISTS idx_payment_provider ON payments(provider);
CREATE INDEX IF NOT EXISTS idx_payment_date ON payments(payment_date);
CREATE INDEX IF NOT EXISTS idx_payment_transaction ON payments(transaction_id);

-- ============================================================================
-- COMMENTS
-- ============================================================================
COMMENT ON TABLE payments IS 'Pagamentos de entregas';
COMMENT ON COLUMN payments.delivery_id IS 'ID da entrega relacionada';
COMMENT ON COLUMN payments.payer_id IS 'ID do usuário que está pagando';
COMMENT ON COLUMN payments.organization_id IS 'ID da organização que receberá o pagamento';
COMMENT ON COLUMN payments.transaction_id IS 'ID único da transação (gerado internamente)';
COMMENT ON COLUMN payments.amount IS 'Valor do pagamento';
COMMENT ON COLUMN payments.payment_method IS 'Método de pagamento utilizado';
COMMENT ON COLUMN payments.status IS 'Status do pagamento';
COMMENT ON COLUMN payments.payment_date IS 'Data/hora em que o pagamento foi concluído';
COMMENT ON COLUMN payments.provider IS 'Provedor de pagamento (stripe, mercadopago, etc)';
COMMENT ON COLUMN payments.provider_payment_id IS 'ID do pagamento no provedor externo';
COMMENT ON COLUMN payments.metadata IS 'Dados adicionais em formato JSON';
SQLEOF

echo "✅ Migration V44 fixed with IF NOT EXISTS"

echo ""
echo "📋 Step 4: Cleaning build..."
echo ""
./gradlew clean --quiet

echo ""
echo "📋 Step 5: Starting application..."
echo ""
echo "Starting in background... Check logs with:"
echo "  tail -f app-boot.log"
echo ""

./gradlew bootRun > app-boot.log 2>&1 &
APP_PID=$!
echo $APP_PID > app.pid

echo ""
echo "=========================================="
echo -e "${GREEN}✅ Fix Complete!${NC}"
echo "=========================================="
echo ""
echo "Application PID: $APP_PID"
echo ""
echo "📊 Monitor startup:"
echo "  tail -f app-boot.log"
echo ""
echo "🔍 Check if running:"
echo "  curl http://localhost:8080/actuator/health"
echo ""
echo "🛑 Stop application:"
echo "  kill $APP_PID"
echo ""
