#!/bin/bash

echo "🏥 Health Check - Porta 8080"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Tenta o endpoint Spring Boot Actuator
HEALTH_URL="http://localhost:8080/actuator/health"

echo "📡 Verificando: $HEALTH_URL"
echo ""

# Faz a requisição com timeout de 5 segundos
HTTP_CODE=$(curl -s -o /tmp/health-response.json -w "%{http_code}" --connect-timeout 5 --max-time 5 "$HEALTH_URL" 2>&1)
HTTP_BODY=$(cat /tmp/health-response.json 2>/dev/null)

if [ "$HTTP_CODE" = "200" ]; then
    echo "✅ Aplicação ONLINE"
    echo ""
    echo "📊 Status:"
    echo "$HTTP_BODY" | jq '.' 2>/dev/null || echo "$HTTP_BODY"
    echo ""
    exit 0
elif [ "$HTTP_CODE" = "503" ]; then
    echo "⚠️  Aplicação DEGRADED (código 503)"
    echo ""
    echo "📊 Status:"
    echo "$HTTP_BODY" | jq '.' 2>/dev/null || echo "$HTTP_BODY"
    echo ""
    exit 1
elif [ "$HTTP_CODE" = "000" ] || [ -z "$HTTP_CODE" ]; then
    echo "❌ Aplicação OFFLINE"
    echo ""
    echo "🔍 Verificando processos Java na porta 8080..."
    PID=$(lsof -ti:8080)
    if [ ! -z "$PID" ]; then
        echo "⚠️  Processo encontrado (PID: $PID) mas não responde"
    else
        echo "⚠️  Nenhum processo rodando na porta 8080"
    fi
    echo ""
    exit 1
else
    echo "⚠️  Status inesperado: HTTP $HTTP_CODE"
    echo ""
    echo "📊 Resposta:"
    echo "$HTTP_BODY"
    echo ""
    exit 1
fi
