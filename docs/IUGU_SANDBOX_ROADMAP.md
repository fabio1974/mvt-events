# 🗺️ Roadmap - Configuração e Testes Iugu Sandbox

**Data**: 03/12/2025  
**Objetivo**: Configurar credenciais Iugu e testar fluxo completo no ambiente Sandbox

---

## 📋 Índice

1. [Pré-requisitos](#1-pré-requisitos)
2. [Criar Conta Iugu](#2-criar-conta-iugu)
3. [Obter Credenciais](#3-obter-credenciais)
4. [Configurar Application](#4-configurar-application)
5. [Testar Endpoints](#5-testar-endpoints)
6. [Configurar Webhooks](#6-configurar-webhooks)
7. [Testes End-to-End](#7-testes-end-to-end)
8. [Troubleshooting](#8-troubleshooting)

---

## 1. Pré-requisitos

### ✅ Checklist

- [ ] Sistema compilando sem erros
- [ ] Aplicação rodando na porta 8080
- [ ] Banco de dados configurado
- [ ] Usuários de teste criados (COURIER, CLIENT)
- [ ] Acesso à internet para APIs Iugu

### 🔍 Verificar Status Atual

```bash
# 1. Compilar
./gradlew clean build

# 2. Verificar se aplicação está rodando
curl -s http://localhost:8080/actuator/health

# 3. Verificar usuários de teste
./create-test-users.sh
```

---

## 2. Criar Conta Iugu

### 📝 Passo a Passo

1. **Acessar**: https://iugu.com/
2. **Clicar**: "Cadastre-se" ou "Começar Grátis"
3. **Preencher**:
   - Nome completo
   - Email
   - Telefone
   - CPF/CNPJ
   - Senha

4. **Confirmar email**
5. **Completar cadastro**:
   - Dados da empresa
   - Endereço
   - Conta bancária (para recebimentos)

### ⏱️ Tempo estimado: 10-15 minutos

---

## 3. Obter Credenciais

### 🔑 API Token (Sandbox)

1. **Login** em https://iugu.com/
2. **Menu** → "Administração" → "Configurações de Conta"
3. **Aba** "API e Webhooks"
4. **Copiar**: 
   - ✅ **Token de Teste (Sandbox)** - Começa com `test_`
   - ⚠️ **NÃO usar** Token de Produção ainda

### 📋 Informações a Coletar

```
✅ API Token (Sandbox):    test_xxxxxxxxxxxxxxxxxxxxxxxx
✅ Account ID:             xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
✅ Webhook Secret:         (será configurado no passo 6)
```

### 💾 Salvar em Arquivo Seguro

```bash
# Criar arquivo .env local (NÃO commitar!)
cat > .env.iugu << 'EOF'
IUGU_API_KEY=test_xxxxxxxxxxxxxxxxxxxxxxxx
IUGU_ACCOUNT_ID=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
IUGU_WEBHOOK_SECRET=
EOF

# Adicionar ao .gitignore
echo ".env.iugu" >> .gitignore
```

---

## 4. Configurar Application

### 📝 Opção 1: application.properties

Editar `/src/main/resources/application.properties`:

```properties
# ============================================================================
# IUGU PAYMENT GATEWAY CONFIGURATION
# ============================================================================

# API Configuration
iugu.api-key=${IUGU_API_KEY:test_SEU_TOKEN_AQUI}
iugu.api-url=https://api.iugu.com/v1

# Account Configuration
iugu.account-id=${IUGU_ACCOUNT_ID:}

# Payment Split Configuration (87% / 5% / 8%)
iugu.split.courier-percentage=0.87
iugu.split.manager-percentage=0.05
iugu.split.platform-percentage=0.08

# Invoice Configuration
iugu.invoice.default-expiration-hours=24
iugu.invoice.max-installments=1
iugu.invoice.late-payment-fine=0.02

# Webhook Configuration
iugu.webhook.secret=${IUGU_WEBHOOK_SECRET:}

# Verification Sync Job
iugu.verification-sync.enabled=true
iugu.verification-sync.cron=0 0 */6 * * *
iugu.verification-sync.max-pending-days=10

# Security (Opcional - para uso futuro)
app.security.encryption.key=${ENCRYPTION_KEY:}
```

### 🔧 Opção 2: Variables de Ambiente

```bash
# Adicionar ao .bashrc ou .zshrc
export IUGU_API_KEY="test_xxxxxxxxxxxxxxxxxxxxxxxx"
export IUGU_ACCOUNT_ID="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"

# Recarregar
source ~/.bashrc
```

### 🐳 Opção 3: Docker Compose (se usar)

```yaml
# compose.yaml
services:
  app:
    environment:
      - IUGU_API_KEY=test_xxxxxxxxxxxxxxxxxxxxxxxx
      - IUGU_ACCOUNT_ID=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
```

### ✅ Verificar Configuração

```bash
# Reiniciar aplicação
pkill -f 'java.*mvt-events'
./gradlew bootRun > app-iugu-sandbox.log 2>&1 &

# Verificar logs
tail -f app-iugu-sandbox.log | grep -i iugu
```

---

## 5. Testar Endpoints

### 🧪 Teste 1: Cadastrar Dados Bancários

```bash
# 1. Fazer login e obter token
TOKEN=$(curl -s -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "joao.motoboy@test.com",
    "password": "Test123!@#"
  }' | python3 -c "import sys, json; print(json.load(sys.stdin)['token'])")

echo "Token: $TOKEN"

# 2. Cadastrar dados bancários (cria subconta Iugu)
curl -X POST "http://localhost:8080/api/motoboy/bank-data" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "bankCode": "341",
    "bankName": "Banco Itaú",
    "agency": "0001",
    "accountNumber": "12345-6",
    "accountType": "CHECKING"
  }' | python3 -m json.tool
```

**Resultado Esperado:**
```json
{
  "id": 1,
  "bankCode": "341",
  "status": "PENDING_VALIDATION",
  "iuguAccountId": "xxxxx",  // ← ID da subconta criada no Iugu
  "canReceivePayments": false
}
```

**Se der erro 401 Unauthorized:**
- Verificar se `iugu.api-key` está correto
- Verificar se é token de TESTE (começa com `test_`)

---

### 🧪 Teste 2: Criar Delivery

```bash
# Criar uma delivery para testar pagamento
curl -X POST "http://localhost:8080/api/deliveries" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "fromAddress": "Rua A, 123, São Paulo - SP",
    "toAddress": "Rua B, 456, São Paulo - SP",
    "fromLatitude": -23.550520,
    "fromLongitude": -46.633308,
    "toLatitude": -23.561684,
    "toLongitude": -46.656139,
    "recipientName": "João Silva",
    "recipientPhone": "(11) 98765-4321",
    "totalAmount": 50.00
  }' | python3 -m json.tool
```

---

### 🧪 Teste 3: Criar Pagamento PIX com Split

```bash
# Obter dados necessários
DELIVERY_ID=1  # ID da delivery criada
MOTOBOY_IUGU_ID=$(curl -s "http://localhost:8080/api/motoboy/bank-data" \
  -H "Authorization: Bearer $TOKEN" | python3 -c "import sys, json; print(json.load(sys.stdin).get('iuguAccountId', ''))")

echo "Delivery ID: $DELIVERY_ID"
echo "Motoboy Iugu ID: $MOTOBOY_IUGU_ID"

# Criar pagamento
curl -X POST "http://localhost:8080/api/payment/create-with-split" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{
    \"deliveryIds\": [$DELIVERY_ID],
    \"amount\": 50.00,
    \"clientEmail\": \"cliente@test.com\",
    \"motoboyAccountId\": \"$MOTOBOY_IUGU_ID\",
    \"description\": \"Pagamento de entrega #$DELIVERY_ID\",
    \"expirationHours\": 24
  }" | python3 -m json.tool
```

**Resultado Esperado:**
```json
{
  "paymentId": 1,
  "iuguInvoiceId": "XXXXXX",
  "pixQrCode": "00020126360014BR.GOV.BCB.PIX...",
  "pixQrCodeUrl": "https://faturas.iugu.com/qr/xxxxx.png",
  "secureUrl": "https://faturas.iugu.com/xxxxx",
  "amount": 50.00,
  "status": "PENDING",
  "expiresAt": "2025-12-04T18:00:00",
  "expired": false,
  "statusMessage": "⏳ Aguardando pagamento..."
}
```

---

## 6. Configurar Webhooks

### 🔗 Configurar URL de Webhook no Iugu

1. **Acessar Dashboard Iugu** → "API e Webhooks"
2. **Clicar** "Adicionar Webhook"
3. **Configurar**:

```
URL: https://SEU_DOMINIO.com/api/webhooks/iugu
Eventos:
  ✅ invoice.status_changed
  ✅ invoice.payment_failed
  ✅ invoice.refunded
  ✅ withdrawal.completed
```

### 🌐 Opções para Expor Localhost

#### Opção A: ngrok (Recomendado para testes)

```bash
# 1. Instalar ngrok
# https://ngrok.com/download

# 2. Criar túnel
ngrok http 8080

# 3. Copiar URL pública (ex: https://xxxx.ngrok.io)
# 4. Usar no Iugu: https://xxxx.ngrok.io/api/webhooks/iugu
```

#### Opção B: localtunnel

```bash
npm install -g localtunnel
lt --port 8080
```

#### Opção C: Deploy temporário (Render/Railway)

```bash
# Deploy rápido no Render.com ou Railway.app
# URL será algo como: https://mvt-events.onrender.com
```

### 🔐 Obter Webhook Secret

1. Após criar webhook no Iugu
2. **Copiar** "Webhook Secret" exibido
3. **Adicionar** em `application.properties`:

```properties
iugu.webhook.secret=seu_webhook_secret_aqui
```

---

## 7. Testes End-to-End

### 🎯 Fluxo Completo

```bash
#!/bin/bash
# test-iugu-e2e.sh

echo "🧪 TESTE END-TO-END IUGU SANDBOX"
echo "================================"

# 1. Login
echo "1️⃣ Fazendo login..."
TOKEN=$(curl -s -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username": "joao.motoboy@test.com", "password": "Test123!@#"}' \
  | python3 -c "import sys, json; print(json.load(sys.stdin)['token'])")

# 2. Cadastrar dados bancários
echo "2️⃣ Cadastrando dados bancários..."
BANK_RESPONSE=$(curl -s -X POST "http://localhost:8080/api/motoboy/bank-data" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "bankCode": "341",
    "bankName": "Banco Itaú",
    "agency": "0001",
    "accountNumber": "12345-6",
    "accountType": "CHECKING"
  }')

echo "$BANK_RESPONSE" | python3 -m json.tool
IUGU_ACCOUNT_ID=$(echo "$BANK_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin).get('iuguAccountId', ''))")

# 3. Criar delivery
echo "3️⃣ Criando delivery..."
DELIVERY_RESPONSE=$(curl -s -X POST "http://localhost:8080/api/deliveries" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "fromAddress": "Rua A, 123",
    "toAddress": "Rua B, 456",
    "fromLatitude": -23.550520,
    "fromLongitude": -46.633308,
    "toLatitude": -23.561684,
    "toLongitude": -46.656139,
    "recipientName": "João Silva",
    "recipientPhone": "(11) 98765-4321",
    "totalAmount": 50.00
  }')

DELIVERY_ID=$(echo "$DELIVERY_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin)['id'])")

# 4. Criar pagamento
echo "4️⃣ Criando pagamento PIX..."
PAYMENT_RESPONSE=$(curl -s -X POST "http://localhost:8080/api/payment/create-with-split" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{
    \"deliveryIds\": [$DELIVERY_ID],
    \"amount\": 50.00,
    \"clientEmail\": \"cliente@test.com\",
    \"motoboyAccountId\": \"$IUGU_ACCOUNT_ID\",
    \"description\": \"Pagamento teste\",
    \"expirationHours\": 24
  }")

echo "$PAYMENT_RESPONSE" | python3 -m json.tool

# 5. Extrair QR Code
PIX_QR=$(echo "$PAYMENT_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin).get('pixQrCode', ''))")
SECURE_URL=$(echo "$PAYMENT_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin).get('secureUrl', ''))")

echo ""
echo "✅ TESTE CONCLUÍDO!"
echo ""
echo "📱 PIX QR Code: ${PIX_QR:0:50}..."
echo "🔗 URL Pagamento: $SECURE_URL"
echo ""
echo "👉 Próximo passo: Acessar URL e pagar com PIX de teste"
```

### 💳 Pagar Fatura de Teste

1. **Abrir** `secureUrl` no navegador
2. **Escanear** QR Code PIX (ou copiar código)
3. **Pagar** usando app de teste do Iugu ou PIX fake

### 🎯 Verificar Webhook Recebido

```bash
# Monitorar logs
tail -f app-iugu-sandbox.log | grep -i webhook
```

**Webhook esperado:**
```json
{
  "event": "invoice.status_changed",
  "data": {
    "id": "XXXXXX",
    "status": "paid",
    "paid_at": "2025-12-03T18:00:00"
  }
}
```

---

## 8. Troubleshooting

### ❌ Problema: 401 Unauthorized

**Causa**: API Key inválida ou não configurada

**Solução**:
```bash
# Verificar configuração
grep "iugu.api-key" src/main/resources/application.properties

# Testar API Key manualmente
curl -u "test_SEU_TOKEN:" https://api.iugu.com/v1/accounts
```

---

### ❌ Problema: Subconta não criada

**Causa**: Dados bancários inválidos ou conta Iugu não verificada

**Solução**:
1. Verificar se completou cadastro no Iugu
2. Usar dados bancários válidos (mesmo que fictícios no sandbox)
3. Verificar logs: `grep "SubAccount" app-iugu-sandbox.log`

---

### ❌ Problema: PIX QR Code não gerado

**Causa**: Split inválido ou subconta inexistente

**Solução**:
1. Verificar se percentuais somam 100%:
   ```properties
   iugu.split.courier-percentage=0.87
   iugu.split.manager-percentage=0.05
   iugu.split.platform-percentage=0.08
   # Total: 0.87 + 0.05 + 0.08 = 1.00 ✅
   ```
2. Verificar se motoboy tem `iuguAccountId`

---

### ❌ Problema: Webhook não recebido

**Causa**: URL não acessível ou secret incorreto

**Solução**:
1. Testar webhook manualmente:
   ```bash
   curl -X POST "http://localhost:8080/api/webhooks/iugu" \
     -H "Content-Type: application/json" \
     -d '{
       "event": "invoice.status_changed",
       "data": {"id": "TEST", "status": "paid"}
     }'
   ```
2. Verificar ngrok está ativo
3. Verificar logs de erro

---

## 📊 Checklist Final

### ✅ Sandbox Configurado

- [ ] Conta Iugu criada
- [ ] API Token obtido (começa com `test_`)
- [ ] `application.properties` configurado
- [ ] Aplicação reiniciada com novas configs

### ✅ Endpoints Testados

- [ ] POST /api/motoboy/bank-data → Subconta criada
- [ ] GET /api/motoboy/bank-data → Dados retornados
- [ ] POST /api/payment/create-with-split → Invoice criada
- [ ] PIX QR Code gerado

### ✅ Webhooks Configurados

- [ ] URL webhook registrada no Iugu
- [ ] ngrok ou deploy público ativo
- [ ] Webhook secret configurado
- [ ] Teste de webhook OK

### ✅ Fluxo E2E

- [ ] Pagamento criado
- [ ] PIX pago (teste)
- [ ] Webhook recebido
- [ ] Status atualizado para COMPLETED

---

## 🎉 Próximos Passos

Após sandbox funcionando:

1. **Atualizar Roadmap** → Marcar sandbox como ✅
2. **Criar Testes Automatizados** → Unit + Integration
3. **Documentar Processo** → Para time
4. **Planejar Produção** → Credentials reais, monitoramento

---

## 📚 Recursos

- **Iugu Docs**: https://dev.iugu.com/docs
- **Iugu API Reference**: https://dev.iugu.com/reference
- **Iugu Sandbox**: https://iugu.com/
- **ngrok**: https://ngrok.com/
- **Projeto**: `/docs/IUGU_*.md`

---

**Gerado em**: 03/12/2025  
**Versão**: 1.0  
**Status**: 📝 Pronto para execução
