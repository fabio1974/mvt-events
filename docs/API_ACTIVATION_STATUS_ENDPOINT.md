# API - Endpoint de Status de Ativação do Usuário

## Visão Geral

Endpoint que retorna o status de ativação do usuário em **tempo real**, detalhando exatamente o que está faltando para ele estar completamente habilitado no sistema.

**Vantagens:**
- ✅ Status em tempo real (não depende do JWT)
- ✅ Mensagens amigáveis prontas para exibir no mobile
- ✅ Lista exata do que está faltando
- ✅ Sugestões de melhorias opcionais

---

## Endpoint

```http
GET /api/users/me/activation-status
```

**Autenticação:** Bearer Token (JWT) obrigatório

---

## Resposta

### Estrutura do JSON

```typescript
{
  enabled: boolean;           // Se o usuário está completamente habilitado
  role: string;               // Role do usuário (COURIER, CUSTOMER, etc)
  missing: string[];          // Lista de itens obrigatórios faltando
  messages: {                 // Mensagens amigáveis para cada item faltante
    [key: string]: string;
  };
  suggested: string[];        // Itens opcionais/sugeridos
}
```

---

## Exemplos de Resposta

### ✅ COURIER Completamente Habilitado

```json
{
  "enabled": true,
  "role": "COURIER",
  "missing": [],
  "messages": {},
  "suggested": []
}
```

---

### ❌ COURIER NÃO Habilitado

```json
{
  "enabled": false,
  "role": "COURIER",
  "missing": ["vehicle", "bankAccount", "phone"],
  "messages": {
    "vehicle": "Cadastre um veículo",
    "bankAccount": "Cadastre sua conta bancária",
    "phone": "Preencha seu telefone nas informações pessoais"
  },
  "suggested": ["defaultAddress"]
}
```

---

### ✅ CUSTOMER Completamente Habilitado

```json
{
  "enabled": true,
  "role": "CUSTOMER",
  "missing": [],
  "messages": {},
  "suggested": []
}
```

---

### ❌ CUSTOMER NÃO Habilitado

```json
{
  "enabled": false,
  "role": "CUSTOMER",
  "missing": ["paymentMethod", "phone"],
  "messages": {
    "paymentMethod": "Cadastre um meio de pagamento",
    "phone": "Preencha seu telefone nas informações pessoais"
  },
  "suggested": ["defaultAddress"]
}
```

---

## Descrição dos Campos

### `enabled`
- **Tipo:** `boolean`
- **Descrição:** `true` se o usuário está completamente habilitado, `false` se falta algo obrigatório
- **Lógica:** `missing.length === 0`

### `role`
- **Tipo:** `string`
- **Valores possíveis:** `"COURIER"`, `"CUSTOMER"`, `"ORGANIZER"`, `"ADMIN"`, `"USER"`
- **Descrição:** Role do usuário logado

### `missing`
- **Tipo:** `string[]`
- **Valores possíveis:**
  - `"vehicle"` - Falta cadastrar veículo (COURIER)
  - `"bankAccount"` - Falta cadastrar conta bancária (COURIER)
  - `"paymentMethod"` - Falta cadastrar cartão de crédito (CUSTOMER)
  - `"phone"` - Falta preencher telefone (TODOS)
- **Descrição:** Lista de itens **obrigatórios** que estão faltando

### `messages`
- **Tipo:** `{ [key: string]: string }`
- **Descrição:** Mensagens amigáveis em português para cada item em `missing`
- **Uso:** Exibir direto no mobile para orientar o usuário

### `suggested`
- **Tipo:** `string[]`
- **Valores possíveis:**
  - `"defaultAddress"` - Sugerido cadastrar endereço padrão
- **Descrição:** Itens **opcionais** que melhoram a experiência mas não são obrigatórios

---

## Regras de Validação

### Para COURIER

O COURIER está habilitado quando possui **TODOS** os itens:

| Item | Campo Validado | Endpoint para Cadastro |
|------|---------------|------------------------|
| ✅ Veículo | Pelo menos 1 veículo cadastrado | `POST /api/vehicles` |
| ✅ Conta Bancária | Conta bancária vinculada | `POST /api/bank-accounts` |
| ✅ Telefone | Campo `phone` preenchido | `PUT /api/users/{id}` |

**Sugerido:**
- 💡 Endereço Padrão: Facilita criação de entregas

---

### Para CUSTOMER

O CUSTOMER está habilitado quando possui **TODOS** os itens:

