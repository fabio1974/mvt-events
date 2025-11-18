#!/bin/bash

# Script para testar envio de notificação push via backend
# Usa o endpoint do backend que chama o Expo Push Service

set -e

echo "📱 Teste de Notificação Push - MVT Events"
echo "=========================================="
echo ""

# Cores
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# Verificar se backend está rodando
echo "1️⃣  Verificando se backend está rodando..."
if curl -s http://localhost:8080/actuator/health | grep -q "UP"; then
    echo -e "${GREEN}✅ Backend está rodando${NC}"
else
    echo -e "${RED}❌ Backend não está rodando!${NC}"
    exit 1
fi

echo ""

# Buscar usuário motoboy com token ativo
echo "2️⃣  Buscando usuário motoboy com token push ativo..."
MOTOBOY_INFO=$(docker exec -it mvt-events-db psql -U mvt -d mvt-events -t -c "
    SELECT 
        u.id,
        u.username,
        pt.token,
        pt.device_type
    FROM users u
    INNER JOIN user_push_tokens pt ON pt.user_id = u.id
    WHERE u.role = 'COURIER'
    AND pt.is_active = true
    LIMIT 1;
" | xargs)

if [ -z "$MOTOBOY_INFO" ]; then
    echo -e "${RED}❌ Nenhum motoboy com token push ativo encontrado!${NC}"
    echo ""
    echo "Para registrar um token:"
    echo "1. Abra o app mobile no iPhone"
    echo "2. Faça login como motoboy"
    echo "3. Aceite permissões de notificação"
    exit 1
fi

MOTOBOY_ID=$(echo $MOTOBOY_INFO | awk '{print $2}')
MOTOBOY_USERNAME=$(echo $MOTOBOY_INFO | awk '{print $4}')
PUSH_TOKEN=$(echo $MOTOBOY_INFO | awk '{print $6}')

echo -e "${GREEN}✅ Motoboy encontrado:${NC}"
echo "   ID: $MOTOBOY_ID"
echo "   Username: $MOTOBOY_USERNAME"
echo "   Token: ${PUSH_TOKEN:0:50}..."
echo ""

# Fazer login para obter JWT
echo "3️⃣  Fazendo login como admin para obter JWT..."
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/users/login \
    -H "Content-Type: application/json" \
    -d '{"username":"moveltrack@gmail.com","password":"senha123"}')

JWT_TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$JWT_TOKEN" ]; then
    echo -e "${RED}❌ Falha ao obter JWT token${NC}"
    echo "Response: $LOGIN_RESPONSE"
    exit 1
fi

echo -e "${GREEN}✅ JWT obtido${NC}"
echo ""

# Menu de opções
echo "Escolha o tipo de notificação:"
echo ""
echo "1️⃣  Notificação de teste simples"
echo "2️⃣  Notificação de nova entrega (delivery invite)"
echo "3️⃣  Criar delivery real (dispara notificação automaticamente)"
echo ""
read -p "Digite a opção (1-3): " OPTION

echo ""

case $OPTION in
    1)
        echo "📤 Enviando notificação de teste..."
        
        # Chamar endpoint de teste do backend
        RESPONSE=$(curl -s -X POST "http://localhost:8080/api/push/test/$MOTOBOY_ID" \
            -H "Authorization: Bearer $JWT_TOKEN" \
            -H "Content-Type: application/json")
        
        echo ""
        echo "Resposta do backend:"
        echo "$RESPONSE"
        ;;
        
    2)
        echo "📤 Enviando notificação de nova entrega..."
        
        # Endpoint customizado para enviar notificação de entrega
        RESPONSE=$(curl -s -X POST "http://localhost:8080/api/push/send-delivery-notification" \
            -H "Authorization: Bearer $JWT_TOKEN" \
            -H "Content-Type: application/json" \
            -d "{
                \"userId\": \"$MOTOBOY_ID\",
                \"title\": \"🚚 Nova Entrega Disponível!\",
                \"body\": \"Entrega de R$ 45,00 próxima a você\",
                \"data\": {
                    \"type\": \"delivery_invite\",
                    \"deliveryId\": \"999\",
                    \"message\": \"Teste de notificação manual\",
                    \"clientName\": \"Cliente Teste\",
                    \"value\": \"45.00\",
                    \"address\": \"R. Teste, 123\"
                }
            }")
        
        echo ""
        echo "Resposta do backend:"
        echo "$RESPONSE"
        ;;
        
    3)
        echo "🚚 Criando delivery real..."
        
        # Buscar cliente
        CLIENT_ID="189c7d79-cb21-40c1-9b7c-006ebaa3289a"
        
        # Criar delivery
        DELIVERY_RESPONSE=$(curl -s -X POST http://localhost:8080/api/deliveries \
            -H "Authorization: Bearer $JWT_TOKEN" \
            -H "Content-Type: application/json" \
            -d "{
                \"client\": \"$CLIENT_ID\",
                \"fromAddress\": \"R. Teste Push, 100 - Ubajara, CE\",
                \"fromLatitude\": -3.8710,
                \"fromLongitude\": -40.9163,
                \"toAddress\": \"R. Destino Push, 200 - Ubajara, CE\",
                \"toLatitude\": -3.8669,
                \"toLongitude\": -40.9176,
                \"totalAmount\": 55.00,
                \"itemDescription\": \"Teste REAL de notificação push - $(date +%H:%M:%S)\",
                \"recipientName\": \"Cliente Push Test\",
                \"recipientPhone\": \"85999999999\"
            }")
        
        DELIVERY_ID=$(echo "$DELIVERY_RESPONSE" | grep -o '"id":[0-9]*' | cut -d':' -f2 | head -1)
        
        if [ ! -z "$DELIVERY_ID" ]; then
            echo ""
            echo -e "${GREEN}✅ Delivery criada com ID: $DELIVERY_ID${NC}"
            echo ""
            echo "📱 Notificação será enviada automaticamente em background..."
            echo "   O sistema segue o fluxo de 3 níveis:"
            echo "   - Nível 1: Motoboys da organização titular"
            echo "   - Nível 2: Motoboys de outras organizações (após 2min)"
            echo "   - Nível 3: Todos motoboys próximos (após 4min)"
            echo ""
            echo "🔍 Acompanhe os logs:"
            echo "   tail -f app-boot-production.log | grep -i \"notif\|push\|delivery.*$DELIVERY_ID\""
        else
            echo -e "${RED}❌ Erro ao criar delivery${NC}"
            echo "$DELIVERY_RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$DELIVERY_RESPONSE"
        fi
        ;;
        
    *)
        echo -e "${RED}❌ Opção inválida!${NC}"
        exit 1
        ;;
esac

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "📱 Verifique seu iPhone!"
echo ""
echo "Se a notificação NÃO chegou, verifique:"
echo "1. Token no banco é válido (ExponentPushToken[...] não DEV_)"
echo "2. Permissões de notificação estão ativas no iPhone"
echo "3. Token Expo configurado no backend é válido"
echo "4. Logs do backend para erros"
echo ""
echo "Para ver logs em tempo real:"
echo -e "${BLUE}tail -f app-boot-production.log | grep -i \"push\|notif\"${NC}"
echo ""
