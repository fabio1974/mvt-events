#!/bin/bash

# Script para testar notificações push FCM/Expo
# Uso: ./test-push-notification.sh [EXPO_TOKEN] [JWT_TOKEN]

set -e

echo "🚀 MVT Events - Teste de Notificações Push"
echo "=========================================="
echo ""

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Verificar se app está rodando
echo "📡 Verificando se a aplicação está rodando..."
if curl -s http://localhost:8080/actuator/health | grep -q "UP"; then
    echo -e "${GREEN}✅ Aplicação está rodando${NC}"
else
    echo -e "${RED}❌ Aplicação não está rodando!${NC}"
    echo "Execute: ./gradlew bootRun"
    exit 1
fi

echo ""

# Token Expo (pode ser passado como argumento ou usar padrão de teste)
EXPO_TOKEN=${1:-"ExpoAccessToken[development-test-token-for-local]"}
JWT_TOKEN=${2:-""}

if [ -z "$JWT_TOKEN" ]; then
    echo -e "${YELLOW}⚠️  JWT Token não fornecido. Obtendo token de admin...${NC}"
    
    # Fazer login para obter JWT
    LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/users/login \
        -H "Content-Type: application/json" \
        -d '{"username":"moveltrack@gmail.com","password":"senha123"}')
    
    JWT_TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"token":"[^"]*' | cut -d'"' -f4)
    
    if [ -z "$JWT_TOKEN" ]; then
        echo -e "${RED}❌ Falha ao obter JWT token${NC}"
        echo "Response: $LOGIN_RESPONSE"
        exit 1
    fi
    
    echo -e "${GREEN}✅ JWT Token obtido${NC}"
fi

echo ""
echo "🔧 Configuração:"
echo "   Expo Token: ${EXPO_TOKEN:0:30}..."
echo "   JWT Token: ${JWT_TOKEN:0:30}..."
echo ""

# Menu de opções
echo "Escolha uma opção:"
echo ""
echo "1️⃣  Registrar token de dispositivo móvel"
echo "2️⃣  Criar delivery para disparar notificação"
echo "3️⃣  Verificar tokens cadastrados"
echo "4️⃣  Enviar notificação de teste direta"
echo "5️⃣  Verificar logs de notificações"
echo "6️⃣  Teste completo (registrar + criar delivery)"
echo ""
read -p "Digite a opção (1-6): " OPTION

echo ""

