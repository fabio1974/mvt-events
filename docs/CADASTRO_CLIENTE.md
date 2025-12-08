# 👤 Como Cadastrar um Cliente

## ⚠️ Pré-requisito Obrigatório

Para criar um pagamento consolidado, o **cliente DEVE estar cadastrado** na tabela `users` antes.

## 📝 Opções de Cadastro

### **Opção 1: Via SQL (Desenvolvimento/Teste)**

```sql
-- Inserir cliente na tabela users
INSERT INTO users (
    id,
    created_at,
    updated_at,
    name,
    username,
    password,
    role,
    cpf,
    enabled
) VALUES (
    gen_random_uuid(),
    NOW(),
    NOW(),
    'João Silva',                           -- Nome do cliente
    'cliente@example.com',                  -- Email (usado como username)
    '$2a$10$DUMMY_PASSWORD',                -- Senha (pode ser dummy)
    'CLIENT',                               -- Role obrigatório
    '123.456.789-00',                       -- CPF válido
    true
);

-- Verificar se foi criado
SELECT id, name, username, role, cpf 
FROM users 
WHERE username = 'cliente@example.com';
```

### **Opção 2: Via Endpoint de Cadastro (API)**

**Endpoint:** `POST /api/users` (ou similar)

```json
{
  "name": "João Silva",
  "username": "cliente@example.com",
  "password": "senha123",
  "role": "CLIENT",
  "cpf": "123.456.789-00",
  "phone": "+55 11 98765-4321"
}
```

### **Opção 3: Via Frontend (Interface Admin)**

Se houver uma interface administrativa:
1. Acesse a área de **Cadastro de Usuários**
2. Preencha os dados:
   - **Nome**: João Silva
   - **Email**: cliente@example.com
   - **CPF**: 123.456.789-00
   - **Role**: CLIENT
   - **Senha**: (qualquer senha válida)
3. Clique em **Salvar**

## 🔍 Como Verificar se Cliente Existe

### **Via SQL:**
```sql
SELECT id, name, username, role, cpf 
FROM users 
WHERE username = 'cliente@example.com';
```

### **Via API:**
```bash
curl -X GET 'http://localhost:8080/api/users?email=cliente@example.com' \
  -H 'Authorization: Bearer SEU_TOKEN'
```

## ⚠️ Campos Obrigatórios do User

| Campo | Tipo | Obrigatório | Descrição |
|-------|------|-------------|-----------|
| `id` | UUID | ✅ Sim | Gerado automaticamente |
| `name` | String | ✅ Sim | Nome completo |
| `username` | String | ✅ Sim | **Email do cliente** (único) |
| `password` | String | ✅ Sim | Hash da senha |
| `role` | Enum | ✅ Sim | Deve ser `CLIENT` |
| `cpf` | String | ✅ Sim | CPF válido (formato: XXX.XXX.XXX-XX) |
| `enabled` | Boolean | ✅ Sim | Default: `true` |

## 🧪 Cliente de Teste (Para Desenvolvimento)

```sql
-- Cliente de teste já configurado
INSERT INTO users (
    id,
    created_at,
    updated_at,
    name,
    username,
    password,
    role,
    cpf,
    phone,
    enabled
) VALUES (
    gen_random_uuid(),
    NOW(),
    NOW(),
    'Cliente Teste',
    'teste@mvt.com',
    '$2a$10$PLACEHOLDER_HASH',
    'CLIENT',
    '111.111.111-11',
    '+55 11 99999-9999',
    true
) ON CONFLICT (username) DO NOTHING;
```

## 🎯 Fluxo Completo: Cadastro → Pagamento

### **1. Cadastrar Cliente**
```sql
INSERT INTO users (...) VALUES (...);
-- ✅ Cliente: teste@mvt.com cadastrado
```

### **2. Verificar Cadastro**
```sql
SELECT * FROM users WHERE username = 'teste@mvt.com';
-- ✅ Retorna o cliente
```

### **3. Criar Pagamento Consolidado**
```bash
curl -X POST 'http://localhost:8080/api/payment/create-invoice' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer TOKEN' \
  -d '{
    "deliveryIds": [1, 13],
    "clientEmail": "teste@mvt.com"
  }'
```

### **4. Resultado**
```json
{
  "paymentId": 123,
  "iuguInvoiceId": "MOCK_INV_...",
  "pixQrCode": "00020126360014BR.GOV.BCB.PIX...",
  "amount": 25.00,
  "status": "PENDING"
}
```

## ❌ Erros Comuns

### **Erro: "Cliente não encontrado"**
```json
{
  "error": "Bad Request",
  "message": "Cliente com email 'fulano@example.com' não encontrado. Por favor, cadastre o cliente primeiro antes de criar o pagamento."
}
```

**Solução:** Cadastre o cliente com o email `fulano@example.com` antes de criar o pagamento.

### **Erro: "CPF inválido"**
```json
{
  "violations": {
    "cpf": "CPF inválido"
  }
}
```

**Solução:** Use um CPF válido. Validação de dígito verificador é aplicada.

### **Erro: "Username já existe"**
```sql
ERROR: duplicate key value violates unique constraint "users_username_key"
```

**Solução:** O email já está cadastrado. Use outro email ou busque o User existente.

## 📚 Documentos Relacionados

- [TEST_CONSOLIDATED_PAYMENT.md](./TEST_CONSOLIDATED_PAYMENT.md) - Guia completo de teste de pagamentos
- [IUGU_MODES_GUIDE.md](./IUGU_MODES_GUIDE.md) - Modos de operação Iugu

## 💡 Dicas

1. **Development:** Use `*@mvt.com` para clientes de teste
2. **Production:** Valide CPF real e email real
3. **CPF único:** Cada cliente deve ter CPF único no banco
4. **Email único:** `username` é único na tabela
5. **Role CLIENT:** Necessário para associar a pagamentos
