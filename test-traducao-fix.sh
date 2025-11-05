#!/bin/bash

echo ""
echo "=========================================="
echo "  🔍 Testando Correção da Tradução"
echo "=========================================="
echo ""

# Cores para output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

TRANSLATIONS_FILE="src/main/java/com/mvt/mvt_events/metadata/JpaMetadataExtractor.java"

echo -e "${BLUE}🔍 Verificando tradução de serviceContracts:${NC}"
echo ""

# Verificar se a tradução está correta
if grep -q 'serviceContracts.*Contratos de Cliente' "$TRANSLATIONS_FILE"; then
    echo -e "${GREEN}✅ serviceContracts → 'Contratos de Cliente' (CORRETO)${NC}"
else
    echo -e "${RED}❌ serviceContracts ainda não está traduzido corretamente${NC}"
fi

# Verificar se não tem mais "Estabelecimentos"
if grep -q 'serviceContracts.*Estabelecimentos' "$TRANSLATIONS_FILE"; then
    echo -e "${RED}❌ ERRO: serviceContracts ainda está como 'Estabelecimentos'${NC}"
else
    echo -e "${GREEN}✅ 'Estabelecimentos' removido (CORRETO)${NC}"
fi

echo ""
echo -e "${BLUE}📋 Resumo das Traduções de Contratos:${NC}"
echo ""
echo "  • employmentContracts → 'Contratos Motoboy'"
echo "  • contracts → 'Contratos de Cliente'"
echo "  • serviceContracts → 'Contratos de Cliente'"
echo ""

# Aguardar aplicação iniciar
echo -e "${YELLOW}⏳ Aguardando aplicação iniciar...${NC}"
sleep 15

if lsof -i :8080 >/dev/null 2>&1; then
    echo -e "${GREEN}✅ Aplicação rodando!${NC}"
    echo ""
    echo -e "${BLUE}🧪 Testando API de metadata:${NC}"
    echo ""
    
    # Testar metadata da Organization
    echo "curl http://localhost:8080/api/metadata/Organization | jq '.relationships[] | select(.name == \"serviceContracts\") | {name, label}'"
    curl -s http://localhost:8080/api/metadata/Organization | jq '.relationships[] | select(.name == "serviceContracts") | {name, label}'
    echo ""
    
    echo "curl http://localhost:8080/api/metadata/Organization | jq '.relationships[] | select(.name == \"employmentContracts\") | {name, label}'"
    curl -s http://localhost:8080/api/metadata/Organization | jq '.relationships[] | select(.name == "employmentContracts") | {name, label}'
    echo ""
else
    echo -e "${RED}❌ Aplicação não está rodando em :8080${NC}"
fi

echo ""
echo "=========================================="
echo "  ✅ Teste Completo"
echo "=========================================="
echo ""
