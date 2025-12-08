# 📊 Guia de Validação - Logging Detalhado de Invoice Consolidada

**Data**: 04/12/2025  
**Versão**: 1.0  

---

## 🎯 Objetivo

Este documento explica o **logging detalhado** implementado para facilitar a validação dos cálculos de splits nas invoices consolidadas.

---

## 📋 Fluxo de Logs

### 1️⃣ Controller - Request Recebido

```log
═══════════════════════════════════════════════════════════════
📨 REQUEST RECEBIDO - Invoice Consolidada
═══════════════════════════════════════════════════════════════
📦 Delivery IDs: [1, 13]
📧 Client Email: cliente@example.com
⏰ Expiration Hours: 24 (padrão)
───────────────────────────────────────────────────────────────
🔧 cURL equivalente:
curl -X POST 'http://localhost:8080/api/payment/create-invoice' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer YOUR_TOKEN' \
  -d '{
    "deliveryIds": [1, 13],
    "clientEmail": "cliente@example.com",
    "expirationHours": 24
  }'
═══════════════════════════════════════════════════════════════
```

**O que validar:**
- ✅ Delivery IDs recebidos estão corretos
- ✅ Email está correto
- ✅ Comando cURL para reproduzir o teste

---

### 2️⃣ Service - Deliveries Encontradas

```log
═══════════════════════════════════════════════════════════════
📦 DELIVERIES ENCONTRADAS: 2
═══════════════════════════════════════════════════════════════
📦 Delivery #1
   💰 Valor: R$ 25
   👨‍🚀 Motoboy: João Silva Santos (Iugu: joaosilva_acc_123)
   👔 Gerente: Samuel Ferreira Costa (Iugu: samuel_acc_456)
   📍 Status: DELIVERED
───────────────────────────────────────────────────────────────
📦 Delivery #13
   💰 Valor: R$ 15
   👨‍🚀 Motoboy: Carlos Eduardo Lima (Iugu: carlos_acc_789)
   👔 Gerente: Rodrigo Alves Sousa (Iugu: rodrigo_acc_012)
   📍 Status: DELIVERED
───────────────────────────────────────────────────────────────
💰 VALOR TOTAL DAS DELIVERIES: R$ 40
═══════════════════════════════════════════════════════════════
```

**O que validar:**
- ✅ Valores das deliveries estão corretos (R$ 25 + R$ 15 = R$ 40)
- ✅ Motoboys e Gerentes corretos
- ✅ Contas Iugu dos recipients existem

---

### 3️⃣ SplitCalculator - Cálculo Detalhado

```log
═══════════════════════════════════════════════════════════════
📊 CALCULANDO SPLITS CONSOLIDADOS
═══════════════════════════════════════════════════════════════
📦 Deliveries: 2
💰 Percentuais: Motoboy 87%, Gerente 5%, Plataforma (resto)
───────────────────────────────────────────────────────────────
💰 Valor total: R$ 40.00
───────────────────────────────────────────────────────────────
🔢 CÁLCULO POR DELIVERY:
📦 Delivery #1 - R$ 25
   👨‍🚀 Motoboy: João Silva Santos (joaosilva_acc_123)
      R$ 25 × 87% = R$ 21.75 (2175¢)
   👔 Gerente: Samuel Ferreira Costa (samuel_acc_456)
      R$ 25 × 5% = R$ 1.25 (125¢)
   ───────────────────────────────────────────────────────────
📦 Delivery #13 - R$ 15
   👨‍🚀 Motoboy: Carlos Eduardo Lima (carlos_acc_789)
      R$ 15 × 87% = R$ 13.05 (1305¢)
   👔 Gerente: Rodrigo Alves Sousa (rodrigo_acc_012)
      R$ 15 × 5% = R$ 0.75 (75¢)
   ───────────────────────────────────────────────────────────
```

**O que validar:**
- ✅ **Delivery #1 - R$ 25**:
  - Motoboy (87%): R$ 25 × 0.87 = **R$ 21.75** ✅
  - Gerente (5%): R$ 25 × 0.05 = **R$ 1.25** ✅
  
- ✅ **Delivery #13 - R$ 15**:
  - Motoboy (87%): R$ 15 × 0.87 = **R$ 13.05** ✅
  - Gerente (5%): R$ 15 × 0.05 = **R$ 0.75** ✅

---

### 4️⃣ SplitCalculator - Splits Consolidados

