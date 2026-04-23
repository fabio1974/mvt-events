#!/usr/bin/env bash
# E2E: flag autoPrintEnabled no StoreProfile
# - Default true para CLIENT com StoreProfile existente (migration aplicada)
# - Admin pode togglear via PUT /api/users/{id} com nested storeProfile
# - Metadata /api/metadata/forms/user inclui o campo dentro do storeProfile.relationship.fields
# - GET /api/orders/{id} expõe storeAutoPrintEnabled no response

set -u
API=http://localhost:8080/api
PASS="Demo@123"
ADMIN_EMAIL="moveltrack@gmail.com"
CLIENT_EMAIL="client@demo.com"
CLIENT_ID="c1111111-1111-1111-1111-111111111111"

pass=0; fail=0
ok()   { echo "  ✅ $1"; pass=$((pass+1)); }
bad()  { echo "  ❌ $1"; fail=$((fail+1)); }
section() { echo; echo "── $1 ──"; }

login() {
  curl -s -X POST "$API/auth/login" -H "Content-Type: application/json" \
    -d "{\"username\":\"$1\",\"password\":\"$PASS\"}" \
    | python3 -c 'import json,sys;print(json.load(sys.stdin).get("token",""))'
}

jget() {  # $1 = JSON, $2 = path "a.b.c"
  python3 -c "
import json, sys
d = json.loads(sys.argv[1])
for k in sys.argv[2].split('.'):
    if isinstance(d, list): d = d[int(k)]
    elif isinstance(d, dict): d = d.get(k)
    else: d = None
print(d)
" "$1" "$2" 2>/dev/null || echo "null"
}

# ========================================================
section "1) Default autoPrintEnabled=true em CLIENT com StoreProfile"
TOK_ADMIN=$(login "$ADMIN_EMAIL")
[[ -n "$TOK_ADMIN" ]] && ok "login ADMIN" || { bad "login ADMIN falhou"; exit 1; }

USER_JSON=$(curl -s "$API/users/$CLIENT_ID" -H "Authorization: Bearer $TOK_ADMIN")
FLAG=$(jget "$USER_JSON" "storeProfile.autoPrintEnabled")
[[ "$FLAG" == "True" ]] && ok "GET /users/$CLIENT_ID → storeProfile.autoPrintEnabled=true" || bad "flag=$FLAG (esperava true)"

# ========================================================
section "2) Metadata inclui autoPrintEnabled no nested storeProfile"
META=$(curl -s "$API/metadata/forms/user" -H "Authorization: Bearer $TOK_ADMIN")
HAS_FIELD=$(echo "$META" | python3 -c '
import json, sys
m = json.load(sys.stdin)
sp = next((f for f in m.get("fields", []) if f.get("name") == "storeProfile"), None)
if not sp:
    print("no_storeProfile"); sys.exit(0)
nested = (sp.get("relationship") or {}).get("fields") or []
f = next((nf for nf in nested if nf.get("name") == "autoPrintEnabled"), None)
if not f:
    print("no_field"); sys.exit(0)
parts = ["type=" + str(f.get("type")), "default=" + str(f.get("defaultValue")), "label=" + str(f.get("label"))]
print("ok:" + ",".join(parts))
')
if [[ "$HAS_FIELD" == ok:* ]]; then
  ok "metadata tem autoPrintEnabled ($HAS_FIELD)"
else
  bad "metadata sem autoPrintEnabled (result: $HAS_FIELD)"
fi

# ========================================================
section "3) PUT flag para false e verifica"
PUT_RESP=$(curl -s -X PUT "$API/users/$CLIENT_ID" \
  -H "Authorization: Bearer $TOK_ADMIN" -H "Content-Type: application/json" \
  -d '{"storeProfile":{"autoPrintEnabled":false}}' \
  -w "\n__HTTP__:%{http_code}")
CODE=$(echo "$PUT_RESP" | grep __HTTP__ | cut -d: -f2)
[[ "$CODE" == "200" ]] && ok "PUT /users/$CLIENT_ID HTTP 200" || bad "PUT HTTP $CODE | body: $(echo "$PUT_RESP" | head -c 200)"

USER_JSON=$(curl -s "$API/users/$CLIENT_ID" -H "Authorization: Bearer $TOK_ADMIN")
FLAG=$(jget "$USER_JSON" "storeProfile.autoPrintEnabled")
[[ "$FLAG" == "False" ]] && ok "após PUT: autoPrintEnabled=false" || bad "após PUT: flag=$FLAG (esperava false)"

# ========================================================
section "4) Order response expõe storeAutoPrintEnabled do client"
# Procura qualquer pedido do CLIENT no banco
TOK_CLIENT=$(login "$CLIENT_EMAIL")
ORDER_LIST=$(curl -s "$API/orders?size=1" -H "Authorization: Bearer $TOK_CLIENT")
ORDER_ID=$(echo "$ORDER_LIST" | python3 -c '
import json, sys
d = json.load(sys.stdin)
if isinstance(d, dict) and "content" in d: d = d["content"]
print(d[0]["id"] if d else "")
' 2>/dev/null)

if [[ -n "$ORDER_ID" ]]; then
  ORDER_JSON=$(curl -s "$API/orders/$ORDER_ID" -H "Authorization: Bearer $TOK_CLIENT")
  ORDER_FLAG=$(jget "$ORDER_JSON" "storeAutoPrintEnabled")
  [[ "$ORDER_FLAG" == "False" ]] && ok "GET /orders/$ORDER_ID → storeAutoPrintEnabled=false" || bad "order flag=$ORDER_FLAG (esperava false após PUT=false)"
else
  echo "  ⏭️  (sem orders p/ $CLIENT_EMAIL — pulando; ok)"
fi

# ========================================================
section "5) Restaura default (true) para não sujar estado"
curl -s -X PUT "$API/users/$CLIENT_ID" \
  -H "Authorization: Bearer $TOK_ADMIN" -H "Content-Type: application/json" \
  -d '{"storeProfile":{"autoPrintEnabled":true}}' > /dev/null
USER_JSON=$(curl -s "$API/users/$CLIENT_ID" -H "Authorization: Bearer $TOK_ADMIN")
FLAG=$(jget "$USER_JSON" "storeProfile.autoPrintEnabled")
[[ "$FLAG" == "True" ]] && ok "reset: autoPrintEnabled=true" || bad "reset falhou (flag=$FLAG)"

# ========================================================
section "6) Resultado"
echo "   ✅ $pass passou  ❌ $fail falhou"
[[ $fail -eq 0 ]]
