# 🏦 API - Cadastro de Conta Bancária (Motoboy)

## 📋 Visão Geral

Esta documentação descreve o fluxo completo para o **motoboy cadastrar sua conta bancária** através do aplicativo mobile Zapi10, incluindo:

- ✅ Validação de duplicidade no Pagar.me
- ✅ Criação de recipient no Pagar.me
- ✅ Validação de dados KYC completos
- ✅ Feedback detalhado de erros

---

## 🔐 Autenticação

Todas as requisições requerem **Bearer Token** no header:

```http
Authorization: Bearer {token_jwt_do_motoboy}
```

O token é obtido no login e identifica o usuário logado (motoboy).

---

## 📡 Endpoint Principal

### **POST /api/motoboy/bank-data**

Cadastra ou atualiza a conta bancária do motoboy logado.

**URL Completa:**
```
POST https://api.zapi10.com/api/motoboy/bank-data
```

---

## 📥 Request Body

### Exemplo Completo (JSON)

```json
{
  "bankCode": "260",
  "bankName": "Nubank",
  "agency": "0001",
  "agencyDigit": "",
  "accountNumber": "12345678-9",
  "accountType": "CHECKING",
  "accountHolderName": "João da Silva",
  "accountHolderDocument": "12345678901",
  
  // ============ DADOS KYC OBRIGATÓRIOS ============
  "email": "joao.silva@email.com",
  "motherName": "Maria da Silva",
  "birthdate": "15/03/1990",
  "monthlyIncome": "300000",
  "professionalOccupation": "Motoboy",
  
  // ============ TELEFONE ============
  "phoneDdd": "11",
  "phoneNumber": "987654321",
  
  // ============ ENDEREÇO COMPLETO ============
  "addressStreet": "Rua das Flores",
  "addressNumber": "123",
  "addressComplement": "Apto 45",
  "addressNeighborhood": "Centro",
  "addressCity": "São Paulo",
  "addressState": "SP",
  "addressZipCode": "01310100",
  "addressReferencePoint": "Próximo ao mercado"
}
```

---

## 📝 Campos do Request

### 🏦 Dados Bancários (Obrigatórios)

| Campo | Tipo | Validação | Descrição | Exemplo |
|-------|------|-----------|-----------|---------|
| `bankCode` | String(3) | **Obrigatório**<br>Código válido | Código COMPE do banco | `"260"` (Nubank) |
| `bankName` | String(100) | **Obrigatório** | Nome do banco | `"Nubank"` |
| `agency` | String(10) | **Obrigatório**<br>3-10 dígitos<br>Apenas números | Agência **sem dígito** | `"0001"` |
| `agencyDigit` | String(2) | Opcional | Dígito da agência | `""` ou `"5"` |
| `accountNumber` | String(20) | **Obrigatório**<br>Formato: `12345-6` | Conta **com dígito** | `"12345678-9"` |
| `accountType` | Enum | **Obrigatório** | Tipo de conta | `"CHECKING"` ou `"SAVINGS"` |
| `accountHolderName` | String(200) | Opcional | Nome do titular | `"João da Silva"` |
| `accountHolderDocument` | String(14) | Opcional | CPF do titular (apenas números) | `"12345678901"` |

**Tipos de Conta:**
- `CHECKING` = Conta Corrente
- `SAVINGS` = Conta Poupança

---

### 👤 Dados KYC - Obrigatórios Pagar.me

| Campo | Tipo | Formato | Descrição | Exemplo |
|-------|------|---------|-----------|---------|
| `email` | String(200) | **Obrigatório**<br>Email válido | E-mail do motoboy | `"joao@email.com"` |
| `motherName` | String(200) | **Obrigatório** | Nome completo da mãe | `"Maria da Silva"` |
| `birthdate` | String(10) | **Obrigatório**<br>`DD/MM/YYYY` | Data de nascimento | `"15/03/1990"` |
| `monthlyIncome` | String(20) | **Obrigatório**<br>Valor em centavos | Renda mensal | `"300000"` (R$ 3.000,00) |
| `professionalOccupation` | String(100) | **Obrigatório** | Profissão | `"Motoboy"` |

