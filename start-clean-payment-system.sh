#!/bin/bash

# ============================================================================
# Script: start-clean-payment-system.sh
# Descrição: Inicia aplicação com sistema de pagamentos pronto
# ============================================================================

set -e

PROJECT_DIR="/Users/jose.barros.br/Documents/projects/mvt-events"
cd "$PROJECT_DIR"

echo "============================================"
echo "🚀 Iniciando Sistema de Pagamentos"
echo "============================================"
echo ""

# Step 1: Limpar processos
echo "📌 1/6: Parando processos antigos..."
pkill -f "mvt_events" 2>/dev/null || true
pkill -f "gradlew" 2>/dev/null || true
sleep 2
echo "   ✅ Processos parados"

# Step 2: Verificar Docker
echo ""
echo "📌 2/6: Iniciando banco de dados..."
docker compose up -d db
sleep 5
echo "   ✅ Banco de dados iniciado"

# Step 3: Limpar build
echo ""
echo "📌 3/6: Limpando build anterior..."
rm -rf build/ 2>/dev/null || true
./gradlew clean --no-daemon --quiet
echo "   ✅ Build limpo"

# Step 4: Compilar
echo ""
echo "📌 4/6: Compilando projeto..."
./gradlew compileJava --no-daemon --quiet

if [ $? -eq 0 ]; then
    echo "   ✅ Compilação bem-sucedida!"
else
    echo "   ❌ Erro na compilação!"
    echo ""
    echo "Executando diagnóstico..."
    grep -n "private PaymentRepository" src/main/java/com/mvt/mvt_events/service/UnifiedPayoutService.java || true
    exit 1
fi

# Step 5: Verificar estrutura
echo ""
echo "📌 5/6: Verificando estrutura do sistema..."
echo "   Payment.java:           $([ -f src/main/java/com/mvt/mvt_events/jpa/Payment.java ] && echo '✅' || echo '❌')"
echo "   PaymentStatus.java:     $([ -f src/main/java/com/mvt/mvt_events/jpa/PaymentStatus.java ] && echo '✅' || echo '❌')"
echo "   PaymentMethod.java:     $([ -f src/main/java/com/mvt/mvt_events/jpa/PaymentMethod.java ] && echo '✅' || echo '❌')"
echo "   PaymentRepository.java: $([ -f src/main/java/com/mvt/mvt_events/repository/PaymentRepository.java ] && echo '✅' || echo '❌')"
echo "   Migration V44:          $([ -f src/main/resources/db/migration/V44__create_payments_table.sql ] && echo '✅' || echo '❌')"

# Step 6: Iniciar aplicação
echo ""
echo "📌 6/6: Iniciando aplicação Spring Boot..."
echo ""
echo "============================================"
echo "⏳ INICIANDO..."
echo "============================================"
echo ""
echo "📝 Logs serão exibidos abaixo."
echo "   Para parar: Ctrl+C"
echo ""
echo "============================================"
echo ""

./gradlew bootRun --no-daemon
