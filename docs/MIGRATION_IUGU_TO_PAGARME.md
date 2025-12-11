# 🔄 Plano de Migração: Iugu → Pagar.me

**Data:** 9 de dezembro de 2025  
**Status:** 📋 Planejamento

---

## 📊 Visão Geral

Migração do gateway de pagamentos de **Iugu** para **Pagar.me** (antigo Pagar.me, agora Stone Co.)

### Motivação
- [ ] Custos menores
- [ ] Melhor suporte
- [ ] Funcionalidades específicas
- [ ] Outro: _____________

---

## 🎯 Funcionalidades Atuais (Iugu)

### ✅ O que temos implementado:
1. **Subcontas (Marketplace)**
   - Criar subconta para courier/organizer
   - Atualizar dados bancários
   - Verificação automática de dados bancários
   - Auto-withdraw (D+1)

2. **Pagamentos PIX**
   - Criar invoice com QR Code PIX
   - Split automático (87% courier, 5% manager, 8% platform)
   - Webhook de confirmação de pagamento
   - Invoice consolidada (múltiplas deliveries)

3. **Sincronização**
   - Job schedulado para verificar status de contas
   - Validação de webhook signatures

4. **Entidades**
   - `Payment` com campos Iugu-specific
   - `BankAccount` com validação Febraban
   - `User` com `iuguAccountId`, `bankDataComplete`, `autoWithdrawEnabled`

---

## 🔄 Mapeamento Iugu → Pagar.me

### 1. Conceitos Equivalentes

| Iugu | Pagar.me | Notas |
|------|----------|-------|
| Subconta (Sub-account) | Recipient | Funcionalidade similar |
| Invoice | Order + Charge | Pagar.me separa conceitos |
| Split de Pagamento | Split Rules | Sintaxe diferente |
| Webhook Token | Webhook Secret | Validação HMAC SHA256 |
| Auto-withdraw | Transfer Rules | Configuração automática |
| Bank Verification | Recipient KYC | Processo de verificação |

### 2. Endpoints API

| Função | Iugu | Pagar.me |
|--------|------|----------|
| Criar subconta | `POST /marketplace/create_account` | `POST /recipients` |
| Atualizar banco | `PUT /accounts/{id}/bank_verification` | `PUT /recipients/{id}` |
| Criar pagamento | `POST /invoices` | `POST /orders` |
| Webhook | Custom token | HMAC SHA256 |
| Status conta | `GET /accounts/{id}` | `GET /recipients/{id}` |

### 3. Criar Recipient (Subconta)

#### Request - Criar Recipient
```json
POST https://api.pagar.me/core/v5/recipients
Authorization: Basic {API_KEY_BASE64}
Content-Type: application/json

{
  "name": "João Silva Santos",
  "email": "courier1@mvt.com",
  "description": "Courier - MVT Events",
  "document": "12345678901",
  "type": "individual",
  "default_bank_account": {
    "holder_name": "João Silva Santos",
    "holder_type": "individual",
    "holder_document": "12345678901",
    "bank": "237",
    "branch_number": "1234",
    "branch_check_digit": "5",
    "account_number": "12345",
    "account_check_digit": "6",
    "type": "checking"
  },
  "transfer_settings": {
    "transfer_enabled": true,
    "transfer_interval": "daily",
    "transfer_day": 1
  },
  "automatic_anticipation_settings": {
    "enabled": false
  }
}
```

#### Response - Recipient Criado
```json
{
  "id": "rp_abc123xyz",
  "name": "João Silva Santos",
  "email": "courier1@mvt.com",
  "document": "12345678901",
  "description": "Courier - MVT Events",
  "type": "individual",
  "status": "active",
  "created_at": "2025-12-09T12:00:00Z",
  "updated_at": "2025-12-09T12:00:00Z",
  "default_bank_account": {
    "id": "ba_xyz789",
    "holder_name": "João Silva Santos",
    "holder_type": "individual",
    "holder_document": "12345678901",
    "bank": "237",
    "branch_number": "1234",
    "branch_check_digit": "5",
    "account_number": "12345",
    "account_check_digit": "6",
    "type": "checking",
    "status": "active"
  },
  "transfer_settings": {
    "transfer_enabled": true,
    "transfer_interval": "daily",
    "transfer_day": 1
  }
}
```