---

### 📞 Telefone

| Campo | Tipo | Formato | Descrição | Exemplo |
|-------|------|---------|-----------|---------|
| `phoneDdd` | String(2) | **Obrigatório**<br>2 dígitos | DDD | `"11"` |
| `phoneNumber` | String(9) | **Obrigatório**<br>8-9 dígitos | Número sem DDD | `"987654321"` |

---

### 🏠 Endereço Completo

| Campo | Tipo | Formato | Descrição | Exemplo |
|-------|------|---------|-----------|---------|
| `addressStreet` | String(200) | **Obrigatório** | Logradouro | `"Rua das Flores"` |
| `addressNumber` | String(10) | **Obrigatório** | Número | `"123"` |
| `addressComplement` | String(100) | Opcional | Complemento | `"Apto 45"` |
| `addressNeighborhood` | String(100) | **Obrigatório** | Bairro | `"Centro"` |
| `addressCity` | String(100) | **Obrigatório** | Cidade | `"São Paulo"` |
| `addressState` | String(2) | **Obrigatório**<br>UF | Estado | `"SP"` |
| `addressZipCode` | String(8) | **Obrigatório**<br>8 dígitos | CEP (apenas números) | `"01310100"` |
| `addressReferencePoint` | String(200) | Opcional | Ponto de referência | `"Próximo ao mercado"` |

---

## ✅ Response - Sucesso

### Status: `200 OK` (Atualização) ou `201 Created` (Criação)

```json
{
  "id": 123,
  "userId": 456,
  "bankCode": "260",
  "bankName": "Nubank",
  "agency": "0001",
  "accountNumber": "12345678-9",
  "accountType": "CHECKING",
  "status": "PENDING_VALIDATION",
  "pagarmeRecipientId": "rp_abc123xyz456",
  "createdAt": "2025-12-10T15:30:00",
  "updatedAt": "2025-12-10T15:30:00"
}
```

### Campos do Response

| Campo | Descrição |
|-------|-----------|
| `id` | ID interno da conta bancária |
| `userId` | ID do motoboy |
| `pagarmeRecipientId` | **ID do recipient criado no Pagar.me** |
| `status` | Status da validação:<br>• `PENDING_VALIDATION` - Aguardando validação<br>• `ACTIVE` - Ativa e validada<br>• `BLOCKED` - Bloqueada<br>• `CANCELLED` - Cancelada |

---

## ❌ Erros Possíveis

### 1. **Recipient Duplicado no Pagar.me**

**Status:** `409 Conflict`

```json
{
  "error": "DUPLICATE_RECIPIENT",
  "message": "Já existe um recipient cadastrado no Pagar.me com este CPF e conta bancária",
  "details": {
    "existingRecipientId": "rp_xyz789abc123",
    "cpf": "123.456.789-01",
    "bankAccount": "0001/12345678-9"
  }
}
```

**Solução para o Mobile:**
- Exibir mensagem: *"Conta bancária já cadastrada. Entre em contato com o suporte."*
- Não permitir nova tentativa com os mesmos dados

---

### 2. **Dados KYC Incompletos**

**Status:** `400 Bad Request`

```json
{
  "error": "VALIDATION_ERROR",
  "message": "Dados obrigatórios não preenchidos",
  "fields": [
    {
      "field": "email",
      "message": "E-mail é obrigatório"
    },
    {
      "field": "motherName",
      "message": "Nome da mãe é obrigatório"
    },
    {
      "field": "birthdate",
      "message": "Data de nascimento é obrigatória"
    }
  ]
}
```

**Solução para o Mobile:**
- Destacar campos com erro em vermelho
- Exibir mensagem de validação abaixo de cada campo

---

### 3. **Banco Inválido**

**Status:** `400 Bad Request`

```json
{
  "error": "INVALID_BANK_CODE",
  "message": "Código do banco inválido ou não cadastrado",
  "details": {
    "bankCode": "999",
    "validBanks": ["001", "033", "104", "237", "260", "341", "422"]
  }
}
```

