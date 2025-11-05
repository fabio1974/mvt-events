#!/bin/bash

echo "=========================================="
echo "🔧 Corrigindo Migration V44"
echo "=========================================="

echo ""
echo "1️⃣ Removendo migration V44 falhada do Flyway..."
PGPASSWORD=postgres psql -h localhost -p 5435 -U postgres -d mvt-events -c "DELETE FROM flyway_schema_history WHERE version = '44' AND success = false;"

if [ $? -eq 0 ]; then
    echo "   ✅ Migration V44 removida do histórico"
else
    echo "   ⚠️  Aviso: Não foi possível remover (pode não existir)"
fi

echo ""
echo "2️⃣ Limpando build..."
./gradlew clean

echo ""
echo "3️⃣ Compilando código..."
./gradlew compileJava

if [ $? -ne 0 ]; then
    echo ""
    echo "❌ Erro na compilação!"
    exit 1
fi

echo ""
echo "4️⃣ Iniciando aplicação (migration V44 será executada)..."
echo ""
echo "=========================================="
echo "🚀 Aguarde... aplicação iniciando..."
echo "=========================================="
echo ""

./gradlew bootRun
