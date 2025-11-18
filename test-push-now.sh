#!/bin/bash

# TESTE RÁPIDO - Enviar notificação push AGORA

echo "🚀 Enviando notificação push de teste..."
echo ""

# 1. Login
echo "1️⃣  Fazendo login..."
LOGIN=$(curl -s -X POST http://localhost:8080/api/users/login \
    -H "Content-Type: application/json" \
    -d '{"username":"moveltrack@gmail.com","password":"senha123"}')

JWT=$(echo $LOGIN | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$JWT" ]; then
    echo "❌ Falha no login!"
    exit 1
fi

echo "✅ Login OK"
echo ""

# 2. Buscar motoboy
echo "2️⃣  Buscando motoboy..."
MOTOBOY_ID="6186c7af-2311-4756-bfc6-ce98bd31ed27"
echo "✅ Motoboy ID: $MOTOBOY_ID"
echo ""

# 3. Enviar notificação
echo "3️⃣  Enviando notificação push..."
echo ""

RESPONSE=$(curl -s -X POST "http://localhost:8080/api/users/$MOTOBOY_ID/test-notification" \
    -H "Authorization: Bearer $JWT" \
    -H "Content-Type: application/json" \
    -d '{
        "title": "🚚 TESTE - Nova Entrega!",
        "body": "Entrega de R$ 50,00 - Cliente Teste Push"
    }')

echo "Resposta:"
echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "📱 VERIFIQUE SEU IPHONE AGORA!"
echo ""
echo "Se não chegou, verifique:"
echo "1. Token no app é REAL (não DEV_)"
echo "2. App está aberto/em background"
echo "3. Permissões de notificação ativas"
echo ""
echo "Ver logs:"
echo "tail -f app-boot-production.log | grep -i push"
echo ""
