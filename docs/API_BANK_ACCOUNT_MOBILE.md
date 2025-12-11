# 📱 API de Cadastro de Conta Bancária - Mobile (Zapi10)

## 🎯 Overview

Este documento descreve o endpoint para cadastro de conta bancária de motoboys/couriers no app Zapi10 Mobile.

Quando o motoboy cadastra sua conta bancária no app, o backend:
1. Salva os dados no PostgreSQL
2. Cria automaticamente um **recipient (subconta)** no Pagar.me
3. Habilita o motoboy para receber pagamentos via PIX com split automático

---

## 🔐 Autenticação

```http
Authorization: Bearer {access_token}
```

O token deve ser do usuário logado (motoboy).

---

## 📍 Endpoint

```http
POST /api/bank-accounts
Content-Type: application/json
Authorization: Bearer {token}
```

---

## 📥 Request Body

### Campos Obrigatórios

| Campo | Tipo | Formato | Descrição | Exemplo |
|-------|------|---------|-----------|---------|
| **DADOS BANCÁRIOS** |
| `bankCode` | string | 3 dígitos | Código do banco | `"341"` |
| `bankName` | string | - | Nome do banco | `"Itaú Unibanco"` |
| `agency` | string | Apenas números | Agência sem dígito | `"1234"` |
| `agencyDigit` | string | 1 caractere ou null | Dígito da agência (opcional) | `"X"` ou `null` |
| `accountNumber` | string | Apenas números | Conta sem dígito | `"12345678"` |
| `accountDigit` | string | 1-2 caracteres | Dígito verificador | `"9"` |
| `accountType` | enum | CHECKING/SAVINGS | Tipo de conta | `"CHECKING"` |
| `accountHolderName` | string | - | Nome do titular | `"João da Silva"` |
| `accountHolderDocument` | string | 11 dígitos | CPF sem pontuação | `"12345678901"` |
| **DADOS PESSOAIS** |
| `email` | string | - | Email do usuário | `"joao@email.com"` |
| `motherName` | string | - | Nome da mãe | `"Maria da Silva"` |
| `birthdate` | string | DD/MM/YYYY | Data de nascimento | `"15/05/1990"` |
| `monthlyIncome` | string | - | Renda mensal | `"3000"` |
| `professionalOccupation` | string | - | Ocupação | `"Motoboy"` |
| **CONTATO** |
| `phoneDdd` | string | 2 dígitos | DDD | `"85"` |
| `phoneNumber` | string | 8-9 dígitos | Telefone sem DDD | `"987654321"` |
| **ENDEREÇO** |
| `addressStreet` | string | - | Nome da rua | `"Rua Alberto Carvalho"` |
| `addressNumber` | string | - | Número | `"111"` |
| `addressComplement` | string | opcional | Complemento | `"Apto 345"` |
| `addressNeighborhood` | string | - | Bairro | `"Centro"` |
| `addressCity` | string | - | Cidade | `"Fortaleza"` |
| `addressState` | string | UF (2 letras) | Estado | `"CE"` |
| `addressZipCode` | string | 8 dígitos | CEP sem pontuação | `"60000000"` |
| `addressReferencePoint` | string | - | Ponto de referência | `"Próximo ao supermercado"` |

---

## 📤 Exemplo de Request

```json
{
  "bankCode": "341",
  "bankName": "Itaú Unibanco",
  "agency": "1234",
  "agencyDigit": "6",
  "accountNumber": "12345678",
  "accountDigit": "9",
  "accountType": "CHECKING",
  "accountHolderName": "João da Silva",
  "accountHolderDocument": "12345678901",
  
  "email": "joao@email.com",
  "motherName": "Maria da Silva",
  "birthdate": "15/05/1990",
  "monthlyIncome": "3000",
  "professionalOccupation": "Motoboy",
  
  "phoneDdd": "85",
  "phoneNumber": "987654321",
  
  "addressStreet": "Rua Alberto Carvalho",
  "addressNumber": "111",
  "addressComplement": "Apto 345",
  "addressNeighborhood": "Centro",
  "addressCity": "Fortaleza",
  "addressState": "CE",
  "addressZipCode": "60000000",
  "addressReferencePoint": "Próximo ao supermercado Atacadão"
}
```

---

## ✅ Response - Sucesso (201 Created)

```json
{
  "id": 123,
  "userId": "uuid-do-usuario",
  "bankCode": "341",
  "bankName": "Itaú Unibanco",
  "agency": "1234",
  "agencyDigit": "6",
  "accountNumber": "12345678",
  "accountDigit": "9",
  "accountType": "CHECKING",
  "status": "ACTIVE",
  "createdAt": "2025-12-10T10:30:00Z",
  "updatedAt": "2025-12-10T10:30:00Z"
}
```

