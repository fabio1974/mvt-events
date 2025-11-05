#!/bin/bash
# Script para rodar migrations e verificar resultado

echo "=========================================="
echo "🚀 Iniciando aplicação e migrations..."
echo "=========================================="
echo ""

cd /Users/jose.barros.br/Documents/projects/mvt-events

# Rodar aplicação e filtrar apenas logs relevantes
./gradlew bootRun 2>&1 | grep -E "(Migration|V40|V41|employment_contract|contracts|Flyway|Started|ERROR|Exception)" &

# Guardar PID do processo
PID=$!

echo "Processo iniciado com PID: $PID"
echo ""
echo "⏳ Aguardando migrations..."
echo "Pressione Ctrl+C para parar"
echo ""

# Aguardar
wait $PID
