#!/bin/bash

# Script para obter e configurar token Expo
# Este script ajuda a configurar o token Expo no projeto

echo "🔐 Configuração de Token Expo"
echo "=============================="
echo ""

# Cores
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "Para obter um token válido do Expo, você tem 3 opções:"
echo ""
echo -e "${BLUE}Opção 1: Token de Acesso da Conta Expo (Produção)${NC}"
echo "   1. Acesse: https://expo.dev/signup"
echo "   2. Crie/faça login na sua conta"
echo "   3. Vá em: Settings → Access Tokens"
echo "   4. Clique em 'Create Token'"
echo "   5. Copie o token (formato: ExpoAccessToken[...])"
echo ""

echo -e "${BLUE}Opção 2: Token do Dispositivo (Desenvolvimento/Testes)${NC}"
echo "   1. No seu app React Native/Expo, use:"
echo "      import * as Notifications from 'expo-notifications';"
echo "      const token = await Notifications.getExpoPushTokenAsync();"
echo "   2. Registre esse token no backend via API"
echo "   3. Use para testes locais"
echo ""

echo -e "${BLUE}Opção 3: Modo Simulação (Apenas Logs)${NC}"
echo "   - Já está configurado!"
echo "   - Token atual: development-test-token-for-local"
echo "   - Logs simulados com 🧪"
echo ""

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

read -p "Digite o token Expo (ou Enter para manter modo simulação): " EXPO_TOKEN

if [ -z "$EXPO_TOKEN" ]; then
    echo ""
    echo -e "${YELLOW}ℹ️  Mantendo modo simulação${NC}"
    echo "   As notificações serão simuladas nos logs."
    exit 0
fi

echo ""
echo "🔧 Configurando token..."

# Verificar formato do token
if [[ $EXPO_TOKEN == ExpoAccessToken* ]] || [[ $EXPO_TOKEN == ExponentPushToken* ]]; then
    echo -e "${GREEN}✅ Formato de token válido${NC}"
else
    echo -e "${YELLOW}⚠️  Aviso: Token não parece estar no formato esperado${NC}"
    echo "   Formato esperado: ExpoAccessToken[...] ou ExponentPushToken[...]"
    read -p "Continuar mesmo assim? (s/N): " CONFIRM
    if [[ $CONFIRM != "s" ]] && [[ $CONFIRM != "S" ]]; then
        exit 1
    fi
fi

# Opções de configuração
echo ""
echo "Como você quer configurar?"
echo "1. Variável de ambiente (temporário - esta sessão)"
echo "2. Arquivo .env (persistente)"
echo "3. application.properties (persistente)"
echo ""
read -p "Escolha (1-3): " CONFIG_OPTION

case $CONFIG_OPTION in
    1)
        echo ""
        echo "Execute o seguinte comando:"
        echo ""
        echo -e "${GREEN}export EXPO_ACCESS_TOKEN=\"$EXPO_TOKEN\"${NC}"
        echo -e "${GREEN}./gradlew bootRun${NC}"
        echo ""
        ;;
        
    2)
        ENV_FILE=".env"
        
        # Verificar se arquivo existe
        if [ -f "$ENV_FILE" ]; then
            # Remover linha antiga se existir
            sed -i '/^EXPO_ACCESS_TOKEN=/d' "$ENV_FILE"
        fi
        
        # Adicionar nova linha
        echo "EXPO_ACCESS_TOKEN=$EXPO_TOKEN" >> "$ENV_FILE"
        
        echo ""
        echo -e "${GREEN}✅ Token adicionado ao $ENV_FILE${NC}"
        echo ""
        echo "Para carregar as variáveis, execute:"
        echo -e "${GREEN}source .env && ./gradlew bootRun${NC}"
        echo ""
        ;;
        
    3)
        PROPS_FILE="src/main/resources/application.properties"
        
        # Backup
        cp "$PROPS_FILE" "$PROPS_FILE.backup"
        
        # Substituir token
        sed -i "s|expo.access-token=.*|expo.access-token=$EXPO_TOKEN|" "$PROPS_FILE"
        
        echo ""
        echo -e "${GREEN}✅ Token atualizado em $PROPS_FILE${NC}"
        echo "   (Backup criado: $PROPS_FILE.backup)"
        echo ""
        echo "Reinicie a aplicação para aplicar:"
        echo -e "${GREEN}./gradlew bootRun${NC}"
        echo ""
        ;;
        
    *)
        echo "Opção inválida!"
        exit 1
        ;;
esac

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "📱 Próximos passos:"
echo ""
echo "1. Reinicie a aplicação (se necessário)"
echo "2. Registre tokens de dispositivos via API"
echo "3. Crie uma delivery para testar notificações"
echo "4. Verifique os logs: tail -f app.log | grep -i notif"
echo ""
echo "💡 Dica: Use o script test-push-notification.sh para testes rápidos!"
echo ""