**⚠️ Campos Importantes:**
- `status: "active"` - Recipient pronto para receber
- `transfer_enabled: true` - Transferências automáticas D+1
- `transfer_interval: "daily"` - Todos os dias
- `transfer_day: 1` - Repasse no próximo dia útil

### 4. Formato de Dados

#### Split Rules (87% courier, 5% manager, 8% platform)

```json
// IUGU (antigo)
{
  "splits": [
    {
      "recipient_account_id": "courier_account_id",
      "percent": 87.0
    },
    {
      "recipient_account_id": "manager_account_id", 
      "percent": 5.0
    }
    // Platform (8%) fica na conta master automaticamente
  ]
}

// PAGAR.ME (novo)
{
  "split": [
    {
      "amount": 87,
      "type": "percentage",
      "recipient_id": "rp_courier_xxx",
      "options": {
        "liable": false,              // Não responsável por chargeback
        "charge_processing_fee": false, // Não paga taxa de processamento
        "charge_remainder_fee": false   // Não recebe sobra de arredondamento
      }
    },
    {
      "amount": 5,
      "type": "percentage", 
      "recipient_id": "rp_manager_xxx",
      "options": {
        "liable": false,
        "charge_processing_fee": false,
        "charge_remainder_fee": false
      }
    }
    // Platform (8%) automaticamente fica na conta principal
    // Não precisa criar split para a plataforma!
  ]
}
```

**⚠️ IMPORTANTE - Diferenças Pagar.me:**
1. **Percentual sem decimais**: `87` ao invés de `87.0`
2. **Platform NÃO vai no split**: Os 8% sobram automaticamente na conta principal
3. **Options obrigatórias**: `liable`, `charge_processing_fee`, `charge_remainder_fee`
4. **Primeiro recipient (liable=true)**: É responsável por chargebacks e taxas

**💡 Nossa configuração recomendada:**
- **Courier (87%)**: `liable: false`, não paga taxas, recebe 87% líquido
- **Manager (5%)**: `liable: false`, não paga taxas, recebe 5% líquido  
- **Platform (8%)**: Recebe automaticamente, paga todas as taxas Pagar.me

#### PIX QR Code
```json
// IUGU (antigo)
{
  "pix": {
    "qrcode": "00020126...",
    "qrcode_url": "https://..."
  }
}

// PAGAR.ME (novo)
{
  "charges": [{
    "id": "ch_xxx",
    "status": "pending",
    "amount": 10000,
    "last_transaction": {
      "qr_code": "00020126...",        // Código PIX copia-e-cola
      "qr_code_url": "https://..."      // URL da imagem QR Code
    }
  }]
}
```

---

## 📦 Arquivos a Serem Criados/Modificados

### ✅ 1. Configuration
- [ ] `src/main/java/com/mvt/mvt_events/config/PagarMeConfig.java`
- [ ] `src/main/resources/application.properties` (adicionar configs Pagar.me)

### ✅ 2. DTOs Pagar.me
- [ ] `payment/dto/pagarme/RecipientRequest.java`
- [ ] `payment/dto/pagarme/RecipientResponse.java`
- [ ] `payment/dto/pagarme/OrderRequest.java`
- [ ] `payment/dto/pagarme/OrderResponse.java`
- [ ] `payment/dto/pagarme/ChargeRequest.java`
- [ ] `payment/dto/pagarme/SplitRuleRequest.java`
- [ ] `payment/dto/pagarme/WebhookEvent.java`

### ✅ 3. Service Layer
- [ ] `payment/service/PagarMeService.java` (substituir IuguService)
- [ ] Atualizar `service/PaymentService.java` (usar PagarMeService)
- [ ] Criar `payment/service/PagarMeVerificationSyncService.java`

### ✅ 4. Controllers
- [ ] Atualizar `controller/PaymentController.java`
- [ ] Atualizar `controller/WebhookController.java` (endpoint Pagar.me)
- [ ] Atualizar `controller/BankAccountController.java`