```log
═══════════════════════════════════════════════════════════════
✅ SPLITS CONSOLIDADOS (após agrupamento):
═══════════════════════════════════════════════════════════════
👨‍🚀 MOTOBOYS (2 pessoa(s)):
   joaosilva_acc_123 (2175¢): R$ 21.75
   carlos_acc_789 (1305¢): R$ 13.05
   TOTAL MOTOBOYS: R$ 34.80
───────────────────────────────────────────────────────────────
👔 GERENTES (2 pessoa(s)):
   samuel_acc_456 (125¢): R$ 1.25
   rodrigo_acc_012 (75¢): R$ 0.75
   TOTAL GERENTES: R$ 2.00
───────────────────────────────────────────────────────────────
🏢 PLATAFORMA: R$ 3.20
───────────────────────────────────────────────────────────────
💰 TOTAL GERAL: R$ 40.00
═══════════════════════════════════════════════════════════════
```

**O que validar:**
- ✅ **Soma dos Motoboys**: R$ 21.75 + R$ 13.05 = **R$ 34.80** ✅
- ✅ **Soma dos Gerentes**: R$ 1.25 + R$ 0.75 = **R$ 2.00** ✅
- ✅ **Plataforma (resto)**: R$ 40.00 - R$ 34.80 - R$ 2.00 = **R$ 3.20** ✅
- ✅ **Total**: R$ 34.80 + R$ 2.00 + R$ 3.20 = **R$ 40.00** ✅

---

### 5️⃣ IuguService - Request para Iugu

```log
═══════════════════════════════════════════════════════════════
🚀 PREPARANDO REQUEST PARA IUGU
═══════════════════════════════════════════════════════════════
📧 Email: cliente@example.com
💰 Valor Total: R$ 40.00 (4000¢)
📝 Descrição: Pagamento de 2 entregas
⏰ Expiração: 24 horas
───────────────────────────────────────────────────────────────
🔄 Convertendo splits para formato Iugu (excluindo PLATFORM):
   ✅ COURIER joaosilva_acc_123: 2175¢ (R$ 21.75)
   ✅ COURIER carlos_acc_789: 1305¢ (R$ 13.05)
   ✅ MANAGER samuel_acc_456: 125¢ (R$ 1.25)
   ✅ MANAGER rodrigo_acc_012: 75¢ (R$ 0.75)
   ⏭️  Pulando PLATFORM (320¢) - receberá automaticamente o resto
───────────────────────────────────────────────────────────────
📦 Splits para Iugu: 4 (PLATFORM não incluído)
═══════════════════════════════════════════════════════════════
📤 ENVIANDO REQUEST PARA IUGU API
═══════════════════════════════════════════════════════════════
```

**O que validar:**
- ✅ Valor total: **4000 centavos = R$ 40.00**
- ✅ **4 splits** enviados ao Iugu (PLATFORM não é enviado, receberá o resto automaticamente)
- ✅ Cada split tem o valor correto em centavos

---

### 6️⃣ IuguService - Resposta do Iugu

```log
═══════════════════════════════════════════════════════════════
✅ RESPOSTA RECEBIDA DO IUGU
═══════════════════════════════════════════════════════════════
🆔 Invoice ID: ABC123DEF456GHI789
🔗 Secure URL: https://faturas.iugu.com/ABC123DEF456GHI789
🖼️  QR Code URL: https://faturas.iugu.com/qr/ABC123DEF456GHI789.png
📋 QR Code: 187 caracteres
═══════════════════════════════════════════════════════════════
```

**O que validar:**
- ✅ Invoice ID foi gerado
- ✅ Secure URL está disponível (para abrir no navegador)
- ✅ QR Code URL está disponível (para exibir imagem)
- ✅ QR Code (string) foi retornado (para copiar/colar)

---

### 7️⃣ Controller - Response Final

```log
═══════════════════════════════════════════════════════════════
✅ INVOICE CRIADA COM SUCESSO
═══════════════════════════════════════════════════════════════
💳 Payment ID: 123
🆔 Iugu Invoice ID: ABC123DEF456GHI789
💰 Valor Total: R$ 40.00
📦 Deliveries: 2
───────────────────────────────────────────────────────────────
👨‍🚀 Motoboys (2 pessoa(s)): R$ 34.80
👔 Gerentes (2 pessoa(s)): R$ 2.00
🏢 Plataforma: R$ 3.20
───────────────────────────────────────────────────────────────
🔗 QR Code URL: https://faturas.iugu.com/qr/ABC123DEF456GHI789.png
🌐 Secure URL: https://faturas.iugu.com/ABC123DEF456GHI789
⏰ Expira em: 2025-12-05T20:30:16.123
═══════════════════════════════════════════════════════════════
```

