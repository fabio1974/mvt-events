#!/bin/bash

echo ""
echo "=========================================="
echo "  🔧 Testando Correção do StackOverflow"
echo "=========================================="
echo ""

# Aguardar aplicação iniciar
echo "⏳ Aguardando aplicação iniciar..."
sleep 20

# Verificar se aplicação está rodando
if ! lsof -i :8080 >/dev/null 2>&1; then
    echo "❌ Aplicação não está rodando em :8080"
    echo "📋 Logs:"
    tail -50 app-stackoverflow-fix.log
    exit 1
fi

echo "✅ Aplicação rodando!"
echo ""

# Testar o PUT que estava causando StackOverflow
echo "🧪 Testando PUT /api/organizations/4 com contratos..."
echo ""

RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT 'http://localhost:8080/api/organizations/4' \
  -H 'Accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJvcmdhbml6YXRpb25JZCI6Niwicm9sZSI6IkFETUlOIiwiYWRkcmVzcyI6IlJ1YSBKb2FxdWltIE5hYnVjbyBQZXJlaXJhLCAxMTYiLCJwaG9uZSI6Ijg1OTk3NTcyOTE5IiwibmFtZSI6IkZhYmlvIEFkbWluIiwiY3BmIjoiMjIyLjMzMy40NDQtMDUiLCJ1c2VySWQiOiI3NDJmNThlYS01YmMxLTRiYjUtODRkYy01ZWE0NjNkMTUwNDQiLCJhdXRob3JpdGllcyI6WyJST0xFX0FETUlOIl0sImVtYWlsIjoiYWRtaW5AdGVzdC5jb20iLCJzdWIiOiJhZG1pbkB0ZXN0LmNvbSIsImlhdCI6MTc2MTQxMTY3MiwiZXhwIjoxNzYxNDI5NjcyfQ.wIzjBzaVeCoRagZNwTNtvQDa1vKaug-B9vkEFLoyOAA' \
  -H 'Content-Type: application/json' \
  --data-raw '{"commissionPercentage":5,"status":"ACTIVE","employmentContracts":[{"courier":"6008534c-fe16-4d69-8bb7-d54745a3c980","linkedAt":"2025-10-25T14:01:14.503507","isActive":true}],"serviceContracts":[{"client":"45158434-073d-43df-b93a-11ac88353327","contractNumber":"","isPrimary":true,"status":"ACTIVE","contractDate":"2025-10-25","startDate":"2025-10-25T03:00:00.000Z","endDate":""}],"id":4,"createdAt":"2025-09-23T01:51:06.912064","updatedAt":"2025-10-22T20:40:02.95938","name":"Grupo do Samuel","slug":"moveltrack-sistemas-ltda","contactEmail":"samuel@gmail.com","phone":"85997572919","website":"http://movletrackeventos","description":"descrição da organização Grupo do Samuel","logoUrl":"","city":{"id":1068}}')

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | head -n-1)

echo "📊 HTTP Status: $HTTP_CODE"
echo ""

if [ "$HTTP_CODE" = "200" ]; then
    echo "✅ SUCCESS! PUT executado com sucesso"
    echo ""
    echo "📄 Resposta:"
    echo "$BODY" | jq '.'
    echo ""
    echo "✅ StackOverflowError corrigido!"
elif [ "$HTTP_CODE" = "500" ]; then
    echo "❌ ERRO 500 - Ainda há problema"
    echo ""
    echo "📄 Resposta:"
    echo "$BODY" | jq '.'
    echo ""
    echo "📋 Últimas linhas do log:"
    tail -30 app-stackoverflow-fix.log | grep -A 10 "StackOverflow\|Error\|Exception"
else
    echo "⚠️  HTTP $HTTP_CODE"
    echo ""
    echo "📄 Resposta:"
    echo "$BODY"
fi

echo ""
echo "=========================================="