### ✅ 5. Database Migration
- [ ] `V10__migrate_iugu_to_pagarme.sql`
  - Renomear `iugu_account_id` → `recipient_id`
  - Renomear `iugu_invoice_id` → `order_id`
  - Adicionar campos Pagar.me se necessário

### ✅ 6. Entities
- [ ] Atualizar `jpa/User.java` (campos específicos gateway)
- [ ] Atualizar `jpa/Payment.java` (campos específicos gateway)
- [ ] Atualizar `jpa/BankAccount.java` (se necessário)

### ✅ 7. Documentation
- [ ] `docs/PAGARME_INTEGRATION.md`
- [ ] `docs/PAGARME_API_GUIDE.md`
- [ ] Atualizar `docs/README.md`

### ✅ 8. Tests
- [ ] `payment/service/PagarMeServiceTest.java`
- [ ] `controller/WebhookControllerTest.java` (Pagar.me)

---

## 🔧 Configurações Pagar.me

### application.properties
```properties
# ==============================================================================
# PAGAR.ME PAYMENT GATEWAY CONFIGURATION
# ==============================================================================
# Documentação: https://docs.pagar.me/reference/introducao-a-api
# Dashboard: https://dashboard.pagar.me

# Modo: dry-run (mock), sandbox (test), production
pagarme.mode=${PAGARME_MODE:dry-run}

# API Configuration
pagarme.api.url=${PAGARME_API_URL:https://api.pagar.me/core/v5}
pagarme.api.key=${PAGARME_API_KEY:sk_test_PLACEHOLDER}
pagarme.public.key=${PAGARME_PUBLIC_KEY:pk_test_PLACEHOLDER}

# Account Configuration  
pagarme.account.id=${PAGARME_ACCOUNT_ID:acc_PLACEHOLDER}

# Webhook Configuration
pagarme.webhook.secret=${PAGARME_WEBHOOK_SECRET:PLACEHOLDER}

# Split Configuration (percentuais)
pagarme.split.courier-percentage=87.0
pagarme.split.manager-percentage=5.0
pagarme.split.platform-percentage=8.0

# Transfer Configuration
pagarme.transfer.enabled=true
pagarme.transfer.interval=daily
pagarme.transfer.day=1

# Verification Sync Job
pagarme.verification-sync.enabled=true
pagarme.verification-sync.cron=0 0 */6 * * *
pagarme.verification-sync.max-pending-days=10

# Timeouts
pagarme.api.connect-timeout=10000
pagarme.api.read-timeout=30000
```

---

## 💡 Exemplo Prático: Entrega de R$ 100,00

### Cenário
- **Valor da entrega**: R$ 100,00
- **Courier (João)**: Deve receber 87% = R$ 87,00
- **Manager (Maria)**: Deve receber 5% = R$ 5,00  
- **Platform (MVT)**: Deve receber 8% = R$ 8,00

### Request Pagar.me (Create Order)

```json
POST https://api.pagar.me/core/v5/orders
Authorization: Basic {API_KEY_BASE64}
Content-Type: application/json

{
  "customer": {
    "name": "Cliente Farmácia Pague Menos",
    "email": "client1@mvt.com",
    "document": "07123456000189",
    "document_type": "CNPJ",
    "type": "company"
  },
  "items": [
    {
      "amount": 10000,
      "description": "Entrega #D-001 - Farmácia Pague Menos",
      "quantity": 1,
      "code": "DELIVERY-001"
    }
  ],
  "payments": [
    {
      "payment_method": "pix",
      "pix": {
        "expires_in": 86400,
        "additional_information": [
          {
            "name": "Entrega",
            "value": "D-001"
          }
        ]
      },
      "split": [
        {
          "amount": 87,
          "type": "percentage",
          "recipient_id": "rp_joao_courier_xxx",
          "options": {
            "liable": false,
            "charge_processing_fee": false,
            "charge_remainder_fee": false
          }
        },
        {
          "amount": 5,
          "type": "percentage",
          "recipient_id": "rp_maria_manager_xxx",
          "options": {
            "liable": false,
            "charge_processing_fee": false,
            "charge_remainder_fee": false
          }
        }
      ]
    }
  ]
}
```

### Response Pagar.me

