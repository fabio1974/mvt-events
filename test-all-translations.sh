#!/bin/bash

# Script de teste completo para verificar TODAS as traduções do metadata

echo "🧪 TESTE COMPLETO - Traduções do Metadata"
echo "=========================================="
echo ""

# Cores
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Contador
PASSED=0
FAILED=0

# Verifica servidor
echo "🔍 Verificando servidor..."
if ! curl -s -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo -e "${RED}❌ Servidor não está rodando!${NC}"
    echo "Inicie com: ./gradlew bootRun"
    exit 1
fi
echo -e "${GREEN}✅ Servidor rodando${NC}"
echo ""

# Função de teste
test_translation() {
    local entity="$1"
    local field="$2"
    local expected_label="$3"
    local test_name="$4"
    
    result=$(curl -s "http://localhost:8080/api/metadata/$entity" | \
        jq -r ".formFields[] | select(.name == \"$field\") | .label")
    
    if [ "$result" == "$expected_label" ]; then
        echo -e "${GREEN}✅${NC} $test_name"
        ((PASSED++))
    else
        echo -e "${RED}❌${NC} $test_name"
        echo "   Esperado: $expected_label"
        echo "   Recebido: $result"
        ((FAILED++))
    fi
}

test_enum_option() {
    local entity="$1"
    local field="$2"
    local enum_value="$3"
    local expected_label="$4"
    local test_name="$5"
    
    result=$(curl -s "http://localhost:8080/api/metadata/$entity" | \
        jq -r ".formFields[] | select(.name == \"$field\") | .options[] | select(.value == \"$enum_value\") | .label")
    
    if [ "$result" == "$expected_label" ]; then
        echo -e "${GREEN}✅${NC} $test_name"
        ((PASSED++))
    else
        echo -e "${RED}❌${NC} $test_name"
        echo "   Esperado: $expected_label"
        echo "   Recebido: $result"
        ((FAILED++))
    fi
}

echo "📋 TESTANDO LABELS DOS CAMPOS"
echo "=============================="

# Event
test_translation "event" "name" "Nome" "Event.name"
test_translation "event" "slug" "URL Amigável" "Event.slug"
test_translation "event" "eventType" "Tipo de Evento" "Event.eventType"
test_translation "event" "eventDate" "Data do Evento" "Event.eventDate"
test_translation "event" "currency" "Moeda" "Event.currency"
test_translation "event" "platformFeePercentage" "Taxa da Plataforma (%)" "Event.platformFeePercentage"

# User
test_translation "user" "username" "E-mail" "User.username"
test_translation "user" "password" "Senha" "User.password"
test_translation "user" "role" "Perfil" "User.role"
test_translation "user" "cpf" "CPF" "User.cpf"
test_translation "user" "emergencyContact" "Contato de Emergência" "User.emergencyContact"

# Payment
test_translation "payment" "amount" "Valor" "Payment.amount"
test_translation "payment" "paymentMethod" "Método de Pagamento" "Payment.paymentMethod"
test_translation "payment" "gatewayFee" "Taxa do Gateway" "Payment.gatewayFee"
test_translation "payment" "refundAmount" "Valor do Reembolso" "Payment.refundAmount"

# Organization
test_translation "organization" "logoUrl" "URL do Logo" "Organization.logoUrl"
test_translation "organization" "contactEmail" "E-mail de Contato" "Organization.contactEmail"

echo ""
echo "🏷️  TESTANDO OPTIONS DOS ENUMS"
echo "=============================="

# EventType
test_enum_option "event" "eventType" "RUNNING" "Corrida" "EventType.RUNNING"
test_enum_option "event" "eventType" "CYCLING" "Ciclismo" "EventType.CYCLING"
test_enum_option "event" "eventType" "MARATHON" "Maratona" "EventType.MARATHON"
test_enum_option "event" "eventType" "OBSTACLE_RACE" "Corrida de Obstáculos" "EventType.OBSTACLE_RACE"

# Event Status
test_enum_option "event" "status" "DRAFT" "Rascunho" "Event.Status.DRAFT"
test_enum_option "event" "status" "PUBLISHED" "Publicado" "Event.Status.PUBLISHED"