| Item | Campo Validado | Endpoint para Cadastro |
|------|---------------|------------------------|
| ✅ Meio de Pagamento | Pelo menos 1 cartão ativo | `POST /api/customer-cards` |
| ✅ Telefone | Campo `phone` preenchido | `PUT /api/users/{id}` |

**Sugerido:**
- 💡 Endereço Padrão: Facilita criação de entregas

---

## Exemplo de Uso no Mobile

### TypeScript/React Native

```typescript
interface ActivationStatus {
  enabled: boolean;
  role: string;
  missing: string[];
  messages: { [key: string]: string };
  suggested: string[];
}

async function checkActivationStatus(): Promise<ActivationStatus> {
  const response = await fetch(
    'https://api.zapi10.com/api/users/me/activation-status',
    {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${authToken}`,
        'Content-Type': 'application/json'
      }
    }
  );
  
  return await response.json();
}

// Uso
const status = await checkActivationStatus();

if (!status.enabled) {
  // Exibir lista de pendências
  status.missing.forEach(item => {
    console.log(`⚠️ ${status.messages[item]}`);
  });
  
  // Exemplo: Redirecionar para tela de cadastro
  if (status.missing.includes('vehicle')) {
    navigation.navigate('VehicleRegistration');
  }
}
```

---

### Flutter/Dart

```dart
class ActivationStatus {
  final bool enabled;
  final String role;
  final List<String> missing;
  final Map<String, String> messages;
  final List<String> suggested;

  ActivationStatus({
    required this.enabled,
    required this.role,
    required this.missing,
    required this.messages,
    required this.suggested,
  });

  factory ActivationStatus.fromJson(Map<String, dynamic> json) {
    return ActivationStatus(
      enabled: json['enabled'],
      role: json['role'],
      missing: List<String>.from(json['missing']),
      messages: Map<String, String>.from(json['messages']),
      suggested: List<String>.from(json['suggested']),
    );
  }
}

Future<ActivationStatus> checkActivationStatus() async {
  final response = await http.get(
    Uri.parse('https://api.zapi10.com/api/users/me/activation-status'),
    headers: {
      'Authorization': 'Bearer $authToken',
      'Content-Type': 'application/json',
    },
  );
  
  return ActivationStatus.fromJson(jsonDecode(response.body));
}
```

---

## Fluxo Recomendado no App

### 1️⃣ **Login/Splash Screen**
```
Usuário faz login
   ↓
Chama GET /api/users/me/activation-status
   ↓
if (enabled === false) {
   Redireciona para tela de "Complete seu Cadastro"
} else {
   Libera acesso total ao app
}
```

### 2️⃣ **Tela "Complete seu Cadastro"**
```
Exibe lista de pendências com base em status.missing:

[ ] Cadastre um veículo           → Botão: "Cadastrar Agora"
[ ] Cadastre sua conta bancária   → Botão: "Adicionar Conta"
[ ] Preencha seu telefone         → Botão: "Atualizar Perfil"

Progresso: 0/3 itens completados
```

### 3️⃣ **Validação Contínua**
- ✅ Chamar endpoint após cada cadastro concluído
- ✅ Atualizar UI em tempo real
- ✅ Quando `enabled === true`, liberar navegação completa

---

## Tratamento de Erros

### 401 Unauthorized
```json
{
  "error": "Token inválido ou expirado"
}
```
**Ação:** Redirecionar para login

### 404 Not Found
```json
{
  "error": "Usuário não encontrado"
}
```
**Ação:** Fazer logout e voltar para login

### 500 Internal Server Error
```json
{
  "error": "Erro interno do servidor"
}
```
**Ação:** Exibir mensagem genérica e tentar novamente

---

## Notas Importantes

1. **Cache:** Não fazer cache dessa resposta. Sempre chamar o endpoint para ter dados atualizados.

2. **Frequência:** Chamar quando:
   - Usuário faz login
   - Usuário volta para tela principal (app resume)
   - Após cadastrar veículo/conta/cartão
   - Após atualizar perfil

3. **Performance:** Endpoint é rápido (< 100ms) pois usa queries otimizadas.

4. **Campos futuros:** Podem ser adicionados novos campos em `missing` conforme evolução do sistema.

---

## Changelog

| Versão | Data | Descrição |
|--------|------|-----------|
| 1.0 | 2026-02-18 | Versão inicial do endpoint |

---

## Suporte

Em caso de dúvidas ou problemas:
- 📧 Email: dev@zapi10.com
- 💬 Slack: #mobile-dev
- 📱 WhatsApp: (11) 99999-9999
