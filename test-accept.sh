#!/bin/bash

echo "=== Login ==="
RESPONSE=$(curl -s -X POST 'http://localhost:8080/api/auth/login' \
  -H 'Content-Type: application/json' \
  -d '{"username": "motoboy1@gmail.com", "password": "senha123"}')

echo "$RESPONSE" > /tmp/login-response.json
echo "Response salvo em /tmp/login-response.json"
cat /tmp/login-response.json
echo ""

TOKEN=$(echo "$RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
  echo "ERRO: Token não obtido"
  exit 1
fi

echo ""
echo "Token: ${TOKEN:0:50}..."
echo ""
echo "=== Testando ACCEPT da delivery 93 ==="
echo ""

curl -X PATCH "http://localhost:8080/api/deliveries/93/accept" \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"courierId":"6186c7af-2311-4756-bfc6-ce98bd31ed27"}' \
  -w "\n\nHTTP Status: %{http_code}\n" | tee /tmp/accept-response.json

echo ""
echo ""
echo "=== Response completa salva em /tmp/accept-response.json ==="