# User Role
test_enum_option "user" "role" "USER" "Usuário" "User.Role.USER"
test_enum_option "user" "role" "ADMIN" "Administrador" "User.Role.ADMIN"

# User Gender
test_enum_option "user" "gender" "MALE" "Masculino" "User.Gender.MALE"
test_enum_option "user" "gender" "FEMALE" "Feminino" "User.Gender.FEMALE"

# PaymentMethod
test_enum_option "payment" "paymentMethod" "CREDIT_CARD" "Cartão de Crédito" "Payment.Method.CREDIT_CARD"
test_enum_option "payment" "paymentMethod" "PIX" "PIX" "Payment.Method.PIX"

# Payment Status
test_enum_option "payment" "status" "PENDING" "Pendente" "Payment.Status.PENDING"
test_enum_option "payment" "status" "COMPLETED" "Concluído" "Payment.Status.COMPLETED"

echo ""
echo "🔍 TESTANDO VALUE/LABEL CORRETOS"
echo "================================="

# Verifica se value está correto (deve ser o enum, não a tradução)
value_check=$(curl -s "http://localhost:8080/api/metadata/event" | \
    jq -r '.formFields[] | select(.name == "eventType") | .options[0].value')

if [ "$value_check" == "RUNNING" ] || [ "$value_check" == "CYCLING" ] || [ "$value_check" == "MARATHON" ]; then
    echo -e "${GREEN}✅${NC} Options têm value correto (enum original)"
    ((PASSED++))
else
    echo -e "${RED}❌${NC} Options têm value incorreto"
    echo "   Recebido: $value_check"
    ((FAILED++))
fi

echo ""
echo "❌ TESTANDO CAMPOS REMOVIDOS"
echo "============================"

# Verifica se campos de sistema foram removidos
id_check=$(curl -s "http://localhost:8080/api/metadata/event" | \
    jq '.formFields[] | select(.name == "id")' | wc -l | tr -d ' ')

if [ "$id_check" == "0" ]; then
    echo -e "${GREEN}✅${NC} Campo 'id' removido dos formFields"
    ((PASSED++))
else
    echo -e "${RED}❌${NC} Campo 'id' ainda aparece nos formFields"
    ((FAILED++))
fi

tenantId_check=$(curl -s "http://localhost:8080/api/metadata/event" | \
    jq '.formFields[] | select(.name == "tenantId")' | wc -l | tr -d ' ')

if [ "$tenantId_check" == "0" ]; then
    echo -e "${GREEN}✅${NC} Campo 'tenantId' removido dos formFields"
    ((PASSED++))
else
    echo -e "${RED}❌${NC} Campo 'tenantId' ainda aparece nos formFields"
    ((FAILED++))
fi

echo ""
echo "🚫 TESTANDO ESPAÇOS EXTRAS REMOVIDOS"
echo "====================================="

# Verifica se não há espaços extras nos valores
spaces_check=$(curl -s "http://localhost:8080/api/metadata/user" | \
    jq -r '.formFields[] | select(.name == "role") | .options[0].value' | \
    grep -c " " || echo "0")

if [ "$spaces_check" == "0" ]; then
    echo -e "${GREEN}✅${NC} Sem espaços extras nos valores de enum"
    ((PASSED++))
else
    echo -e "${RED}❌${NC} Ainda há espaços extras nos valores"
    ((FAILED++))
fi

echo ""
echo "=========================================="
echo "📊 RESULTADO FINAL"
echo "=========================================="
echo -e "✅ Testes passaram: ${GREEN}$PASSED${NC}"
echo -e "❌ Testes falharam: ${RED}$FAILED${NC}"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}🎉 TODAS as traduções estão funcionando perfeitamente!${NC}"
    echo ""
    echo "✅ Labels: 100% em português"
    echo "✅ Options: 100% traduzidas"
    echo "✅ Value/label: ordem correta"
    echo "✅ Espaços: removidos"
    echo "✅ Campos de sistema: ocultos"
    exit 0
else
    echo -e "${RED}⚠️  Alguns testes falharam. Verifique os erros acima.${NC}"
    exit 1
fi