**O que validar:**
- ✅ Payment ID foi salvo no banco
- ✅ Totais estão corretos
- ✅ URLs do QR Code estão disponíveis para o frontend

---

## 🧪 Teste Prático com suas Deliveries

### Dados Esperados (da screenshot):

**Delivery #1**:
- Valor: R$ 25
- Motoboy: João Silva Santos
- Gerente: Samuel Ferreira Costa

**Delivery #13**:
- Valor: R$ 15
- Motoboy: Carlos Eduardo Lima
- Gerente: Rodrigo Alves Sousa

### Cálculos Esperados:

#### Delivery #1 (R$ 25):
- **Motoboy João (87%)**: R$ 25 × 0.87 = R$ 21.75
- **Gerente Samuel (5%)**: R$ 25 × 0.05 = R$ 1.25

#### Delivery #13 (R$ 15):
- **Motoboy Carlos (87%)**: R$ 15 × 0.87 = R$ 13.05
- **Gerente Rodrigo (5%)**: R$ 15 × 0.05 = R$ 0.75

#### Totais Consolidados:
- **Total Motoboys**: R$ 21.75 + R$ 13.05 = **R$ 34.80**
- **Total Gerentes**: R$ 1.25 + R$ 0.75 = **R$ 2.00**
- **Plataforma**: R$ 40 - R$ 34.80 - R$ 2.00 = **R$ 3.20**
- **TOTAL**: R$ 34.80 + R$ 2.00 + R$ 3.20 = **R$ 40.00** ✅

---

## 🔍 Como Validar

1. **Abra o arquivo de log**:
   ```bash
   tail -f app-boot-detailed-logging.log
   ```

2. **Faça a requisição** (via frontend ou cURL):
   ```bash
   curl -X POST 'http://localhost:8080/api/payment/create-invoice' \
     -H 'Content-Type: application/json' \
     -H 'Authorization: Bearer YOUR_TOKEN' \
     -d '{
       "deliveryIds": [1, 13],
       "clientEmail": "teste@example.com",
       "expirationHours": 24
     }'
   ```

3. **Compare os valores nos logs** com os valores esperados acima

4. **Verifique cada etapa**:
   - ✅ Deliveries carregadas com valores corretos
   - ✅ Cálculo por delivery (87% e 5%)
   - ✅ Agrupamento consolidado
   - ✅ Plataforma recebe o resto
   - ✅ Request para Iugu com splits corretos
   - ✅ Response com QR Code

---

## 📊 Tabela de Validação Rápida

| Item | Valor Esperado | Onde Validar |
|------|---------------|--------------|
| Delivery #1 | R$ 25 | Log "DELIVERIES ENCONTRADAS" |
| Delivery #13 | R$ 15 | Log "DELIVERIES ENCONTRADAS" |
| Total Deliveries | R$ 40 | Log "VALOR TOTAL DAS DELIVERIES" |
| Motoboy João | R$ 21.75 | Log "CÁLCULO POR DELIVERY" |
| Motoboy Carlos | R$ 13.05 | Log "CÁLCULO POR DELIVERY" |
| Gerente Samuel | R$ 1.25 | Log "CÁLCULO POR DELIVERY" |
| Gerente Rodrigo | R$ 0.75 | Log "CÁLCULO POR DELIVERY" |
| Total Motoboys | R$ 34.80 | Log "SPLITS CONSOLIDADOS" |
| Total Gerentes | R$ 2.00 | Log "SPLITS CONSOLIDADOS" |
| Plataforma | R$ 3.20 | Log "SPLITS CONSOLIDADOS" |
| Total Final | R$ 40.00 | Log "TOTAL GERAL" |

---

## ✅ Checklist Final

- [ ] Valores das deliveries estão corretos
- [ ] Motoboys e Gerentes estão corretos
- [ ] Cálculo de 87% para motoboy está correto
- [ ] Cálculo de 5% para gerente está correto
- [ ] Soma dos splits bate com o total
- [ ] Plataforma recebe exatamente o resto (8% + ajustes)
- [ ] Request para Iugu tem 4 splits (sem PLATFORM)
- [ ] Response contém pixQrCode, pixQrCodeUrl e secureUrl
- [ ] Frontend exibe QR Code corretamente

---

## 🎯 Próximos Passos

1. Fazer o teste com suas deliveries (IDs 1 e 13)
2. Validar cada linha do log contra esta documentação
3. Confirmar que os valores batem
4. Testar o pagamento PIX no frontend
5. Verificar se o webhook do Iugu atualiza o status corretamente