---

### 4. **Formato de Conta Inválido**

**Status:** `400 Bad Request`

```json
{
  "error": "INVALID_ACCOUNT_FORMAT",
  "message": "Número da conta deve estar no formato 12345-6 (com hífen)",
  "providedValue": "123456789"
}
```

---

### 5. **Erro na API Pagar.me**

**Status:** `502 Bad Gateway`

```json
{
  "error": "PAGARME_API_ERROR",
  "message": "Erro ao criar recipient no Pagar.me",
  "details": {
    "pagarmeError": "Invalid document number",
    "httpStatus": 422
  }
}
```

**Solução para o Mobile:**
- Exibir: *"Erro ao validar dados bancários. Verifique CPF e outros dados."*
- Permitir nova tentativa após correção

---

### 6. **Usuário Não Autenticado**

**Status:** `401 Unauthorized`

```json
{
  "error": "UNAUTHORIZED",
  "message": "Token inválido ou expirado"
}
```

---

### 7. **Usuário Não é Motoboy**

**Status:** `403 Forbidden`

```json
{
  "error": "FORBIDDEN",
  "message": "Apenas motoboys podem cadastrar conta bancária"
}
```

---

## 🔄 Fluxo Backend (Lógica de Validação)

### Sequência de Validações

```mermaid
sequenceDiagram
    Mobile->>Backend: POST /api/motoboy/bank-data
    Backend->>Backend: Validar JWT (usuário logado)
    Backend->>Backend: Verificar role = COURIER
    Backend->>Backend: Validar campos obrigatórios
    Backend->>Backend: Validar formato conta (12345-6)
    Backend->>Backend: Validar código do banco
    Backend->>BankAccount: Buscar conta existente do user
    
    alt Conta já existe
        BankAccount-->>Backend: BankAccount existente
        Backend->>Pagar.me: GET /recipients (buscar por CPF)
        Pagar.me-->>Backend: Lista de recipients
        Backend->>Backend: Verificar duplicidade
        
        alt Recipient duplicado
            Backend-->>Mobile: 409 Conflict (duplicado)
        else Sem duplicidade
            Backend->>Pagar.me: PUT /recipients/{id} (atualizar)
            Pagar.me-->>Backend: Recipient atualizado
            Backend->>BankAccount: Atualizar registro
            Backend-->>Mobile: 200 OK (atualizado)
        end
    else Conta não existe
        Backend->>Pagar.me: GET /recipients (buscar por CPF)
        Pagar.me-->>Backend: Lista de recipients
        Backend->>Backend: Verificar duplicidade
        
        alt Recipient duplicado
            Backend-->>Mobile: 409 Conflict (duplicado)
        else Sem duplicidade
            Backend->>Pagar.me: POST /recipients (criar)
            Pagar.me-->>Backend: Recipient criado
            Backend->>BankAccount: Criar registro
            Backend-->>Mobile: 201 Created (criado)
        end
    end
```

---

## 🔍 Validação de Duplicidade Pagar.me

### Como Funciona

O Pagar.me **permite criar múltiplos recipients com mesmo CPF** (bug/feature da API).

**Solução Backend:**

1. **Antes de criar recipient**, fazer busca:
   ```bash
   GET https://api.pagar.me/core/v5/recipients
   Authorization: Basic {base64(sk_test_...:)}
   ```

2. **Comparar retorno** com os dados do request:
   - CPF (`document`)
   - Banco (`bank_account.bank_code`)
   - Agência (`bank_account.branch_number`)
   - Conta (`bank_account.account_number`)

3. **Se encontrar match exato** → Retornar `409 Conflict`

4. **Se não encontrar** → Criar novo recipient

---

### Request para Buscar Recipients (Backend)

```bash
curl -X GET 'https://api.pagar.me/core/v5/recipients' \
  -H 'Authorization: Basic c2tfdGVzdF83NTIzMjBhYTMwODI0OGU1OGY5MjZmM2NhMjEwZmMxMTo=' \
  -H 'Content-Type: application/json'
```

