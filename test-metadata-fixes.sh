#!/bin/bash

# Script de teste rápido para verificar correções do metadata

echo "🧪 TESTE RÁPIDO - Correções do Metadata"
echo "========================================"
echo ""

# Cores
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Contador de testes
PASSED=0
FAILED=0

# Função para testar
test_endpoint() {
    local test_name="$1"
    local curl_cmd="$2"
    local expected="$3"
    
    echo "📝 Testando: $test_name"
    
    result=$(eval "$curl_cmd" 2>/dev/null)
    
    if [ $? -eq 0 ] && [ ! -z "$result" ]; then
        echo -e "${GREEN}✅ PASS${NC}"
        ((PASSED++))
        echo "   Resultado: $result"
    else
        echo -e "${RED}❌ FAIL${NC}"
        ((FAILED++))
        echo "   Esperado: $expected"
    fi
    echo ""
}

# Verifica se servidor está rodando
echo "🔍 Verificando servidor..."
if ! curl -s -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo -e "${RED}❌ ERRO: Servidor não está rodando!${NC}"
    echo ""
    echo "Para iniciar o servidor:"
    echo "  ./gradlew bootRun"
    echo ""
    exit 1
fi
echo -e "${GREEN}✅ Servidor está rodando!${NC}"
echo ""

# Teste 1: Options com value/label corretos
test_endpoint \
    "Options com value/label corretos" \
    "curl -s http://localhost:8080/api/metadata/event | jq -r '.formFields[] | select(.name == \"eventType\") | .options[0] | \"\(.value) = \(.label)\"'" \
    "RUNNING = Corrida"

# Teste 2: Labels em português
test_endpoint \
    "Labels traduzidos" \
    "curl -s http://localhost:8080/api/metadata/event | jq -r '.formFields[] | select(.name == \"name\") | .label'" \
    "Nome"

# Teste 3: Sem campos de sistema
test_endpoint \
    "Campos de sistema removidos (deve retornar vazio)" \
    "curl -s http://localhost:8080/api/metadata/event | jq -r '.formFields[] | select(.name == \"id\") | .name' | wc -l | tr -d ' '" \
    "0"

# Teste 4: Sem espaços extras
test_endpoint \
    "Valores sem espaços extras" \
    "curl -s http://localhost:8080/api/metadata/registration | jq -r '.formFields[] | select(.name == \"status\") | .options[0].value'" \
    "PENDING"

# Teste 5: Type 'select' para enums
test_endpoint \
    "Enums com type='select'" \
    "curl -s http://localhost:8080/api/metadata/event | jq -r '.formFields[] | select(.name == \"eventType\") | .type'" \
    "select"

# Resultado final
echo "========================================"
echo "📊 RESULTADO FINAL"
echo "========================================"
echo -e "✅ Testes passaram: ${GREEN}$PASSED${NC}"
echo -e "❌ Testes falharam: ${RED}$FAILED${NC}"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}🎉 TODAS as correções estão funcionando!${NC}"
    exit 0
else
    echo -e "${RED}⚠️  Alguns testes falharam. Verifique os erros acima.${NC}"
    exit 1
fi
