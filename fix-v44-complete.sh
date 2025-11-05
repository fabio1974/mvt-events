#!/bin/bash

echo "================================================"
echo "🔧 Correção da Migration V44 - Payment System"
echo "================================================"

echo ""
echo "📊 1. Removendo migration V44 do histórico Flyway..."
psql -h localhost -p 5435 -U postgres -d mvt-events << EOF
-- Remover a migration V44 que falhou
DELETE FROM flyway_schema_history WHERE version = '44';
EOF

if [ $? -eq 0 ]; then
    echo "   ✅ Migration V44 removida do histórico"
else
    echo "   ❌ Erro ao remover migration do histórico"
    exit 1
fi

echo ""
echo "🗑️ 2. Removendo tabela payments (se existir)..."
psql -h localhost -p 5435 -U postgres -d mvt-events << EOF
-- Remover tabela payments se existir
DROP TABLE IF EXISTS payments CASCADE;
EOF

echo "   ✅ Tabela payments removida"

echo ""
echo "🔨 3. Limpando e compilando..."
./gradlew clean compileJava

if [ $? -eq 0 ]; then
    echo "   ✅ Compilação bem-sucedida"
else
    echo "   ❌ Erro na compilação"
    exit 1
fi

echo ""
echo "================================================"
echo "✅ Correção completa!"
echo "================================================"
echo ""
echo "Agora execute: ./start-app.sh"
echo ""