**Response Exemplo:**

```json
{
  "data": [
    {
      "id": "rp_abc123",
      "name": "João da Silva",
      "email": "joao@email.com",
      "document": "12345678901",
      "type": "individual",
      "default_bank_account": {
        "id": "ba_xyz789",
        "bank_code": "260",
        "branch_number": "0001",
        "branch_check_digit": "",
        "account_number": "12345678",
        "account_check_digit": "9",
        "type": "checking"
      },
      "status": "active"
    }
  ],
  "paging": {
    "total": 1,
    "page": 1,
    "pages": 1
  }
}
```

---

## 🎨 UI/UX - Sugestões Mobile

### Tela de Cadastro

```
┌─────────────────────────────────────┐
│  ← Cadastro de Conta Bancária       │
├─────────────────────────────────────┤
│                                     │
│  📋 DADOS BANCÁRIOS                 │
│  ┌─────────────────────────────┐   │
│  │ Banco: Nubank          [v]  │   │
│  └─────────────────────────────┘   │
│  ┌─────────────────────────────┐   │
│  │ Agência: 0001               │   │
│  └─────────────────────────────┘   │
│  ┌─────────────────────────────┐   │
│  │ Conta: 12345678-9           │   │
│  └─────────────────────────────┘   │
│  ┌─────────────────────────────┐   │
│  │ Tipo: [●] Corrente          │   │
│  │       [ ] Poupança          │   │
│  └─────────────────────────────┘   │
│                                     │
│  👤 DADOS PESSOAIS                  │
│  ┌─────────────────────────────┐   │
│  │ E-mail: joao@email.com      │   │
│  └─────────────────────────────┘   │
│  ┌─────────────────────────────┐   │
│  │ Nome da Mãe: Maria Silva    │   │
│  └─────────────────────────────┘   │
│  ┌─────────────────────────────┐   │
│  │ Data Nasc: 15/03/1990       │   │
│  └─────────────────────────────┘   │
│  ┌─────────────────────────────┐   │
│  │ Renda Mensal: R$ 3.000,00   │   │
│  └─────────────────────────────┘   │
│                                     │
│  📞 TELEFONE                        │
│  ┌────┐  ┌──────────────────┐      │
│  │ 11 │  │ 98765-4321       │      │
│  └────┘  └──────────────────┘      │
│  DDD      Número                   │
│                                     │
│  🏠 ENDEREÇO                        │
│  ┌─────────────────────────────┐   │
│  │ CEP: 01310-100         [🔍] │   │
│  └─────────────────────────────┘   │
│  ┌─────────────────────────────┐   │
│  │ Rua: Rua das Flores         │   │
│  └─────────────────────────────┘   │
│  ┌────┐  ┌──────────────────┐      │
│  │123 │  │ Apto 45          │      │
│  └────┘  └──────────────────┘      │
│  Número   Complemento              │
│                                     │
│  [    SALVAR CONTA BANCÁRIA    ]   │
│                                     │
└─────────────────────────────────────┘
```

---

### Validações em Tempo Real

- **CEP**: Buscar endereço automaticamente via ViaCEP
- **Banco**: Dropdown com bancos válidos
- **Conta**: Mask automático para formato `12345-6`
- **CPF**: Validar dígitos verificadores
- **Data**: DatePicker com validação de idade mínima (18 anos)
- **Renda**: Formatar moeda (R$ 0,00)

---

### Loading States

```
┌─────────────────────────────────────┐
│  Validando dados bancários...       │
│  ┌─────────────────────────────┐   │
│  │                             │   │
│  │      🔄 Processando...      │   │
│  │                             │   │
│  │  Verificando recipient no   │   │
│  │  Pagar.me...                │   │
│  │                             │   │
│  └─────────────────────────────┘   │
└─────────────────────────────────────┘
```

---

### Mensagens de Sucesso

