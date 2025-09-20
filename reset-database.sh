#!/bin/bash

# Script para resetar o banco de dados e recriar com BIGINT IDs

echo "🧹 Resetando banco de dados MVT Events..."

# 1. Conectar no PostgreSQL e dropar/recriar database
psql -h localhost -p 5435 -U postgres -c "DROP DATABASE IF EXISTS \"mvt-events\";"
psql -h localhost -p 5435 -U postgres -c "CREATE DATABASE \"mvt-events\";"

echo "✅ Database recriado"

# 2. Limpar histórico do Flyway
rm -rf build/classes/main/db/migration/*.class 2>/dev/null || true

echo "✅ Cache do Flyway limpo"

# 3. Executar aplicação para aplicar migrations
echo "🚀 Iniciando aplicação para aplicar migrations..."

echo "✅ Reset completo! Database limpo e pronto para BIGINT IDs"
echo "💡 Execute: ./gradlew bootRun para aplicar as migrations atualizadas"