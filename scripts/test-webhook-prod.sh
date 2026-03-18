#!/bin/bash

# Script para testar webhook de produção do Pagar.me
# Este script envia um payload de teste para o endpoint de webhook

# Configurações
PROD_URL="https://mvt-events-api.onrender.com/api/webhooks/order"
ORDER_ID="or_test_webhook_$(date +%s)"

echo "🧪 Testando Webhook de Produção"
echo "================================"
echo ""
echo "📍 URL: $PROD_URL"
echo "🆔 Order ID de teste: $ORDER_ID"
echo ""

# Payload de teste - simulando um evento order.paid
PAYLOAD=$(cat <<EOF
{
  "id": "hook_test_$(date +%s)",
  "type": "order.paid",
  "created_at": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "data": {
    "id": "$ORDER_ID",
    "code": "TEST-WEBHOOK-001",
    "status": "paid",
    "amount": 10000,
    "currency": "BRL",
    "customer": {
      "id": "cus_test123",
      "name": "João Teste Silva",
      "email": "teste@mvt.com"
    },
    "charges": [
      {
        "id": "ch_test123",
        "code": "TEST-CHARGE-001",
        "amount": 10000,
        "status": "paid",
        "payment_method": "pix",
        "paid_at": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
        "last_transaction": {
          "status": "paid",
          "success": true,
          "gateway_response": {
            "code": "200"
          },
          "qr_code": "00020126360014BR.GOV.BCB.PIX0114+5511999999999520400005303986540510.005802BR5913Teste Webhook6009SAO PAULO62070503***6304TEST",
          "qr_code_url": "https://test.pagar.me/pix/qr/test123.png"
        }
      }
    ],
    "items": [
      {
        "description": "Teste de Webhook - Entrega consolidada",
        "quantity": 1,
        "amount": 10000
      }
    ]
  }
}
EOF
)

echo "📤 Enviando requisição..."
echo ""

# Enviar requisição
RESPONSE=$(curl -s -X POST "$PROD_URL" \
  -H "Content-Type: application/json" \
  -d "$PAYLOAD")

HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$PROD_URL" \
  -H "Content-Type: application/json" \
  -d "$PAYLOAD")

echo "📊 Resultado:"
echo "   Status HTTP: $HTTP_STATUS"
echo ""
echo "📄 Response Body:"
echo "$RESPONSE" | jq '.' 2>/dev/null || echo "$RESPONSE"
echo ""

if [ "$HTTP_STATUS" = "200" ]; then
    echo "✅ Webhook funcionando corretamente!"
else
    echo "⚠️ Status HTTP inesperado: $HTTP_STATUS"
fi

echo ""
echo "💡 Nota: Este é um teste simulado. O payment com Order ID '$ORDER_ID'"
echo "   provavelmente não existe no banco, então o webhook deve retornar"
echo "   uma mensagem informando que o payment não foi encontrado."
echo ""
echo "📝 Para testar com um payment real:"
echo "   1. Crie um payment via API"
echo "   2. Anote o provider_payment_id (Order ID do Pagar.me)"
echo "   3. Edite este script e substitua ORDER_ID pelo ID real"
echo "   4. Execute novamente"