```
┌─────────────────────────────────────┐
│  ✅ Conta Cadastrada!                │
│  ┌─────────────────────────────┐   │
│  │                             │   │
│  │  Sua conta bancária foi     │   │
│  │  cadastrada com sucesso!    │   │
│  │                             │   │
│  │  Status: Pendente Validação │   │
│  │                             │   │
│  │  Você receberá uma          │   │
│  │  notificação quando for     │   │
│  │  aprovada.                  │   │
│  │                             │   │
│  │  [      OK, ENTENDI     ]   │   │
│  │                             │   │
│  └─────────────────────────────┘   │
└─────────────────────────────────────┘
```

---

### Mensagens de Erro

```
┌─────────────────────────────────────┐
│  ❌ Conta Duplicada                  │
│  ┌─────────────────────────────┐   │
│  │                             │   │
│  │  Esta conta bancária já     │   │
│  │  está cadastrada no sistema.│   │
│  │                             │   │
│  │  Se você acredita que isso  │   │
│  │  é um erro, entre em        │   │
│  │  contato com o suporte:     │   │
│  │                             │   │
│  │  📞 (11) 9999-9999          │   │
│  │  ✉️  suporte@zapi10.com     │   │
│  │                             │   │
│  │  [   FALAR COM SUPORTE  ]   │   │
│  │                             │   │
│  └─────────────────────────────┘   │
└─────────────────────────────────────┘
```

---

## 🧪 Exemplos de Teste

### 1. Cadastro Completo (Sucesso)

```bash
curl -X POST 'https://api.zapi10.com/api/motoboy/bank-data' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzI1NiIs...' \
  -H 'Content-Type: application/json' \
  -d '{
    "bankCode": "260",
    "bankName": "Nubank",
    "agency": "0001",
    "accountNumber": "12345678-9",
    "accountType": "CHECKING",
    "email": "joao@email.com",
    "motherName": "Maria da Silva",
    "birthdate": "15/03/1990",
    "monthlyIncome": "300000",
    "professionalOccupation": "Motoboy",
    "phoneDdd": "11",
    "phoneNumber": "987654321",
    "addressStreet": "Rua das Flores",
    "addressNumber": "123",
    "addressNeighborhood": "Centro",
    "addressCity": "São Paulo",
    "addressState": "SP",
    "addressZipCode": "01310100"
  }'
```

---

### 2. Dados Incompletos (Erro)

```bash
curl -X POST 'https://api.zapi10.com/api/motoboy/bank-data' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzI1NiIs...' \
  -H 'Content-Type: application/json' \
  -d '{
    "bankCode": "260",
    "agency": "0001",
    "accountNumber": "12345678-9"
  }'
```

**Response:** `400 Bad Request` - Campos obrigatórios faltando

---

## 📚 Bancos Suportados (Principais)

| Código | Nome |
|--------|------|
| 001 | Banco do Brasil |
| 033 | Santander |
| 104 | Caixa Econômica Federal |
| 237 | Bradesco |
| 260 | Nubank |
| 341 | Itaú |
| 422 | Safra |
| 748 | Sicredi |
| 756 | Sicoob |

**Lista completa:** Usar enum `BrazilianBanks` no backend

---

## 🔐 Segurança

### Headers Obrigatórios

```http
Authorization: Bearer {jwt_token}
Content-Type: application/json
```

### Validações Backend

1. ✅ Token JWT válido
2. ✅ Usuário com role `COURIER`
3. ✅ Dados KYC completos
4. ✅ CPF válido (dígitos verificadores)
5. ✅ Recipient não duplicado no Pagar.me
6. ✅ Código de banco válido
7. ✅ Formato de conta correto

---

## 📱 Integração Mobile (React Native / Flutter)

### Exemplo React Native