case $OPTION in
    1)
        echo "📱 Registrando token de dispositivo..."
        
        read -p "Digite o token do dispositivo (ou Enter para token de teste): " DEVICE_TOKEN
        DEVICE_TOKEN=${DEVICE_TOKEN:-"ExpoToken[DEV_$(date +%s)_test]"}
        
        read -p "Nome do dispositivo (ou Enter para padrão): " DEVICE_NAME
        DEVICE_NAME=${DEVICE_NAME:-"iPhone Test"}
        
        RESPONSE=$(curl -s -X POST http://localhost:8080/api/push/register \
            -H "Authorization: Bearer $JWT_TOKEN" \
            -H "Content-Type: application/json" \
            -d "{
                \"token\": \"$DEVICE_TOKEN\",
                \"deviceType\": \"MOBILE\",
                \"deviceName\": \"$DEVICE_NAME\"
            }")
        
        echo ""
        echo "Resposta:"
        echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"
        ;;
        
    2)
        echo "🚚 Criando delivery para disparar notificação..."
        
        # Buscar um cliente para usar
        echo "Buscando cliente..."
        CLIENT_ID="189c7d79-cb21-40c1-9b7c-006ebaa3289a"
        
        RESPONSE=$(curl -s -X POST http://localhost:8080/api/deliveries \
            -H "Authorization: Bearer $JWT_TOKEN" \
            -H "Content-Type: application/json" \
            -d "{
                \"client\": \"$CLIENT_ID\",
                \"fromAddress\": \"R. Teste, 123 - Ubajara, CE\",
                \"fromLatitude\": -3.8710,
                \"fromLongitude\": -40.9163,
                \"toAddress\": \"R. Destino, 456 - Ubajara, CE\",
                \"toLatitude\": -3.8669,
                \"toLongitude\": -40.9176,
                \"totalAmount\": 35.00,
                \"itemDescription\": \"Teste de notificação push $(date +%H:%M:%S)\",
                \"recipientName\": \"Cliente Teste\",
                \"recipientPhone\": \"85999999999\"
            }")
        
        echo ""
        echo "Resposta:"
        echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"
        
        DELIVERY_ID=$(echo "$RESPONSE" | grep -o '"id":[0-9]*' | cut -d':' -f2 | head -1)
        
        if [ ! -z "$DELIVERY_ID" ]; then
            echo ""
            echo -e "${GREEN}✅ Delivery criada com ID: $DELIVERY_ID${NC}"
            echo ""
            echo "📱 Notificações sendo enviadas em background..."
            echo "   Verifique os logs com: tail -f app.log | grep -i notif"
        fi
        ;;
        
    3)
        echo "🔍 Verificando tokens cadastrados..."
        
        docker exec -it mvt-events-db psql -U mvt -d mvt-events -c "
            SELECT 
                id, 
                user_id, 
                device_type, 
                device_name,
                is_active,
                substring(token, 1, 40) as token_preview,
                created_at 
            FROM user_push_tokens 
            WHERE is_active = true 
            ORDER BY created_at DESC 
            LIMIT 10;
        "
        ;;
        
    4)
        echo "📤 Enviando notificação de teste direta..."
        
        # Buscar primeiro token ativo
        TOKEN_INFO=$(docker exec -it mvt-events-db psql -U mvt -d mvt-events -t -c "
            SELECT user_id FROM user_push_tokens WHERE is_active = true LIMIT 1;
        " | xargs)
        
        if [ -z "$TOKEN_INFO" ]; then
            echo -e "${RED}❌ Nenhum token ativo encontrado!${NC}"
            echo "Execute a opção 1 primeiro para registrar um token."
            exit 1
        fi
        
        echo "Usuário alvo: $TOKEN_INFO"
        
        RESPONSE=$(curl -s -X POST "http://localhost:8080/api/push/test/$TOKEN_INFO" \
            -H "Authorization: Bearer $JWT_TOKEN")
        
        echo ""
        echo "Resposta:"
        echo "$RESPONSE"
        ;;
        
    5)
        echo "📋 Verificando logs de notificações..."
        echo ""
        echo "Últimas 50 linhas com 'notif', 'push' ou 'expo':"
        echo "================================================"
        
        tail -50 app.log 2>/dev/null | grep -i "notif\|push\|expo" || echo "Nenhum log encontrado"
        ;;
        
    6)
        echo "🎯 Executando teste completo..."
        echo ""
        
        # Passo 1: Registrar token
        echo "1️⃣  Registrando token de dispositivo..."
        DEVICE_TOKEN="ExpoToken[TEST_$(date +%s)_complete]"
        
        curl -s -X POST http://localhost:8080/api/push/register \
            -H "Authorization: Bearer $JWT_TOKEN" \
            -H "Content-Type: application/json" \
            -d "{
                \"token\": \"$DEVICE_TOKEN\",
                \"deviceType\": \"MOBILE\",
                \"deviceName\": \"Test Device - Complete\"
            }" > /dev/null
        
        echo -e "${GREEN}✅ Token registrado${NC}"
        echo ""
        
        # Passo 2: Criar delivery
        echo "2️⃣  Criando delivery..."
        CLIENT_ID="189c7d79-cb21-40c1-9b7c-006ebaa3289a"
        
        DELIVERY_RESPONSE=$(curl -s -X POST http://localhost:8080/api/deliveries \
            -H "Authorization: Bearer $JWT_TOKEN" \
            -H "Content-Type: application/json" \
            -d "{
                \"client\": \"$CLIENT_ID\",
                \"fromAddress\": \"R. Teste Completo, 789 - Ubajara, CE\",
                \"fromLatitude\": -3.8710,
                \"fromLongitude\": -40.9163,
                \"toAddress\": \"R. Destino Completo, 101 - Ubajara, CE\",
                \"toLatitude\": -3.8669,
                \"toLongitude\": -40.9176,
                \"totalAmount\": 45.00,
                \"itemDescription\": \"Teste completo $(date +%H:%M:%S)\",
                \"recipientName\": \"Cliente Teste Completo\",
                \"recipientPhone\": \"85999999999\"
            }")
        
        DELIVERY_ID=$(echo "$DELIVERY_RESPONSE" | grep -o '"id":[0-9]*' | cut -d':' -f2 | head -1)
        
        if [ ! -z "$DELIVERY_ID" ]; then
            echo -e "${GREEN}✅ Delivery criada: ID $DELIVERY_ID${NC}"
        else
            echo -e "${RED}❌ Erro ao criar delivery${NC}"
            echo "$DELIVERY_RESPONSE"
            exit 1
        fi
        
        echo ""
        
        # Passo 3: Aguardar e verificar logs
        echo "3️⃣  Aguardando processamento (5 segundos)..."
        sleep 5
        
        echo ""
        echo "4️⃣  Verificando logs de notificação:"
        echo "====================================="
        tail -30 app.log 2>/dev/null | grep -i "delivery.*$DELIVERY_ID\|notif\|push" | tail -15
        
        echo ""
        echo -e "${GREEN}✅ Teste completo executado!${NC}"
        echo ""
        echo "📱 Verifique seu dispositivo para confirmar recebimento da notificação."
        ;;
        
    *)
        echo -e "${RED}❌ Opção inválida!${NC}"
        exit 1
        ;;
esac

echo ""
echo "✨ Concluído!"
