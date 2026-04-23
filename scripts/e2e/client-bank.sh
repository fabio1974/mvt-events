#!/usr/bin/env bash
# E2E: estabelecimento (CLIENT) sem conta bancária não consegue operar.
# Requer BE local em http://localhost:8080 e seed users.

set -u
API=http://localhost:8080/api
PASS="Demo@123"
CLIENT_NO_BANK_EMAIL="client@zapi.com"
CLIENT_NO_BANK_ID="895be532-b207-4ea2-a2b9-23c0d1165098"
CLIENT_WITH_BANK_EMAIL="client@demo.com"
CLIENT_WITH_BANK_ID="c1111111-1111-1111-1111-111111111111"
CUSTOMER_EMAIL="demo.customer@zapi10.com"

pass=0; fail=0
ok()   { echo "  ✅ $1"; pass=$((pass+1)); }
bad()  { echo "  ❌ $1"; fail=$((fail+1)); }
section() { echo; echo "── $1 ──"; }

login() {
  curl -s -X POST "$API/auth/login" -H "Content-Type: application/json" \
    -d "{\"username\":\"$1\",\"password\":\"$PASS\"}" \
    | python3 -c 'import json,sys;print(json.load(sys.stdin).get("token",""))'
}

# ========================================================
section "1) GET /api/stores — enabled reflete existência de conta bancária"
TOK_CUST=$(login "$CUSTOMER_EMAIL")
[[ -n "$TOK_CUST" ]] && ok "login CUSTOMER" || { bad "login CUSTOMER falhou"; exit 1; }

STORES=$(curl -s "$API/stores" -H "Authorization: Bearer $TOK_CUST")
E_NO=$(echo "$STORES"   | python3 -c "import json,sys;d=json.load(sys.stdin);print([s for s in d if s['id']=='$CLIENT_NO_BANK_ID'][0].get('enabled'))" 2>/dev/null || echo "NOT_FOUND")
E_WITH=$(echo "$STORES" | python3 -c "import json,sys;d=json.load(sys.stdin);print([s for s in d if s['id']=='$CLIENT_WITH_BANK_ID'][0].get('enabled'))" 2>/dev/null || echo "NOT_FOUND")

[[ "$E_NO" == "False" ]]  && ok  "$CLIENT_NO_BANK_EMAIL    → enabled=false"  || bad "$CLIENT_NO_BANK_EMAIL → enabled=$E_NO (esperava false)"
[[ "$E_WITH" == "True" ]] && ok  "$CLIENT_WITH_BANK_EMAIL  → enabled=true"   || bad "$CLIENT_WITH_BANK_EMAIL → enabled=$E_WITH (esperava true)"

# ========================================================
section "2) /me/activation-status — CLIENT sem bank tem missing=[bankAccount]"
TOK_CNB=$(login "$CLIENT_NO_BANK_EMAIL")
[[ -n "$TOK_CNB" ]] && ok "login CLIENT sem bank" || bad "login CLIENT sem bank falhou"

STATUS=$(curl -s "$API/users/me/activation-status" -H "Authorization: Bearer $TOK_CNB")
MISSING=$(echo "$STATUS" | python3 -c 'import json,sys;print(",".join(json.load(sys.stdin).get("missing",[])))')
ENABLED_FLAG=$(echo "$STATUS" | python3 -c 'import json,sys;print(json.load(sys.stdin).get("enabled"))')

echo "$MISSING" | grep -q "bankAccount" && ok "missing inclui 'bankAccount' (atual: $MISSING)" || bad "missing NÃO inclui 'bankAccount' (atual: $MISSING)"
[[ "$ENABLED_FLAG" == "False" ]] && ok "enabled=false" || bad "enabled=$ENABLED_FLAG (esperava false)"

# ========================================================
section "3) CLIENT COM bank não tem bankAccount em missing"
TOK_CWB=$(login "$CLIENT_WITH_BANK_EMAIL")
STATUS2=$(curl -s "$API/users/me/activation-status" -H "Authorization: Bearer $TOK_CWB")
MISSING2=$(echo "$STATUS2" | python3 -c 'import json,sys;print(",".join(json.load(sys.stdin).get("missing",[])))')
if echo "$MISSING2" | grep -q "bankAccount"; then
  bad "missing AINDA tem 'bankAccount' (atual: $MISSING2)"
else
  ok "missing não tem 'bankAccount' (atual: $MISSING2)"
fi

# ========================================================
section "4) POST /api/orders para CLIENT sem bank → 400 'não habilitado'"
BODY=$(cat <<JSON
{
  "clientId": "$CLIENT_NO_BANK_ID",
  "items": [{"productId": 1, "quantity": 1}],
  "deliveryAddress": {"address":"Rua X","latitude":-3.0,"longitude":-40.0},
  "paymentTiming": "ON_DELIVERY"
}
JSON
)
RESP=$(curl -s -w "\n__HTTP__:%{http_code}" -X POST "$API/orders" \
  -H "Authorization: Bearer $TOK_CUST" -H "Content-Type: application/json" \
  -d "$BODY")
CODE=$(echo "$RESP" | grep __HTTP__ | cut -d: -f2)
BODY_RESP=$(echo "$RESP" | sed '/__HTTP__/d')
echo "   HTTP $CODE | body: $(echo "$BODY_RESP" | head -c 180)"
# Spring serializa RuntimeException genérica como 500; o que garante o bloqueio é a mensagem.
[[ "$CODE" == "400" || "$CODE" == "500" ]] && ok "HTTP $CODE (rejeição com erro)" || bad "HTTP $CODE (esperava 400/500)"
echo "$BODY_RESP" | grep -qi "habilitado" && ok "resposta contém 'habilitado'" || bad "resposta NÃO contém 'habilitado'"

# ========================================================
section "5) Resultado"
echo "   ✅ $pass passou  ❌ $fail falhou"
[[ $fail -eq 0 ]]