```typescript
import axios from 'axios';

interface BankAccountData {
  bankCode: string;
  bankName: string;
  agency: string;
  accountNumber: string;
  accountType: 'CHECKING' | 'SAVINGS';
  email: string;
  motherName: string;
  birthdate: string;
  monthlyIncome: string;
  professionalOccupation: string;
  phoneDdd: string;
  phoneNumber: string;
  addressStreet: string;
  addressNumber: string;
  addressNeighborhood: string;
  addressCity: string;
  addressState: string;
  addressZipCode: string;
  addressComplement?: string;
  addressReferencePoint?: string;
}

async function cadastrarContaBancaria(data: BankAccountData) {
  try {
    const token = await AsyncStorage.getItem('jwt_token');
    
    const response = await axios.post(
      'https://api.zapi10.com/api/motoboy/bank-data',
      data,
      {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      }
    );
    
    if (response.status === 201) {
      Alert.alert('Sucesso', 'Conta bancária cadastrada!');
      navigation.navigate('Home');
    }
    
  } catch (error) {
    if (error.response?.status === 409) {
      Alert.alert(
        'Conta Duplicada',
        'Esta conta já está cadastrada. Entre em contato com o suporte.'
      );
    } else if (error.response?.status === 400) {
      const fields = error.response.data.fields;
      Alert.alert('Dados Inválidos', fields.map(f => f.message).join('\n'));
    } else {
      Alert.alert('Erro', 'Erro ao cadastrar conta. Tente novamente.');
    }
  }
}
```

---

## 🌐 Integração Frontend Web (React)

### Exemplo React + Axios

```typescript
import { useState } from 'react';
import { useAuth } from '@/hooks/useAuth';

export function BankAccountForm() {
  const { token } = useAuth();
  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState({
    bankCode: '',
    agency: '',
    accountNumber: '',
    // ... outros campos
  });

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    
    try {
      const response = await fetch('/api/motoboy/bank-data', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(formData)
      });
      
      if (response.status === 409) {
        alert('Conta duplicada! Entre em contato com o suporte.');
      } else if (response.ok) {
        alert('Conta cadastrada com sucesso!');
        router.push('/dashboard');
      }
      
    } catch (error) {
      console.error('Erro ao cadastrar:', error);
      alert('Erro ao cadastrar conta bancária');
    } finally {
      setLoading(false);
    }
  };
  
  return (
    <form onSubmit={handleSubmit}>
      {/* Campos do formulário */}
      <button type="submit" disabled={loading}>
        {loading ? 'Salvando...' : 'Salvar Conta'}
      </button>
    </form>
  );
}
```

---

## 🎯 Checklist de Implementação

### Backend (Spring Boot)

- [x] Criar endpoint `POST /api/motoboy/bank-data`
- [x] Implementar validação de role `COURIER`
- [x] Validar campos obrigatórios KYC
- [x] Implementar busca de recipients no Pagar.me (`GET /recipients`)
- [x] Implementar lógica de detecção de duplicidade
- [x] Criar recipient no Pagar.me se não duplicado
- [x] Salvar `pagarmeRecipientId` no BankAccount
- [x] Retornar erros padronizados (409, 400, 502)
- [x] Adicionar logs para debug

### Mobile (React Native / Flutter)

- [ ] Criar tela de cadastro de conta bancária
- [ ] Implementar formulário com validações
- [ ] Integrar busca de CEP (ViaCEP)
- [ ] Implementar dropdown de bancos
- [ ] Adicionar masks (CPF, telefone, CEP, conta)
- [ ] Implementar loading states
- [ ] Tratar erros 409 (duplicado)
- [ ] Tratar erros 400 (validação)
- [ ] Exibir mensagem de sucesso
- [ ] Navegação pós-cadastro

### Frontend Web (React)

- [ ] Criar página de cadastro
- [ ] Implementar formulário responsivo
- [ ] Validações client-side
- [ ] Integração com API backend
- [ ] Feedback visual de erros
- [ ] Loading states

---

## 📞 Suporte

**Problemas comuns:**

1. **"Recipient duplicado"** → Verificar se CPF/conta já cadastrados
2. **"Dados KYC inválidos"** → Validar formato de birthdate, email, etc
3. **"Erro Pagar.me"** → Verificar credenciais da API Key
4. **"Token expirado"** → Renovar JWT no login

**Contato Técnico:**
- Slack: `#dev-zapi10`
- Email: `dev@zapi10.com`

---

**Última atualização:** 10/12/2025  
**Versão da API:** v1.0  
**Ambiente:** Sandbox Pagar.me
