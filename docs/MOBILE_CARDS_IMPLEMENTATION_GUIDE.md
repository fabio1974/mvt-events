# 💳 Documentação Mobile - Sistema de Cartões de Crédito

## 📋 Índice
1. [Visão Geral](#visão-geral)
2. [Arquitetura e Fluxo](#arquitetura-e-fluxo)
3. [Endpoints da API Backend](#endpoints-da-api-backend)
4. [Integração com Pagar.me](#integração-com-pagarme)
5. [Componentes Mobile a Implementar](#componentes-mobile-a-implementar)
6. [Código de Exemplo Completo](#código-de-exemplo-completo)
7. [Validações e Segurança](#validações-e-segurança)
8. [Tratamento de Erros](#tratamento-de-erros)
9. [Testes](#testes)

---

## 🎯 Visão Geral

Sistema que permite **CUSTOMERS/CLIENTS** gerenciarem seus cartões de crédito de forma segura (PCI compliant), similar ao Uber:

- ✅ Adicionar múltiplos cartões
- ✅ Listar cartões salvos (apenas últimos 4 dígitos)
- ✅ Definir cartão padrão
- ✅ Deletar cartões
- ✅ Usar cartão salvo em pagamentos
- ✅ Segurança: NUNCA armazena número completo ou CVV

---

## 🏗️ Arquitetura e Fluxo

### ⚠️ IMPORTANTE: Dois Endpoints Diferentes

O mobile faz chamadas para **DOIS sistemas diferentes**:

| Sistema | URL Base | Autenticação | Uso |
|---------|----------|--------------|-----|
| **🔵 Pagar.me** | `https://api.pagar.me/core/v5` | Public Key (`pk_test_xxx`) | Tokenizar cartão |
| **🟢 Zapi10 Backend** | `http://192.168.18.171:8080/api` | JWT Token (Bearer) | Gerenciar cartões |

---

### 📱 Fluxo Completo Passo a Passo (Após Submit do Form)

```
┌─────────────────────────────────────────────────────────────────┐
│ PASSO 1: Validação no Mobile (Frontend)                        │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                                                                 │
│ Usuário preenche form e clica "Adicionar"                      │
│                                                                 │
│ Mobile valida:                                                  │
│ ✓ Número do cartão (Algoritmo de Luhn)                        │
│ ✓ Nome do titular (não vazio)                                 │
│ ✓ Validade (não expirado)                                     │
│ ✓ CVV (3 ou 4 dígitos)                                        │
│                                                                 │
│ ⏱️  Tempo: ~50ms                                               │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ PASSO 2: Tokenização no Pagar.me (Mobile → Pagar.me)          │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                                                                 │
│ 🔵 ENDPOINT PAGAR.ME (não é o Zapi10!)                        │
│                                                                 │
│ POST https://api.pagar.me/core/v5/tokens?appId=pk_test_xxx    │
│                                                                 │
│ Request Body:                                                   │
│ {                                                              │
│   "type": "card",                                             │
│   "card": {                                                   │
│     "number": "4242424242424242",  ← Número completo!         │
│     "holder_name": "JOAO SILVA",                              │
│     "exp_month": 12,                                          │
│     "exp_year": 2026,                                         │
│     "cvv": "123"                    ← CVV!                     │
│   }                                                           │
│ }                                                              │
│                                                                 │
│ ⚠️  DADOS SENSÍVEIS SÓ VÃO DIRETO PRO PAGAR.ME!               │
│ ⚠️  SEU BACKEND NUNCA VÊ ESSES DADOS!                         │
│                                                                 │
│ Response 200:                                                   │
│ {                                                              │
│   "id": "tok_abc123xyz",           ← Token temporário (30min) │
│   "type": "card",                                             │
│   "created_at": "2026-02-02T10:30:00Z"                        │
│ }                                                              │
│                                                                 │
│ O que o Pagar.me faz:                                          │
│ ✓ Valida o cartão com a bandeira                              │
│ ✓ Criptografa os dados                                        │
│ ✓ Cria TOKEN TEMPORÁRIO (validade: 30 minutos)                │
│ ✓ Armazena dados criptografados temporariamente                │
│ ✗ NÃO salva o cartão permanentemente                          │
│ ✗ NÃO cria card_id ainda                                      │
│ ✗ NÃO vincula a nenhum customer                               │
│                                                                 │
│ 💡 Token é uma "prova temporária" de que o cartão é válido    │
│                                                                 │
│ ⏱️  Tempo: ~800ms                                              │
└─────────────────────────────────────────────────────────────────┘
                            ↓
                  Mobile descarta dados sensíveis
                  (número completo e CVV nunca mais são usados)
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ PASSO 3: Enviar Token para Zapi10 (Mobile → Zapi10 Backend)   │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                                                                 │
│ 🟢 ENDPOINT ZAPI10 (seu backend Spring Boot!)                 │
│                                                                 │
│ POST http://192.168.18.171:8080/api/customer-cards            │
│ Headers:                                                        │
│   Authorization: Bearer eyJhbGci...  ← JWT do usuário         │
│   Content-Type: application/json                               │
│                                                                 │
│ Request Body:                                                   │
│ {                                                              │
│   "cardToken": "tok_abc123xyz",    ← Só o token!              │
│   "setAsDefault": true                                         │
│ }                                                              │
│                                                                 │
│ ⚠️  Não envia número, CVV ou dados sensíveis!                 │
│                                                                 │
│ ⏱️  Tempo transmissão: ~200ms                                  │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ PASSO 4: Backend Recebe (CustomerCardController.java)          │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                                                                 │
│ Controller:                                                     │
│ • Extrai customerId do JWT                                     │
│ • Chama CustomerCardService.addCard()                          │
│                                                                 │
│ ⏱️  Tempo: ~50ms                                               │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ PASSO 5: Verificar/Criar Customer no Pagar.me (Backend)        │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                                                                 │
│ Service busca usuário no PostgreSQL:                           │
│ • Se user.pagarme_customer_id == null:                        │
│                                                                 │
│   🔵 Chama Pagar.me:                                           │
│   POST https://api.pagar.me/core/v5/customers                 │
│   Authorization: Basic base64(sk_test_xxx:)  ← Secret Key!    │
│   Body: {name, document, email, phones}                        │
│                                                                 │
│   Response: {"id": "cus_abc123"}                               │
│                                                                 │
│   • Salva cus_abc123 no PostgreSQL (users.pagarme_customer_id)│
│                                                                 │
│ ⏱️  Tempo: ~100ms (criar) ou ~20ms (já existe)                │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ PASSO 6: Criar Cartão no Pagar.me (Backend → Pagar.me)        │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                                                                 │
│ 🔵 ENDPOINT PAGAR.ME (backend usa Secret Key!)                │
│                                                                 │
│ POST https://api.pagar.me/core/v5/customers/cus_abc123/cards  │
│ Authorization: Basic base64(sk_test_xxx:)  ← Secret Key!      │
│                                                                 │
│ Request Body:                                                   │
│ {                                                              │
│   "token": "tok_abc123xyz"         ← Token do Passo 2         │
│ }                                                              │
│                                                                 │
│ O que o Pagar.me faz:                                          │
│ ✓ Valida o token (verifica se ainda é válido)                 │
│ ✓ Descriptografa dados do token                               │
│ ✓ Cria CARD_ID PERMANENTE                                     │
│ ✓ Vincula ao customer (cus_abc123)                            │
│ ✓ Salva metadados: últimos 4 dígitos, bandeira, validade     │
│ ✗ DESCARTA número completo e CVV para sempre                  │
│ ✗ Token tok_abc123 é consumido/expirado                       │
│                                                                 │
│ Response 200:                                                   │
│ {                                                              │
│   "id": "card_xyz789",             ← Card ID permanente!       │
│   "customer_id": "cus_abc123",                                 │
│   "last_four_digits": "4242",                                  │
│   "brand": "Visa",                                             │
│   "holder_name": "JOAO DA SILVA",                              │
│   "exp_month": 12,                                             │
│   "exp_year": 2026,                                            │
│   "status": "active"                                           │
│ }                                                              │
│                                                                 │
│ ⚠️  A partir daqui, card_xyz789 pode ser usado em pagamentos!  │
│ ⚠️  Número completo e CVV foram DESCARTADOS pelo Pagar.me!    │
│                                                                 │
│ ⏱️  Tempo: ~1s                                                 │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ PASSO 7: Salvar no PostgreSQL (Backend)                        │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                                                                 │
│ Service cria CustomerCard entity:                              │
│ • pagarme_card_id = "card_xyz789"                             │
│ • customer_id = UUID do usuário                                │
│ • last_four_digits = "4242"                                    │
│ • brand = "VISA"                                               │
│ • holder_name = "JOAO DA SILVA"                                │
│ • exp_month = 12                                               │
│ • exp_year = 2026                                              │
│ • is_default = true (se for primeiro cartão)                   │
│ • is_active = true                                             │
│                                                                 │
│ INSERT INTO customer_cards (...)                               │
│                                                                 │
│ ⚠️  Nunca salva número completo ou CVV!                        │
│                                                                 │
│ ⏱️  Tempo: ~50ms                                               │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ PASSO 8: Retornar Resposta (Backend → Mobile)                  │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                                                                 │
│ Response 200:                                                   │
│ {                                                              │
│   "id": 1,                         ← ID no PostgreSQL          │
│   "lastFourDigits": "4242",                                    │
│   "brand": "Visa",                                             │
│   "holderName": "JOAO DA SILVA",                               │
│   "expiration": "12/26",                                       │
│   "isDefault": true,                                           │
│   "isActive": true,                                            │
│   "isExpired": false,                                          │
│   "maskedNumber": "Visa **** 4242",                            │
│   "createdAt": "2026-02-02T10:30:00",                          │
│   "lastUsedAt": null                                           │
│ }                                                              │
│                                                                 │
│ ⏱️  Tempo: ~50ms                                               │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ PASSO 9: Exibir Sucesso (Mobile)                               │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                                                                 │
│ Mobile:                                                         │
│ • Limpa formulário                                             │
│ • Mostra Alert: "Cartão Visa **** 4242 adicionado!"           │
│ • Navega de volta para lista de cartões                        │
│                                                                 │
│ ⏱️  Tempo: ~100ms                                              │
│                                                                 │
│ ✅ PROCESSO COMPLETO!                                          │
└─────────────────────────────────────────────────────────────────┘
```

**⏱️ Tempo Total: ~2.3 segundos**

---

### 🔐 Ciclo de Vida dos Dados Sensíveis

| Momento | Número Completo | CVV | Token | Card_ID |
|---------|-----------------|-----|-------|---------|
| **Form Mobile** | ✅ Em memória | ✅ Em memória | ❌ | ❌ |
| **Após Passo 2** | ❌ Descartado | ❌ Descartado | ✅ tok_abc (30min) | ❌ |
| **Após Passo 6** | ❌ | ❌ | ❌ Consumido | ✅ card_xyz |
| **PostgreSQL** | ❌ NUNCA | ❌ NUNCA | ❌ | ✅ card_xyz |

**⏱️ Tempo de vida dos dados sensíveis: ~2-3 segundos**

---

### 💡 Por Que 2 Passos (Tokenização + Criação)?

#### ❌ Se fosse 1 passo só (INSEGURO):
```
Mobile → Backend → Pagar.me
          ↑
     ⚠️ Dados sensíveis passam pelo seu backend!
     ⚠️ Precisa certificação PCI-DSS ($$$$)
```

#### ✅ Modelo Atual (2 passos - SEGURO):
```
Mobile → Pagar.me (tokenizar)    ← Dados sensíveis vão direto
           ↓
        Token
           ↓
Mobile → Backend → Pagar.me (criar cartão)  ← Só token, sem dados sensíveis
```

**Benefícios:**
1. 🔒 **Segurança PCI**: Dados sensíveis nunca passam pelo seu backend
2. 🔑 **Secret Key Protegida**: Mobile usa Public Key, Backend usa Secret Key
3. ⏱️ **Token Temporário**: Se algo falhar, token expira em 30 min
4. ♻️ **Flexibilidade**: Token pode ser usado para pagamento único OU salvar cartão

---

### 🔄 Fluxo de Pagamento (Futuro)

Depois que o cartão está salvo:

```
┌─────────────────┐
│  Mobile App     │
│                 │
│ Seleciona:      │
│ Visa **** 4242  │
│ (card_id:       │
│  card_xyz789)   │
└────────┬────────┘
         │
         │ POST /payments
         │ {cardId: card_xyz789}
         ▼
┌─────────────────┐
│ 🟢 Zapi10       │
│    Backend      │
│                 │
│ Usa card_id     │
│ para cobrar     │
└────────┬────────┘
         │
         │ POST /charges
         │ {customer_id, card_id}
         ▼
┌─────────────────┐
│ 🔵 Pagar.me     │
│                 │
│ Processa        │
│ pagamento       │
└─────────────────┘
```

**⚠️ Nunca precisa pedir número/CVV novamente!**

---

## 🔌 Endpoints da API Backend

### Base URL
```
http://192.168.18.171:8080/api
ou
https://your-production-domain.com/api
```

### Autenticação
Todos os endpoints requerem **Bearer Token** no header:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

---

### 1. **POST /customer-cards**
Adiciona um novo cartão.

**Request:**
```json
{
  "cardToken": "tok_abc123xyz",
  "setAsDefault": true
}
```

**Response 200:**
```json
{
  "id": 1,
  "lastFourDigits": "4242",
  "brand": "Visa",
  "holderName": "JOAO DA SILVA",
  "expiration": "12/26",
  "isDefault": true,
  "isActive": true,
  "isExpired": false,
  "maskedNumber": "Visa **** 4242",
  "createdAt": "2026-02-02T10:30:00",
  "lastUsedAt": null
}
```

**Errors:**
- `400 Bad Request` - Token inválido ou cartão já cadastrado
- `401 Unauthorized` - Token JWT inválido/expirado
- `500 Internal Server Error` - Erro no Pagar.me

---

### 2. **GET /customer-cards**
Lista todos os cartões do usuário logado.

**Response 200:**
```json
[
  {
    "id": 1,
    "lastFourDigits": "4242",
    "brand": "Visa",
    "holderName": "JOAO DA SILVA",
    "expiration": "12/26",
    "isDefault": true,
    "isActive": true,
    "isExpired": false,
    "maskedNumber": "Visa **** 4242",
    "createdAt": "2026-02-02T10:30:00",
    "lastUsedAt": "2026-02-02T15:45:00"
  },
  {
    "id": 2,
    "lastFourDigits": "5555",
    "brand": "Mastercard",
    "holderName": "JOAO DA SILVA",
    "expiration": "03/27",
    "isDefault": false,
    "isActive": true,
    "isExpired": false,
    "maskedNumber": "Mastercard **** 5555",
    "createdAt": "2026-02-03T08:15:00",
    "lastUsedAt": null
  }
]
```

**Ordenação:** Padrão primeiro, depois por último uso.

---

### 3. **GET /customer-cards/default**
Retorna o cartão padrão do usuário.

**Response 200:**
```json
{
  "id": 1,
  "lastFourDigits": "4242",
  "brand": "Visa",
  "maskedNumber": "Visa **** 4242",
  "isDefault": true,
  ...
}
```

**Response 404:**
```json
{
  "error": "Cliente não possui cartão padrão cadastrado"
}
```

---

### 4. **PUT /customer-cards/{cardId}/set-default**
Define um cartão como padrão.

**Response 200:**
```json
{
  "id": 2,
  "isDefault": true,
  ...
}
```

---

### 5. **DELETE /customer-cards/{cardId}**
Remove um cartão (soft delete).

**Response 200:**
```json
{
  "message": "Cartão removido com sucesso"
}
```

---

### 6. **GET /customer-cards/has-cards**
Verifica se usuário tem cartões cadastrados.

**Response 200:**
```json
{
  "hasCards": true
}
```

---

## 🔐 Integração com Pagar.me

### Chaves de API

**Sandbox (Testes):**
```javascript
const PAGARME_PUBLIC_KEY = 'pk_test_xxxxxxxxxxxxxxxx';
const PAGARME_API_URL = 'https://api.pagar.me/core/v5';
```

**Produção:**
```javascript
const PAGARME_PUBLIC_KEY = 'pk_live_xxxxxxxxxxxxxxxx';
const PAGARME_API_URL = 'https://api.pagar.me/core/v5';
```

### Tokenização de Cartão

**Endpoint:** `POST https://api.pagar.me/core/v5/tokens?appId={PUBLIC_KEY}`

**Request:**
```json
{
  "type": "card",
  "card": {
    "number": "4242424242424242",
    "holder_name": "JOAO DA SILVA",
    "exp_month": 12,
    "exp_year": 2026,
    "cvv": "123"
  }
}
```

**Response:**
```json
{
  "id": "tok_abc123xyz",
  "type": "card",
  "created_at": "2026-02-02T10:30:00Z"
}
```

### Cartões de Teste (Sandbox)

| Bandeira | Número | CVV | Resultado |
|----------|--------|-----|-----------|
| Visa | 4242 4242 4242 4242 | 123 | Aprovado |
| Mastercard | 5555 5555 5555 4444 | 123 | Aprovado |
| Amex | 3782 822463 10005 | 1234 | Aprovado |
| Elo | 6362 9707 0000 0000 01 | 123 | Aprovado |
| Visa (Falha) | 4000 0000 0000 0002 | 123 | Negado |

**Validade:** Qualquer data futura (ex: 12/2030)  
**Nome:** Qualquer nome

---

## 📱 Componentes Mobile a Implementar

### Estrutura de Arquivos

```
src/
├── screens/
│   ├── CardsListScreen.tsx          # Lista de cartões
│   ├── AddCardScreen.tsx            # Adicionar cartão
│   └── CardDetailsScreen.tsx        # Detalhes do cartão
├── components/
│   ├── CardItem.tsx                 # Item da lista
│   ├── CardForm.tsx                 # Formulário
│   └── CardInputMask.tsx            # Input com máscara
├── services/
│   ├── pagarme.service.ts           # Tokenização
│   └── cards.service.ts             # API Backend
├── types/
│   └── card.types.ts                # TypeScript types
└── utils/
    ├── cardValidation.ts            # Validações
    └── cardBrand.ts                 # Detectar bandeira
```

---

## 💻 Código de Exemplo Completo

### 1. Types (TypeScript)

```typescript
// src/types/card.types.ts

export interface Card {
  id: number;
  lastFourDigits: string;
  brand: string;
  holderName: string;
  expiration: string;
  isDefault: boolean;
  isActive: boolean;
  isExpired: boolean;
  maskedNumber: string;
  createdAt: string;
  lastUsedAt: string | null;
}

export interface AddCardRequest {
  cardToken: string;
  setAsDefault: boolean;
}

export interface CardFormData {
  number: string;
  holderName: string;
  expMonth: string;
  expYear: string;
  cvv: string;
}
```

---

### 2. Serviço Pagar.me

```typescript
// src/services/pagarme.service.ts

const PAGARME_PUBLIC_KEY = 'pk_test_xxxxxxxxxxxxxxxx';
const PAGARME_API_URL = 'https://api.pagar.me/core/v5';

export const tokenizeCard = async (cardData: CardFormData): Promise<string> => {
  try {
    const response = await fetch(
      `${PAGARME_API_URL}/tokens?appId=${PAGARME_PUBLIC_KEY}`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          type: 'card',
          card: {
            number: cardData.number.replace(/\s/g, ''),
            holder_name: cardData.holderName.toUpperCase(),
            exp_month: parseInt(cardData.expMonth),
            exp_year: parseInt(cardData.expYear),
            cvv: cardData.cvv,
          },
        }),
      }
    );

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || 'Erro ao tokenizar cartão');
    }

    const data = await response.json();
    return data.id; // tok_xxxxx
  } catch (error) {
    console.error('Erro na tokenização:', error);
    throw error;
  }
};
```

---

### 3. Serviço Backend API

```typescript
// src/services/cards.service.ts

import api from './api'; // Seu axios/fetch configurado
import { Card, AddCardRequest } from '../types/card.types';

export const cardsService = {
  /**
   * Lista todos os cartões do usuário
   */
  async listCards(): Promise<Card[]> {
    const response = await api.get<Card[]>('/customer-cards');
    return response.data;
  },

  /**
   * Adiciona um novo cartão
   */
  async addCard(request: AddCardRequest): Promise<Card> {
    const response = await api.post<Card>('/customer-cards', request);
    return response.data;
  },

  /**
   * Busca cartão padrão
   */
  async getDefaultCard(): Promise<Card> {
    const response = await api.get<Card>('/customer-cards/default');
    return response.data;
  },

  /**
   * Define cartão como padrão
   */
  async setDefaultCard(cardId: number): Promise<Card> {
    const response = await api.put<Card>(
      `/customer-cards/${cardId}/set-default`
    );
    return response.data;
  },

  /**
   * Remove cartão
   */
  async deleteCard(cardId: number): Promise<void> {
    await api.delete(`/customer-cards/${cardId}`);
  },

  /**
   * Verifica se tem cartões
   */
  async hasCards(): Promise<boolean> {
    const response = await api.get<{ hasCards: boolean }>(
      '/customer-cards/has-cards'
    );
    return response.data.hasCards;
  },
};
```

---

### 4. Validações

```typescript
// src/utils/cardValidation.ts

/**
 * Valida número do cartão usando algoritmo de Luhn
 */
export const validateCardNumber = (number: string): boolean => {
  const cleaned = number.replace(/\s/g, '');
  
  if (!/^\d+$/.test(cleaned) || cleaned.length < 13 || cleaned.length > 19) {
    return false;
  }

  let sum = 0;
  let isEven = false;

  for (let i = cleaned.length - 1; i >= 0; i--) {
    let digit = parseInt(cleaned[i]);

    if (isEven) {
      digit *= 2;
      if (digit > 9) {
        digit -= 9;
      }
    }

    sum += digit;
    isEven = !isEven;
  }

  return sum % 10 === 0;
};

/**
 * Valida CVV
 */
export const validateCVV = (cvv: string, brand: string): boolean => {
  const length = brand === 'AMEX' ? 4 : 3;
  return /^\d+$/.test(cvv) && cvv.length === length;
};

/**
 * Valida data de expiração
 */
export const validateExpiration = (month: string, year: string): boolean => {
  const monthNum = parseInt(month);
  const yearNum = parseInt(year);

  if (monthNum < 1 || monthNum > 12) return false;
  if (yearNum < 2026) return false;

  const now = new Date();
  const currentYear = now.getFullYear();
  const currentMonth = now.getMonth() + 1;

  if (yearNum < currentYear) return false;
  if (yearNum === currentYear && monthNum < currentMonth) return false;

  return true;
};
```

---

### 5. Detectar Bandeira

```typescript
// src/utils/cardBrand.ts

export type CardBrand = 'VISA' | 'MASTERCARD' | 'AMEX' | 'ELO' | 'HIPERCARD' | 'DINERS' | 'DISCOVER' | 'JCB' | 'OTHER';

export const detectCardBrand = (number: string): CardBrand => {
  const cleaned = number.replace(/\s/g, '');

  // Visa: começa com 4
  if (/^4/.test(cleaned)) return 'VISA';

  // Mastercard: 51-55 ou 2221-2720
  if (/^5[1-5]/.test(cleaned) || /^2(2[2-9][0-9]|[3-6][0-9]{2}|7[0-1][0-9]|720)/.test(cleaned)) {
    return 'MASTERCARD';
  }

  // Amex: 34 ou 37
  if (/^3[47]/.test(cleaned)) return 'AMEX';

  // Elo
  if (/^(4011|4312|4389|4514|4576|5041|5066|5067|6277|6362|6363|6504|6505|6516)/.test(cleaned)) {
    return 'ELO';
  }

  // Hipercard
  if (/^(606282|637095|637568|637599|637609|637612)/.test(cleaned)) {
    return 'HIPERCARD';
  }

  // Diners: 36, 38, 300-305
  if (/^3(0[0-5]|[68])/.test(cleaned)) return 'DINERS';

  // Discover: 6011, 622126-622925, 644-649, 65
  if (/^(6011|65|64[4-9]|622)/.test(cleaned)) return 'DISCOVER';

  // JCB: 3528-3589
  if (/^35(2[89]|[3-8][0-9])/.test(cleaned)) return 'JCB';

  return 'OTHER';
};

export const getCardBrandIcon = (brand: CardBrand): string => {
  const icons: Record<CardBrand, string> = {
    VISA: '💳',
    MASTERCARD: '💳',
    AMEX: '💳',
    ELO: '💳',
    HIPERCARD: '💳',
    DINERS: '💳',
    DISCOVER: '💳',
    JCB: '💳',
    OTHER: '💳',
  };
  return icons[brand];
};
```

---

### 6. Tela: Adicionar Cartão

```typescript
// src/screens/AddCardScreen.tsx

import React, { useState } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  Alert,
  ActivityIndicator,
  StyleSheet,
} from 'react-native';
import { tokenizeCard } from '../services/pagarme.service';
import { cardsService } from '../services/cards.service';
import {
  validateCardNumber,
  validateCVV,
  validateExpiration,
} from '../utils/cardValidation';
import { detectCardBrand } from '../utils/cardBrand';

export const AddCardScreen = ({ navigation }) => {
  const [loading, setLoading] = useState(false);
  const [cardNumber, setCardNumber] = useState('');
  const [holderName, setHolderName] = useState('');
  const [expMonth, setExpMonth] = useState('');
  const [expYear, setExpYear] = useState('');
  const [cvv, setCvv] = useState('');
  const [setAsDefault, setSetAsDefault] = useState(false);

  const formatCardNumber = (text: string) => {
    const cleaned = text.replace(/\s/g, '');
    const chunks = cleaned.match(/.{1,4}/g) || [];
    return chunks.join(' ');
  };

  const handleAddCard = async () => {
    // Validações
    if (!validateCardNumber(cardNumber)) {
      Alert.alert('Erro', 'Número do cartão inválido');
      return;
    }

    if (!holderName.trim()) {
      Alert.alert('Erro', 'Nome do titular é obrigatório');
      return;
    }

    if (!validateExpiration(expMonth, expYear)) {
      Alert.alert('Erro', 'Data de expiração inválida');
      return;
    }

    const brand = detectCardBrand(cardNumber);
    if (!validateCVV(cvv, brand)) {
      Alert.alert('Erro', 'CVV inválido');
      return;
    }

    try {
      setLoading(true);

      // 1. Tokenizar no Pagar.me
      const token = await tokenizeCard({
        number: cardNumber,
        holderName,
        expMonth,
        expYear,
        cvv,
      });

      // 2. Enviar para backend
      const card = await cardsService.addCard({
        cardToken: token,
        setAsDefault,
      });

      Alert.alert(
        'Sucesso',
        `Cartão ${card.maskedNumber} adicionado com sucesso!`
      );

      navigation.goBack();
    } catch (error: any) {
      console.error('Erro ao adicionar cartão:', error);
      Alert.alert(
        'Erro',
        error.message || 'Não foi possível adicionar o cartão'
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Adicionar Cartão</Text>

      {/* Número do Cartão */}
      <TextInput
        style={styles.input}
        placeholder="Número do cartão"
        value={cardNumber}
        onChangeText={(text) => setCardNumber(formatCardNumber(text))}
        keyboardType="number-pad"
        maxLength={19} // 16 dígitos + 3 espaços
      />

      {/* Nome do Titular */}
      <TextInput
        style={styles.input}
        placeholder="Nome no cartão"
        value={holderName}
        onChangeText={setHolderName}
        autoCapitalize="characters"
      />

      {/* Validade */}
      <View style={styles.row}>
        <TextInput
          style={[styles.input, styles.smallInput]}
          placeholder="Mês"
          value={expMonth}
          onChangeText={setExpMonth}
          keyboardType="number-pad"
          maxLength={2}
        />
        <TextInput
          style={[styles.input, styles.smallInput]}
          placeholder="Ano"
          value={expYear}
          onChangeText={setExpYear}
          keyboardType="number-pad"
          maxLength={4}
        />
      </View>

      {/* CVV */}
      <TextInput
        style={styles.input}
        placeholder="CVV"
        value={cvv}
        onChangeText={setCvv}
        keyboardType="number-pad"
        maxLength={4}
        secureTextEntry
      />

      {/* Cartão Padrão */}
      <TouchableOpacity
        style={styles.checkbox}
        onPress={() => setSetAsDefault(!setAsDefault)}
      >
        <View style={[styles.checkboxBox, setAsDefault && styles.checked]} />
        <Text style={styles.checkboxLabel}>Definir como padrão</Text>
      </TouchableOpacity>

      {/* Botão */}
      <TouchableOpacity
        style={[styles.button, loading && styles.buttonDisabled]}
        onPress={handleAddCard}
        disabled={loading}
      >
        {loading ? (
          <ActivityIndicator color="#fff" />
        ) : (
          <Text style={styles.buttonText}>Adicionar Cartão</Text>
        )}
      </TouchableOpacity>

      {/* Aviso Segurança */}
      <Text style={styles.securityNote}>
        🔒 Seus dados são criptografados e protegidos pelo Pagar.me
      </Text>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 20,
    backgroundColor: '#f5f5f5',
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    marginBottom: 30,
    color: '#333',
  },
  input: {
    backgroundColor: '#fff',
    padding: 15,
    borderRadius: 8,
    marginBottom: 15,
    fontSize: 16,
    borderWidth: 1,
    borderColor: '#ddd',
  },
  row: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  smallInput: {
    flex: 1,
    marginRight: 10,
  },
  checkbox: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 20,
  },
  checkboxBox: {
    width: 24,
    height: 24,
    borderWidth: 2,
    borderColor: '#007AFF',
    borderRadius: 4,
    marginRight: 10,
  },
  checked: {
    backgroundColor: '#007AFF',
  },
  checkboxLabel: {
    fontSize: 16,
    color: '#333',
  },
  button: {
    backgroundColor: '#007AFF',
    padding: 16,
    borderRadius: 8,
    alignItems: 'center',
  },
  buttonDisabled: {
    opacity: 0.6,
  },
  buttonText: {
    color: '#fff',
    fontSize: 18,
    fontWeight: 'bold',
  },
  securityNote: {
    marginTop: 20,
    textAlign: 'center',
    color: '#666',
    fontSize: 12,
  },
});
```

---

### 7. Tela: Lista de Cartões

```typescript
// src/screens/CardsListScreen.tsx

import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  FlatList,
  TouchableOpacity,
  Alert,
  StyleSheet,
  RefreshControl,
} from 'react-native';
import { cardsService } from '../services/cards.service';
import { Card } from '../types/card.types';

export const CardsListScreen = ({ navigation }) => {
  const [cards, setCards] = useState<Card[]>([]);
  const [loading, setLoading] = useState(false);
  const [refreshing, setRefreshing] = useState(false);

  useEffect(() => {
    loadCards();
  }, []);

  const loadCards = async () => {
    try {
      setLoading(true);
      const data = await cardsService.listCards();
      setCards(data);
    } catch (error) {
      Alert.alert('Erro', 'Não foi possível carregar os cartões');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  const handleSetDefault = async (cardId: number) => {
    try {
      await cardsService.setDefaultCard(cardId);
      loadCards(); // Recarregar lista
      Alert.alert('Sucesso', 'Cartão padrão atualizado');
    } catch (error) {
      Alert.alert('Erro', 'Não foi possível definir cartão como padrão');
    }
  };

  const handleDelete = (card: Card) => {
    Alert.alert(
      'Remover Cartão',
      `Deseja remover o cartão ${card.maskedNumber}?`,
      [
        { text: 'Cancelar', style: 'cancel' },
        {
          text: 'Remover',
          style: 'destructive',
          onPress: async () => {
            try {
              await cardsService.deleteCard(card.id);
              loadCards();
              Alert.alert('Sucesso', 'Cartão removido');
            } catch (error) {
              Alert.alert('Erro', 'Não foi possível remover o cartão');
            }
          },
        },
      ]
    );
  };

  const renderCard = ({ item }: { item: Card }) => (
    <View style={styles.cardItem}>
      <View style={styles.cardInfo}>
        <Text style={styles.cardNumber}>{item.maskedNumber}</Text>
        <Text style={styles.cardExpiry}>Validade: {item.expiration}</Text>
        <Text style={styles.cardHolder}>{item.holderName}</Text>
        {item.isDefault && (
          <View style={styles.defaultBadge}>
            <Text style={styles.defaultText}>Padrão</Text>
          </View>
        )}
        {item.isExpired && (
          <View style={styles.expiredBadge}>
            <Text style={styles.expiredText}>Expirado</Text>
          </View>
        )}
      </View>

      <View style={styles.cardActions}>
        {!item.isDefault && (
          <TouchableOpacity
            style={styles.actionButton}
            onPress={() => handleSetDefault(item.id)}
          >
            <Text style={styles.actionText}>Tornar Padrão</Text>
          </TouchableOpacity>
        )}
        <TouchableOpacity
          style={[styles.actionButton, styles.deleteButton]}
          onPress={() => handleDelete(item)}
        >
          <Text style={[styles.actionText, styles.deleteText]}>Remover</Text>
        </TouchableOpacity>
      </View>
    </View>
  );

  return (
    <View style={styles.container}>
      <FlatList
        data={cards}
        renderItem={renderCard}
        keyExtractor={(item) => item.id.toString()}
        refreshControl={
          <RefreshControl refreshing={refreshing} onRefresh={loadCards} />
        }
        ListEmptyComponent={
          <View style={styles.empty}>
            <Text style={styles.emptyText}>Nenhum cartão cadastrado</Text>
          </View>
        }
      />

      <TouchableOpacity
        style={styles.addButton}
        onPress={() => navigation.navigate('AddCard')}
      >
        <Text style={styles.addButtonText}>+ Adicionar Cartão</Text>
      </TouchableOpacity>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  cardItem: {
    backgroundColor: '#fff',
    margin: 10,
    padding: 15,
    borderRadius: 12,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  cardInfo: {
    marginBottom: 10,
  },
  cardNumber: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 5,
  },
  cardExpiry: {
    fontSize: 14,
    color: '#666',
    marginBottom: 5,
  },
  cardHolder: {
    fontSize: 14,
    color: '#666',
  },
  defaultBadge: {
    backgroundColor: '#4CAF50',
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 12,
    alignSelf: 'flex-start',
    marginTop: 8,
  },
  defaultText: {
    color: '#fff',
    fontSize: 12,
    fontWeight: 'bold',
  },
  expiredBadge: {
    backgroundColor: '#f44336',
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 12,
    alignSelf: 'flex-start',
    marginTop: 8,
  },
  expiredText: {
    color: '#fff',
    fontSize: 12,
    fontWeight: 'bold',
  },
  cardActions: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginTop: 10,
  },
  actionButton: {
    flex: 1,
    padding: 10,
    borderRadius: 6,
    backgroundColor: '#007AFF',
    marginHorizontal: 5,
  },
  deleteButton: {
    backgroundColor: '#f44336',
  },
  actionText: {
    color: '#fff',
    textAlign: 'center',
    fontWeight: '600',
  },
  deleteText: {
    color: '#fff',
  },
  empty: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingTop: 100,
  },
  emptyText: {
    fontSize: 16,
    color: '#999',
  },
  addButton: {
    backgroundColor: '#007AFF',
    margin: 20,
    padding: 16,
    borderRadius: 12,
    alignItems: 'center',
  },
  addButtonText: {
    color: '#fff',
    fontSize: 18,
    fontWeight: 'bold',
  },
});
```

---

## ⚠️ Validações e Segurança

### Checklist de Segurança

- [x] **NUNCA** armazenar número completo do cartão no app
- [x] **NUNCA** armazenar CVV
- [x] **NUNCA** logar dados sensíveis
- [x] Usar HTTPS em produção
- [x] Validar dados no frontend antes de tokenizar
- [x] Usar token temporário (validade curta)
- [x] Limpar campos sensíveis após uso
- [x] Mascarar número do cartão na UI

### Validações Obrigatórias

1. **Número do Cartão:** Algoritmo de Luhn
2. **CVV:** 3 ou 4 dígitos (Amex)
3. **Validade:** Maior que data atual
4. **Nome:** Não vazio
5. **Bandeira:** Detectada automaticamente

---

## 🚨 Tratamento de Erros

### Erros Comuns

| Erro | Causa | Solução |
|------|-------|---------|
| `Token inválido` | Token expirou ou inválido | Gerar novo token |
| `Cartão já cadastrado` | Card_id já existe | Informar usuário |
| `401 Unauthorized` | JWT expirado | Refresh token |
| `Network error` | Sem internet | Retry com exponential backoff |
| `Card declined` | Cartão recusado | Verificar dados |

### Exemplo de Tratamento

```typescript
try {
  await cardsService.addCard(request);
} catch (error: any) {
  if (error.response?.status === 401) {
    // Token expirado - fazer logout
    navigation.navigate('Login');
  } else if (error.response?.status === 400) {
    // Erro de validação
    Alert.alert('Erro', error.response.data.message);
  } else if (error.message.includes('Network')) {
    // Erro de rede
    Alert.alert('Sem conexão', 'Verifique sua internet');
  } else {
    // Erro genérico
    Alert.alert('Erro', 'Tente novamente mais tarde');
  }
}
```

---

## 🧪 Testes

### Cartões de Teste (Sandbox)

```typescript
// Usar estes cartões para testar
const TEST_CARDS = {
  approved: {
    number: '4242 4242 4242 4242',
    cvv: '123',
    expMonth: '12',
    expYear: '2030',
  },
  declined: {
    number: '4000 0000 0000 0002',
    cvv: '123',
    expMonth: '12',
    expYear: '2030',
  },
};
```

### Cenários de Teste

1. ✅ Adicionar cartão aprovado
2. ✅ Adicionar cartão recusado
3. ✅ Definir cartão como padrão
4. ✅ Remover cartão
5. ✅ Validar campos vazios
6. ✅ Validar número inválido
7. ✅ Validar CVV inválido
8. ✅ Validar data expirada
9. ✅ Listar cartões vazia
10. ✅ Erro de rede

---

## 📚 Recursos Adicionais

### Documentação Pagar.me
- [API Reference](https://docs.pagar.me/reference/api-overview)
- [Cartões de Teste](https://docs.pagar.me/docs/testando-sua-integracao)
- [Tokenização](https://docs.pagar.me/reference/criacao-de-token-usando-biblioteca-javascript)

### UI/UX Guidelines
- Mostrar bandeira do cartão automaticamente
- Máscara de entrada (formatação automática)
- Feedback visual de validação
- Loading states claros
- Mensagens de erro específicas

---

## 🎯 Próximos Passos

1. ✅ Implementar telas conforme exemplos
2. ✅ Testar com cartões sandbox
3. ✅ Integrar com fluxo de pagamento
4. ✅ Adicionar analytics (cards adicionados, removidos)
5. ✅ Configurar Sentry para erros
6. ✅ Testar em iOS e Android
7. ✅ Deploy e homologação

---

## 💬 Suporte

**Dúvidas Técnicas:**
- Backend: Verificar logs em `nohup.out`
- Pagar.me: Dashboard sandbox para ver transações
- Mobile: Usar React Native Debugger

**Contatos:**
- Backend Team: [seu-email]
- Pagar.me Support: suporte@pagar.me

---

**Última atualização:** 02/02/2026  
**Versão:** 1.0  
**Status:** ✅ Pronto para implementação
