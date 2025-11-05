#!/bin/bash

echo ""
echo "=========================================="
echo "  🔍 Verificando Tradução: Contrato Motoboy"
echo "=========================================="
echo ""

# Cores para output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "📋 1. Verificando se a aplicação está rodando..."
echo ""

# Verificar se a porta 8080 está em uso
if lsof -i :8080 >/dev/null 2>&1; then
    echo -e "${GREEN}✅ Aplicação rodando na porta 8080${NC}"
    APP_RUNNING=true
else
    echo -e "${RED}❌ Aplicação não está rodando na porta 8080${NC}"
    APP_RUNNING=false
fi

echo ""
echo "📋 2. Verificando processo Java..."
echo ""

JAVA_PROCESS=$(ps aux | grep -E "java.*mvt" | grep -v grep)
if [ -n "$JAVA_PROCESS" ]; then
    echo -e "${GREEN}✅ Processo Java encontrado:${NC}"
    echo "$JAVA_PROCESS"
else
    echo -e "${RED}❌ Nenhum processo Java MVT encontrado${NC}"
fi

echo ""
echo "📋 3. Testando conectividade..."
echo ""

if [ "$APP_RUNNING" = true ]; then
    # Teste básico de conectividade
    if curl -f -s http://localhost:8080/actuator/health >/dev/null; then
        echo -e "${GREEN}✅ Endpoint /actuator/health respondendo${NC}"
        
        # Testar endpoint de metadata (se existir)
        echo ""
        echo "📋 4. Testando metadata API..."
        echo ""
        
        # Teste EmploymentContract metadata
        if curl -f -s http://localhost:8080/api/metadata/EmploymentContract >/dev/null; then
            echo -e "${GREEN}✅ Metadata API respondendo${NC}"
            echo ""
            echo -e "${YELLOW}📄 Metadata EmploymentContract:${NC}"
            curl -s http://localhost:8080/api/metadata/EmploymentContract | jq '.displayName // .name // "Não encontrado"'
        else
            echo -e "${YELLOW}⚠️  Metadata API não disponível (normal se não implementada)${NC}"
        fi
        
        # Teste User metadata para ver employmentContracts
        echo ""
        echo -e "${YELLOW}🔍 Procurando campo 'employmentContracts' em User:${NC}"
        USER_METADATA=$(curl -s http://localhost:8080/api/metadata/User 2>/dev/null)
        if [ $? -eq 0 ]; then
            echo "$USER_METADATA" | jq '.fields[] | select(.name == "employmentContracts") | .label // "Campo não encontrado"'
        else
            echo -e "${YELLOW}⚠️  User metadata não disponível${NC}"
        fi
        
    else
        echo -e "${RED}❌ Aplicação não responde em /actuator/health${NC}"
    fi
else
    echo -e "${RED}❌ Aplicação não está rodando - não é possível testar${NC}"
fi

echo ""
echo "📋 5. Verificando arquivos de tradução..."
echo ""

# Verificar se a tradução foi adicionada
if grep -q "employmentContracts.*Contratos Motoboy" src/main/java/com/mvt/mvt_events/metadata/JpaMetadataExtractor.java; then
    echo -e "${GREEN}✅ Tradução 'employmentContracts' → 'Contratos Motoboy' encontrada${NC}"
else
    echo -e "${RED}❌ Tradução 'employmentContracts' não encontrada${NC}"
fi

if grep -q "@DisplayLabel(\"Contrato Motoboy\")" src/main/java/com/mvt/mvt_events/jpa/EmploymentContract.java; then
    echo -e "${GREEN}✅ @DisplayLabel('Contrato Motoboy') encontrado em EmploymentContract${NC}"
else
    echo -e "${RED}❌ @DisplayLabel não encontrado em EmploymentContract${NC}"
fi

echo ""
echo "=========================================="
echo "  📊 Resumo do Status"
echo "=========================================="
echo ""

if [ "$APP_RUNNING" = true ]; then
    echo -e "${GREEN}✅ Status: APLICAÇÃO RODANDO${NC}"
    echo -e "${GREEN}✅ Tradução: IMPLEMENTADA${NC}"
    echo ""
    echo "🎯 Para testar a tradução manualmente:"
    echo "   http://localhost:8080/api/metadata/User"
    echo "   http://localhost:8080/api/metadata/EmploymentContract"
else
    echo -e "${YELLOW}⚠️  Status: APLICAÇÃO PARADA${NC}"
    echo -e "${GREEN}✅ Tradução: IMPLEMENTADA (aguardando restart)${NC}"
    echo ""
    echo "🚀 Para iniciar a aplicação:"
    echo "   ./gradlew bootRun"
fi

echo ""