```json
{
  "id": "or_abc123xyz",
  "code": "OR-12345",
  "amount": 10000,
  "currency": "BRL",
  "status": "pending",
  "created_at": "2025-12-09T00:00:00Z",
  "customer": {
    "id": "cus_cliente001",
    "name": "Cliente Farmácia Pague Menos"
  },
  "charges": [
    {
      "id": "ch_xyz789",
      "code": "CH-67890",
      "amount": 10000,
      "status": "pending",
      "payment_method": "pix",
      "last_transaction": {
        "id": "tran_pix001",
        "transaction_type": "pix",
        "qr_code": "00020126580014br.gov.bcb.pix...",
        "qr_code_url": "https://pagar.me/pix/qr/xyz789.png",
        "expires_at": "2025-12-10T00:00:00Z"
      }
    }
  ],
  "split": [
    {
      "id": "sr_split001",
      "type": "percentage",
      "amount": 87,
      "recipient": {
        "id": "rp_joao_courier_xxx",
        "name": "João Silva Santos",
        "email": "courier1@mvt.com",
        "document": "12345678901",
        "status": "active"
      },
      "options": {
        "liable": false,
        "charge_processing_fee": false,
        "charge_remainder_fee": false
      }
    },
    {
      "id": "sr_split002",
      "type": "percentage",
      "amount": 5,
      "recipient": {
        "id": "rp_maria_manager_xxx",
        "name": "Maria Manager",
        "email": "organizer1@mvt.com",
        "document": "98765432100",
        "status": "active"
      },
      "options": {
        "liable": false,
        "charge_processing_fee": false,
        "charge_remainder_fee": false
      }
    }
  ]
}
```

### Distribuição Final

| Recebedor | Percentual | Valor | Quando Recebe |
|-----------|------------|-------|---------------|
| João (Courier) | 87% | R$ 87,00 | D+1 (transfer automático) |
| Maria (Manager) | 5% | R$ 5,00 | D+1 (transfer automático) |
| MVT (Platform) | 8% | R$ 8,00 | Fica na conta principal |
| **Taxa Pagar.me** | ~2% | ~R$ 2,00 | Descontada do platform |
| **TOTAL** | 100% | R$ 100,00 | Cliente paga via PIX |

**📝 Notas:**
- O split acontece automaticamente após confirmação do PIX
- Transfers para recipients são D+1 (próximo dia útil)
- Platform recebe 8% menos taxas Pagar.me (~2%)
- Courier e Manager recebem valores líquidos (sem desconto de taxas)

---

## 📋 Checklist de Migração

### Fase 1: Preparação (Estimativa: 2 dias)
- [ ] Criar conta Pagar.me (sandbox)
- [ ] Obter API keys (test + production)
- [ ] Configurar webhook endpoint no dashboard Pagar.me
- [ ] Estudar documentação API Pagar.me
- [ ] Criar `PagarMeConfig.java`
- [ ] Adicionar dependências se necessário

### Fase 2: Implementação Core (Estimativa: 3 dias)
- [ ] Criar DTOs Pagar.me
- [ ] Implementar `PagarMeService.java`
  - [ ] `createRecipient()` (equivalente a createSubAccount)
  - [ ] `updateRecipient()` (equivalente a updateBankAccount)
  - [ ] `createOrder()` (equivalente a createInvoice)
  - [ ] `validateWebhookSignature()`
  - [ ] `getRecipientStatus()`
- [ ] Criar `PagarMeVerificationSyncService.java`
- [ ] Atualizar `PaymentService.java` para usar Pagar.me

### Fase 3: Controllers & Webhooks (Estimativa: 1 dia)
- [ ] Atualizar `PaymentController.java`
- [ ] Atualizar `BankAccountController.java`
- [ ] Criar novo endpoint webhook `/api/webhooks/pagarme`
- [ ] Implementar validação HMAC SHA256

### Fase 4: Database (Estimativa: 1 dia)
- [ ] Criar migration V10
- [ ] Testar migration em banco local
- [ ] Backup dados produção antes de migrar

