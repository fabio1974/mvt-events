#!/bin/bash

echo "========================================"
echo "Removendo Transfer (relacionado a Events)"
echo "========================================"
echo ""

echo "📋 Transfer é usado para transferências financeiras de EVENTOS"
echo "   Como removemos eventos, Transfer não é mais necessário."
echo ""

echo "1. Removendo arquivos Java..."
rm -f "src/main/java/com/mvt/mvt_events/jpa/Transfer.java"
echo "   ✅ Transfer.java removido"

rm -f "src/main/java/com/mvt/mvt_events/repository/TransferRepository.java"
echo "   ✅ TransferRepository.java removido"

echo ""
echo "2. Criando migration para dropar tabela transfers..."

cat > "src/main/resources/db/migration/V45__drop_transfers_table.sql" << 'EOF'
-- ============================================================================
-- Migration V45: Drop transfers table (related to removed Events system)
-- ============================================================================
-- Description: Removes transfers table as it was only used for Event-based
--              financial transfers. The Delivery system uses Payment entity instead.
-- Author: System
-- Date: 2025-10-23
-- ============================================================================

-- Drop indexes first
DROP INDEX IF EXISTS idx_transfers_event_id;
DROP INDEX IF EXISTS idx_transfers_tenant_id;
DROP INDEX IF EXISTS idx_transfers_tenant_event;

-- Drop table
DROP TABLE IF EXISTS transfers CASCADE;

-- Comments
COMMENT ON SCHEMA public IS 'transfers table removed - was related to Events system (now removed)';
EOF

echo "   ✅ Migration V45 criada"

echo ""
echo "3. Limpando build..."
./gradlew clean --quiet

echo ""
echo "4. Testando compilação..."
./gradlew compileJava --quiet

if [ $? -eq 0 ]; then
    echo ""
    echo "========================================"
    echo "✅ Transfer removido com sucesso!"
    echo "========================================"
    echo ""
    echo "Agora execute:"
    echo "  ./gradlew bootRun"
    echo ""
else
    echo ""
    echo "========================================"
    echo "❌ Erro na compilação"
    echo "========================================"
    echo ""
    echo "Execute manualmente:"
    echo "  ./gradlew compileJava"
    echo ""
fi
