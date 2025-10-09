#!/bin/bash

# Script para testar o endpoint unificado de metadata

echo "🧪 Testando Endpoint Unificado de Metadata"
echo "=========================================="
echo ""

# Função para verificar se o servidor está rodando
check_server() {
    echo "🔍 Verificando se servidor está rodando..."
    if curl -s -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo "✅ Servidor está rodando!"
        return 0
    else
        echo "❌ Servidor não está respondendo"
        return 1
    fi
}

# Função para reiniciar servidor
restart_server() {
    echo ""
    echo "🔄 Para reiniciar o servidor, execute:"
    echo "   ./gradlew bootRun"
    echo ""
    exit 1
}

# Verifica servidor
if ! check_server; then
    restart_server
fi

echo ""
echo "📊 Testando GET /api/metadata/event"
echo "===================================="
echo ""

# Teste 1: Metadata completo
echo "1️⃣ Metadata completo:"
curl -s http://localhost:8080/api/metadata/event | jq '.' | head -50

echo ""
echo ""
echo "2️⃣ Estrutura (keys):"
curl -s http://localhost:8080/api/metadata/event | jq 'keys'

echo ""
echo ""
echo "3️⃣ tableFields (primeiros 3):"
curl -s http://localhost:8080/api/metadata/event | jq '.tableFields[:3]'

echo ""
echo ""
echo "4️⃣ formFields (primeiros 3):"
curl -s http://localhost:8080/api/metadata/event | jq '.formFields[:3]'

echo ""
echo ""
echo "5️⃣ Campos do tipo 'select' (enums com options):"
curl -s http://localhost:8080/api/metadata/event | jq '.formFields[] | select(.type == "select") | {name, options}'

echo ""
echo ""
echo "6️⃣ Campos do tipo 'nested' (relacionamentos):"
curl -s http://localhost:8080/api/metadata/event | jq '.formFields[] | select(.type == "nested") | {name, relationship}'

echo ""
echo ""
echo "7️⃣ Filtros disponíveis:"
curl -s http://localhost:8080/api/metadata/event | jq '.filters[] | {name, type}'

echo ""
echo ""
echo "8️⃣ Configuração de paginação:"
curl -s http://localhost:8080/api/metadata/event | jq '.pagination'

echo ""
echo ""
echo "✅ Teste completo!"
