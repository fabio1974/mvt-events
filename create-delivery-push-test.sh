#!/bin/bash

echo "🚚 Criando delivery para disparar notificação push REAL..."
echo ""

# Dados do motoboy com token real
MOTOBOY_ID="6186c7af-2311-4756-bfc6-ce98bd31ed27"
CLIENT_ID="189c7d79-cb21-40c1-9b7c-006ebaa3289a"

echo "📱 Motoboy ID: $MOTOBOY_ID"
echo "👤 Client ID: $CLIENT_ID"
echo ""

# Você precisa fornecer um JWT válido aqui
# Ou fazer login primeiro

read -p "Cole seu JWT token (ou Enter para tentar sem auth): " JWT_TOKEN

if [ -z "$JWT_TOKEN" ]; then
    echo "⚠️  Tentando sem autenticação (pode falhar)..."
    AUTH_HEADER=""
else
    AUTH_HEADER="-H \"Authorization: Bearer $JWT_TOKEN\""
fi

echo ""
echo "📤 Criando delivery..."
echo ""

RESPONSE=$(curl -s -X POST http://localhost:8080/api/deliveries \
    -H "Content-Type: application/json" \
    ${AUTH_HEADER:+-H "Authorization: Bearer $JWT_TOKEN"} \
    -d "{
        \"client\": \"$CLIENT_ID\",
        \"fromAddress\": \"R. Push Test REAL, 777 - Ubajara, CE\",
        \"fromLatitude\": -3.8710,
        \"fromLongitude\": -40.9163,
        \"toAddress\": \"R. Destino REAL, 888 - Ubajara, CE\",
        \"toLatitude\": -3.8669,
        \"toLongitude\": -40.9176,
        \"totalAmount\": 75.00,
        \"itemDescription\": \"🚀 TESTE REAL de Push - $(date +%H:%M:%S)\",
        \"recipientName\": \"Cliente Push Real\",
        \"recipientPhone\": \"85999999999\"
    }")

echo "Resposta:"
echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"

DELIVERY_ID=$(echo "$RESPONSE" | grep -o '"id":[0-9]*' | cut -d':' -f2 | head -1)

if [ ! -z "$DELIVERY_ID" ]; then
    echo ""
    echo "✅ Delivery criada com ID: $DELIVERY_ID"
    echo ""
    echo "📱 NOTIFICAÇÃO SENDO ENVIADA AGORA..."
    echo "   O sistema vai:"
    echo "   1. Buscar motoboys disponíveis"
    echo "   2. Enviar notificação via Expo Push API"
    echo "   3. Expo entrega no seu iPhone"
    echo ""
    echo "🔍 Acompanhe os logs:"
    echo "   tail -f app-boot-production.log | grep -E \"delivery.*$DELIVERY_ID|Notif|Push|Expo\""
    echo ""
    echo "📱 VERIFIQUE SEU IPHONE AGORA!"
else
    echo ""
    echo "❌ Erro ao criar delivery"
fi

echo ""
