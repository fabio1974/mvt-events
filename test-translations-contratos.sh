#!/bin/bash

echo ""
echo "=========================================="
echo "  🔍 Testando Traduções dos Contratos"
echo "=========================================="
echo ""

# Cores para output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo "📋 Verificando traduções implementadas..."
echo ""

# Arquivo de traduções
TRANSLATIONS_FILE="src/main/java/com/mvt/mvt_events/metadata/JpaMetadataExtractor.java"

echo -e "${BLUE}🔍 Campos de EmploymentContract (Contrato Motoboy):${NC}"
echo ""

# Verificar traduções de EmploymentContract
if grep -q "linkedAt.*Vinculado em" "$TRANSLATIONS_FILE"; then
    echo -e "${GREEN}✅ linkedAt → 'Vinculado em'${NC}"
else
    echo -e "${RED}❌ linkedAt não traduzido${NC}"
fi

if grep -q "isActive.*Ativo" "$TRANSLATIONS_FILE"; then
    echo -e "${GREEN}✅ isActive → 'Ativo'${NC}"
else
    echo -e "${RED}❌ isActive não traduzido${NC}"
fi

echo ""
echo -e "${BLUE}🔍 Campos de Contract (Contrato de Cliente):${NC}"
echo ""

# Verificar traduções de Contract
if grep -q "contractNumber.*Número do Contrato" "$TRANSLATIONS_FILE"; then
    echo -e "${GREEN}✅ contractNumber → 'Número do Contrato'${NC}"
else
    echo -e "${RED}❌ contractNumber não traduzido${NC}"
fi

if grep -q "isPrimary.*Contrato Principal" "$TRANSLATIONS_FILE"; then
    echo -e "${GREEN}✅ isPrimary → 'Contrato Principal'${NC}"
else
    echo -e "${RED}❌ isPrimary não traduzido${NC}"
fi

if grep -q "contractDate.*Data do Contrato" "$TRANSLATIONS_FILE"; then
    echo -e "${GREEN}✅ contractDate → 'Data do Contrato'${NC}"
else
    echo -e "${RED}❌ contractDate não traduzido${NC}"
fi

echo ""
echo -e "${BLUE}🔍 Campos Comuns:${NC}"
echo ""

if grep -q "client.*Cliente" "$TRANSLATIONS_FILE"; then
    echo -e "${GREEN}✅ client → 'Cliente'${NC}"
else
    echo -e "${RED}❌ client não traduzido${NC}"
fi

if grep -q "startDate.*Data de Início" "$TRANSLATIONS_FILE"; then
    echo -e "${GREEN}✅ startDate → 'Data de Início'${NC}"
else
    echo -e "${RED}❌ startDate não traduzido${NC}"
fi

if grep -q "endDate.*Data de Término" "$TRANSLATIONS_FILE"; then
    echo -e "${GREEN}✅ endDate → 'Data de Término'${NC}"
else
    echo -e "${RED}❌ endDate não traduzido${NC}"
fi

echo ""
echo "=========================================="
echo "  📊 Resumo das Traduções"
echo "=========================================="
echo ""

echo -e "${YELLOW}📋 EmploymentContract (Contrato Motoboy):${NC}"
echo "  • linkedAt → 'Vinculado em'"
echo "  • isActive → 'Ativo'"
echo "  • courier → 'Motoboy'"
echo "  • organization → 'Grupo'"
echo ""

echo -e "${YELLOW}📋 Contract (Contrato de Cliente):${NC}"
echo "  • contractNumber → 'Número do Contrato'"
echo "  • isPrimary → 'Contrato Principal'"
echo "  • contractDate → 'Data do Contrato'"
echo "  • client → 'Cliente'"
echo "  • organization → 'Grupo'"
echo "  • startDate → 'Data de Início'"
echo "  • endDate → 'Data de Término'"
echo "  • status → 'Status'"
echo ""

echo -e "${YELLOW}📋 Status dos Contratos:${NC}"
echo "  • ACTIVE → 'Ativa'"
echo "  • SUSPENDED → 'Suspenso'"
echo "  • CANCELLED → 'Cancelado'"
echo ""

if lsof -i :8080 >/dev/null 2>&1; then
    echo -e "${GREEN}✅ Aplicação rodando - traduções estão ativas!${NC}"
    echo ""
    echo "🎯 Para testar as traduções:"
    echo "   http://localhost:8080/api/metadata/EmploymentContract"
    echo "   http://localhost:8080/api/metadata/Contract"
    echo ""
    echo "🔍 Ou via curl:"
    echo "   curl http://localhost:8080/api/metadata/EmploymentContract | jq '.fields[] | select(.name == \"linkedAt\" or .name == \"isActive\") | {name, label}'"
    echo "   curl http://localhost:8080/api/metadata/Contract | jq '.fields[] | select(.name == \"contractNumber\" or .name == \"isPrimary\") | {name, label}'"
else
    echo -e "${YELLOW}⚠️  Aplicação não está rodando${NC}"
    echo ""
    echo "🚀 Para testar as traduções:"
    echo "   ./gradlew bootRun"
    echo "   # Em outro terminal:"
    echo "   curl http://localhost:8080/api/metadata/EmploymentContract"
fi

echo ""