### Fase 5: Testes (Estimativa: 2 dias)
- [ ] Testes unitários `PagarMeService`
- [ ] Testes integração webhook
- [ ] Testes end-to-end (sandbox):
  - [ ] Criar recipient
  - [ ] Criar order com PIX
  - [ ] Simular pagamento via sandbox
  - [ ] Verificar webhook recebido
  - [ ] Validar split executado

### Fase 6: Documentação (Estimativa: 1 dia)
- [ ] `PAGARME_INTEGRATION.md`
- [ ] `PAGARME_API_GUIDE.md`
- [ ] Atualizar README.md
- [ ] Criar guia de migração para outros devs

### Fase 7: Deploy (Estimativa: 1 dia)
- [ ] Deploy em staging
- [ ] Testes em staging
- [ ] Deploy em produção (janela de manutenção)
- [ ] Monitorar logs por 24h

---

## ⚠️ Pontos de Atenção

### 🔴 Crítico
1. **Dados Bancários Existentes**: Precisamos recriar recipients no Pagar.me para todos os users que já têm `iuguAccountId`
2. **Pagamentos Pendentes**: Verificar se há invoices Iugu pendentes antes de migrar
3. **Webhook Downtime**: Durante migração, webhooks Iugu não funcionarão
4. **Split Calculation**: Validar que os percentuais batem exatamente

### 🟡 Importante
1. **Credenciais**: Não commitar API keys reais
2. **Dry-run Mode**: Implementar mock para testes sem chamar API
3. **Rollback Plan**: Manter código Iugu funcionando em paralelo inicialmente
4. **Logs**: Adicionar logs detalhados para debug

### 🟢 Nice to Have
1. **Feature Flag**: Criar toggle para alternar entre Iugu/Pagar.me
2. **Metrics**: Adicionar métricas de sucesso/erro de pagamentos
3. **Admin Panel**: Interface para visualizar recipients/orders

---

## 🚀 Estratégia de Deploy

### Opção A: Big Bang (Recomendado para MVP)
- Migrar tudo de uma vez em janela de manutenção
- Pros: Simples, rápido
- Cons: Maior risco, downtime

### Opção B: Gradual (Recomendado para Produção)
1. Deploy código Pagar.me sem ativar
2. Feature flag: `payment.gateway=iugu` (default)
3. Testar Pagar.me em staging
4. Mudar flag: `payment.gateway=pagarme`
5. Monitorar 24h
6. Remover código Iugu após 1 semana

---

## 📚 Recursos

### Documentação Pagar.me
- [API Reference](https://docs.pagar.me/reference/introducao-a-api)
- [Webhooks](https://docs.pagar.me/docs/webhooks-1)
- [Split de Pagamento](https://docs.pagar.me/docs/split-de-pagamentos)
- [PIX](https://docs.pagar.me/docs/pix)
- [Recipients (Marketplace)](https://docs.pagar.me/docs/marketplace)

### Comparação
- [Iugu vs Pagar.me](https://www.capterra.com.br/compare/158138/176421/iugu/pagar-me)

---

## ✅ Critérios de Sucesso

- [ ] 100% dos recipients migrados com sucesso
- [ ] PIX QR Code gerado corretamente
- [ ] Split executado com valores corretos (87/5/8)
- [ ] Webhooks recebidos e processados
- [ ] Zero erros em produção após 24h
- [ ] Tempo de resposta < 2s para criar order
- [ ] Documentação completa

---

## 📊 Estimativa Total

| Fase | Dias | Desenvolvedor |
|------|------|---------------|
| Preparação | 2 | 1 dev |
| Implementação Core | 3 | 1 dev |
| Controllers & Webhooks | 1 | 1 dev |
| Database | 1 | 1 dev |
| Testes | 2 | 1 dev |
| Documentação | 1 | 1 dev |
| Deploy | 1 | 1 dev |
| **TOTAL** | **11 dias** | **~2 semanas** |

---

## 🎯 Próximos Passos

1. ✅ Criar este documento de planejamento
2. ⏳ Obter aprovação do time
3. ⏳ Criar conta Pagar.me sandbox
4. ⏳ Iniciar implementação `PagarMeService`
5. ⏳ ...

---

**Documento criado por:** GitHub Copilot  
**Última atualização:** 9 de dezembro de 2025
