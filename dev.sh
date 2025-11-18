#!/bin/bash
# Script de desenvolvimento com auto-reload
# Mata processos existentes e inicia aplicação em modo desenvolvimento

echo "🛑 Parando aplicação..."
pkill -f "gradlew bootRun" 2>/dev/null
sleep 2

echo "🚀 Iniciando aplicação em modo desenvolvimento..."
./gradlew bootRun --continuous &

echo "✅ Aplicação iniciada!"
echo "📝 Logs: tail -f app-boot.log"
echo "🔄 Para reiniciar: ./dev.sh"
echo "🛑 Para parar: pkill -f 'gradlew bootRun'"
