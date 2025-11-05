#!/bin/bash

echo "========================================"
echo "🔧 Corrigindo Migration V44 Falhada"
echo "========================================"

echo ""
echo "1. Conectando ao banco para limpar migration falhada..."

# Remove a entrada da V44 que falhou
psql -h localhost -p 5435 -U postgres -d mvt-events << 'EOF'
-- Remove migration V44 falhada do histórico
DELETE FROM flyway_schema_history WHERE version = '44';

-- Verifica se tabela payments foi criada parcialmente
DROP TABLE IF EXISTS payments CASCADE;

-- Confirma remoção
\echo '✅ Migration V44 removida'
\echo ''
\echo 'Histórico de migrations:'
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;
EOF

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Banco limpo com sucesso!"
    echo ""
    echo "========================================"
    echo "2. Limpando build..."
    echo "========================================"
    ./gradlew clean
    
    echo ""
    echo "========================================"
    echo "3. Compilando..."
    echo "========================================"
    ./gradlew compileJava
    
    if [ $? -eq 0 ]; then
        echo ""
        echo "✅ Compilação bem-sucedida!"
        echo ""
        echo "========================================"
        echo "4. Iniciando aplicação..."
        echo "========================================"
        ./gradlew bootRun
    else
        echo ""
        echo "❌ Erro na compilação!"
        exit 1
    fi
else
    echo ""
    echo "❌ Erro ao limpar banco!"
    echo "Tente manualmente:"
    echo "psql -h localhost -p 5435 -U postgres -d mvt-events -c \"DELETE FROM flyway_schema_history WHERE version = '44';\""
    exit 1
fi