### Status da Conta

| Status | Descrição |
|--------|-----------|
| `ACTIVE` | Conta ativa e apta para receber pagamentos |
| `PENDING_VALIDATION` | Aguardando validação |
| `BLOCKED` | Conta bloqueada (erro ao criar recipient) |

---

## ❌ Respostas de Erro

### 400 Bad Request - Validação

```json
{
  "error": "VALIDATION_ERROR",
  "message": "CPF do titular deve ter 11 dígitos",
  "field": "accountHolderDocument"
}
```

### 409 Conflict - Conta já existe

```json
{
  "error": "ACCOUNT_ALREADY_EXISTS",
  "message": "Usuário já possui conta bancária cadastrada. Use PUT para atualizar."
}
```

### 500 Internal Server Error - Erro no Pagar.me

```json
{
  "error": "RECIPIENT_CREATION_FAILED",
  "message": "Falha ao criar recipient no Pagar.me"
}
```

---

## 📋 Validações Client-Side (Recomendadas)

Antes de enviar para o backend, valide:

```javascript
// CPF (11 dígitos)
const cpfRegex = /^\d{11}$/;

// Email
const emailRegex = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$/;

// Banco (3 dígitos)
const bankCodeRegex = /^\d{3}$/;

// Agência (apenas números)
const agencyRegex = /^\d+$/;

// Conta (apenas números)
const accountRegex = /^\d+$/;

// Data de nascimento (DD/MM/YYYY)
const birthdateRegex = /^\d{2}\/\d{2}\/\d{4}$/;

// DDD (2 dígitos)
const dddRegex = /^\d{2}$/;

// Telefone (8 ou 9 dígitos)
const phoneRegex = /^\d{8,9}$/;

// CEP (8 dígitos)
const cepRegex = /^\d{8}$/;

// UF (2 letras maiúsculas)
const ufRegex = /^[A-Z]{2}$/;
```

---

## 🏦 Lista de Bancos (Top 20)

| Código | Nome |
|--------|------|
| 001 | Banco do Brasil |
| 033 | Santander |
| 104 | Caixa Econômica Federal |
| 237 | Bradesco |
| 341 | Itaú Unibanco |
| 745 | Citibank |
| 399 | HSBC |
| 422 | Safra |
| 389 | Banco Mercantil |
| 756 | Sicoob |
| 748 | Sicredi |
| 260 | Nubank |
| 290 | Pagseguro |
| 323 | Mercado Pago |
| 380 | PicPay |
| 102 | XP Investimentos |
| 077 | Banco Inter |
| 336 | C6 Bank |
| 197 | Stone |
| 403 | Cora |

---

## 🎨 UI/UX Recomendações

### Fluxo de Telas

1. **Tela 1 - Dados Bancários**
   - Banco (picker/dropdown)
   - Agência + dígito
   - Conta + dígito
   - Tipo de conta (Corrente/Poupança)
   - Nome do titular
   - CPF do titular

2. **Tela 2 - Dados Pessoais**
   - Nome da mãe
   - Data de nascimento (date picker)
   - Renda mensal (R$)
   - Ocupação profissional

3. **Tela 3 - Contato**
   - Telefone (DDD + número)

4. **Tela 4 - Endereço**
   - CEP (auto-completar rua/bairro/cidade/UF via ViaCEP)
   - Número
   - Complemento (opcional)

5. **Tela 5 - Revisão e Confirmação**
   - Mostrar todos os dados
   - Botão "Confirmar Cadastro"

### Máscaras de Input

```javascript
// CPF: 123.456.789-01
// CEP: 60000-000
// Telefone: (85) 98765-4321
// Data: 15/05/1990
// Agência: 1234-X (se tiver dígito)
// Conta: 12345678-9
```

### Loading States

```javascript
// Enquanto envia
<Button disabled loading>
  Cadastrando conta bancária...
</Button>

// Sucesso
<Alert type="success">
  Conta cadastrada! Você já pode receber pagamentos.
</Alert>

// Erro
<Alert type="error">
  Erro ao cadastrar. Verifique os dados e tente novamente.
</Alert>
```

---

## 🔄 Atualização de Conta

Para atualizar dados bancários:

```http
PUT /api/bank-accounts/{id}
```

Mesmo body, mesmas validações. O backend recriará o recipient no Pagar.me.

---

## 🧪 Teste no Sandbox

Use dados fictícios válidos:

```json
{
  "accountHolderDocument": "12345678901",
  "birthdate": "01/01/1990",
  "phoneDdd": "11",
  "phoneNumber": "987654321"
}
```

O Pagar.me sandbox aceita qualquer CPF válido (11 dígitos).

---

## 📞 Suporte

Em caso de dúvidas, contate o time backend.
