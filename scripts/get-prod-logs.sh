#!/bin/bash

# 📋 Script para capturar logs de produção do Render
# Captura todos os logs recentes para análise

set -e

echo "📋 Capturando logs de produção (Render)..."
echo "=========================================="
echo ""

# Configurações
SERVICE_NAME="mvt-events-api"
OUTPUT_DIR="logs"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Criar diretório de logs se não existir
mkdir -p "$OUTPUT_DIR"

# Arquivo de saída
ALL_LOGS="$OUTPUT_DIR/render-${TIMESTAMP}.log"

echo "🔍 Buscando logs (últimos 30 minutos)..."
echo ""

# Buscar logs dos últimos 30 minutos
# render logs suporta --since para filtrar por tempo
render logs "$SERVICE_NAME" --since 30m > "$ALL_LOGS" 2>&1 || {
    echo "❌ Erro ao buscar logs. Certifique-se de estar autenticado:"
    echo "   render login"
    echo ""
    echo "💡 Tentando buscar últimos 2000 logs como fallback..."
    render logs "$SERVICE_NAME" --tail 2000 > "$ALL_LOGS" 2>&1 || exit 1
}

# Contar linhas
LINE_COUNT=$(wc -l < "$ALL_LOGS")
FILE_SIZE=$(du -h "$ALL_LOGS" | cut -f1)

echo "✅ Logs capturados com sucesso!"
echo ""
echo "=========================================="
echo "📊 RESUMO"
echo "=========================================="
echo ""
echo "📁 Arquivo: $ALL_LOGS"
echo "📊 Linhas: $LINE_COUNT"
echo "💾 Tamanho: $FILE_SIZE"
echo ""

# Mostrar últimas 50 linhas
echo "=========================================="
echo "📄 ÚLTIMAS 50 LINHAS"
echo "=========================================="
echo ""
tail -50 "$ALL_LOGS"
echo ""

echo "=========================================="
echo "✅ Logs salvos para análise!"
echo "=========================================="
echo ""
echo "💡 Comandos úteis para análise:"
echo ""
echo "   # Ver tudo:"
echo "   cat $ALL_LOGS"
echo ""
echo "   # Buscar usuário específico:"
echo "   cat $ALL_LOGS | grep 'jsfb1974'"
echo ""
echo "   # Buscar push/token:"
echo "   cat $ALL_LOGS | grep -i 'push\\|token'"
echo ""
echo "   # Buscar erros:"
echo "   cat $ALL_LOGS | grep -i 'error\\|exception'"
echo ""
echo "   # Abrir no editor:"
echo "   code $ALL_LOGS"
echo ""
