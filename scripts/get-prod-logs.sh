#!/bin/bash

# 📋 Captura logs de produção no Render (CLI v2+ exige --resources <id do serviço>)
#   render services list   → coluna ID do Web Service mvt-events-api

set -e

echo "📋 Capturando logs de produção (Render)..."
echo "=========================================="
echo ""

# ID do Web Service (Production) — confira com: render services list
RENDER_WEB_SERVICE_ID="${RENDER_WEB_SERVICE_ID:-srv-d3718jje5dus738u58m0}"
OUTPUT_DIR="logs"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

mkdir -p "$OUTPUT_DIR"
ALL_LOGS="$OUTPUT_DIR/render-${TIMESTAMP}.log"

echo "🔍 Serviço: $RENDER_WEB_SERVICE_ID"
echo ""

# Render CLI v2: --resources obrigatório; --limit em vez de --tail numérico simples
render logs --resources "$RENDER_WEB_SERVICE_ID" --limit 2000 -o text > "$ALL_LOGS" 2>&1 || {
    echo "❌ Erro ao buscar logs. Certifique-se de estar autenticado:"
    echo "   render login"
    echo "   render services list   # confira o ID e export RENDER_WEB_SERVICE_ID=..."
    exit 1
}

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
echo "💡 Exemplos:"
echo ""
echo "   grep -E 'actual_route|Falha ao cortar|Delivery #' $ALL_LOGS"
echo "   render logs --resources $RENDER_WEB_SERVICE_ID --limit 300 --text actual_route -o text"
echo ""
