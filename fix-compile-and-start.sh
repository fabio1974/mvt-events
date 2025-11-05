#!/bin/bash

# ============================================================================
# Script: fix-compile-and-start.sh
# Description: Corrige compilação e inicia aplicação
# ============================================================================

set -e

echo "============================================"
echo "🔧 Fix Compile and Start Application"
echo "============================================"
echo ""

# Diretório do projeto
PROJECT_DIR="/Users/jose.barros.br/Documents/projects/mvt-events"
cd "$PROJECT_DIR"

# Step 1: Parar processos
echo "📌 Step 1: Parando processos existentes..."
pkill -f "mvt_events" || true
pkill -f "gradlew" || true
sleep 2

# Step 2: Verificar Docker
echo ""
echo "📌 Step 2: Verificando Docker..."
if ! docker compose ps | grep -q "mvt_events-db.*running"; then
    echo "⚠️  Banco de dados não está rodando. Iniciando..."
    docker compose up -d db
    echo "⏳ Aguardando banco de dados iniciar..."
    sleep 5
else
    echo "✅ Banco de dados já está rodando"
fi

# Step 3: Clean build
echo ""
echo "📌 Step 3: Limpando build anterior..."
./gradlew clean

# Step 4: Compilar
echo ""
echo "📌 Step 4: Compilando projeto..."
./gradlew compileJava

if [ $? -eq 0 ]; then
    echo "✅ Compilação bem-sucedida!"
else
    echo "❌ Erro na compilação"
    exit 1
fi

# Step 5: Verificar migração V44
echo ""
echo "📌 Step 5: Verificando migração V44..."
MIGRATION_FILE="$PROJECT_DIR/src/main/resources/db/migration/V44__create_payments_table.sql"
if [ -f "$MIGRATION_FILE" ]; then
    echo "✅ Migração V44 existe"
    echo "📄 Primeiras 20 linhas:"
    head -20 "$MIGRATION_FILE"
else
    echo "❌ Migração V44 não encontrada"
fi

# Step 6: Verificar status do banco
echo ""
echo "📌 Step 6: Verificando status do banco de dados..."
docker exec mvt_events-db-1 psql -U postgres -d mvt_events_db -c "\d payments" 2>&1 || echo "⚠️  Tabela payments não existe ainda (será criada pela migração)"

# Step 7: Iniciar aplicação
echo ""
echo "📌 Step 7: Iniciando aplicação..."
echo "⏳ Iniciando Spring Boot..."
./gradlew bootRun > app-boot.log 2>&1 &
APP_PID=$!
echo $APP_PID > app.pid

echo ""
echo "============================================"
echo "✅ Aplicação iniciada em background"
echo "============================================"
echo "PID: $APP_PID"
echo ""
echo "📋 Comandos úteis:"
echo "   tail -f app-boot.log          # Ver logs em tempo real"
echo "   kill $APP_PID                 # Parar aplicação"
echo "   ./gradlew bootRun             # Rodar em foreground"
echo ""
echo "⏳ Aguardando 10 segundos para verificar logs..."
sleep 10

echo ""
echo "📄 Últimas 50 linhas do log:"
tail -50 app-boot.log

echo ""
echo "✅ Script concluído!"
