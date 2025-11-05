#!/bin/bash

echo "========================================"
echo "Corrigindo Migration V44..."
echo "========================================"

echo ""
echo "1. Conectando ao PostgreSQL e removendo V44 do histórico do Flyway..."
docker compose exec -T postgres psql -U mvt -d mvt-events << 'EOF'
DELETE FROM flyway_schema_history WHERE version = '44';
\q
EOF

echo "   ✅ Migration V44 removida do histórico"

echo ""
echo "2. Limpando build..."
./gradlew clean

echo ""
echo "3. Compilando..."
./gradlew compileJava

echo ""
echo "4. Iniciando aplicação (executará V44 novamente)..."
./gradlew bootRun
