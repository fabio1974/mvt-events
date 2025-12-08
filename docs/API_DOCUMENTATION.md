# 📚 Documentação Completa da API MVT Events

## 🌐 Acesso ao Swagger UI

A documentação interativa está disponível em:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

## 🔐 Autenticação

Todas as rotas (exceto `/api/auth/login` e `/api/events/public/*`) requerem autenticação JWT.

### Como Autenticar

1. **Login**

   ```bash
   curl -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"email": "admin@test.com", "password": "password123"}'
   ```

2. **Copiar o token** retornado no campo `token`

3. **Usar em requisições**

   ```bash
   curl -H "Authorization: Bearer SEU_TOKEN_AQUI" \
     http://localhost:8080/api/events
   ```

4. **Swagger UI**: Clicar no botão 🔓 **Authorize** e colar o token

---

## 🎯 Endpoints Principais

### 📅 Events (`/api/events`)

#### Listar Eventos (GET `/api/events`)

**Filtros Disponíveis:**

| Parâmetro                         | Tipo    | Valores                                               | Descrição                         |
| --------------------------------- | ------- | ----------------------------------------------------- | --------------------------------- |
| `status`                          | Enum    | DRAFT, PUBLISHED, CANCELLED, COMPLETED                | Status do evento                  |
| `organization` / `organizationId` | Long    | ID                                                    | Organização responsável           |
| `category` / `categoryId`         | Long    | ID                                                    | Categoria do evento               |
| `city`                            | String  | Nome                                                  | Busca parcial pelo nome da cidade |
| `state`                           | String  | Sigla                                                 | Sigla do estado (ex: SP, RJ)      |
| `eventType`                       | Enum    | RUNNING, CYCLING, SWIMMING, TRIATHLON, WALKING, OTHER | Tipo de evento                    |
| `name`                            | String  | Texto                                                 | Busca parcial pelo nome           |
| `page`                            | Integer | 0+                                                    | Número da página (default: 0)     |
| `size`                            | Integer | 1-100                                                 | Tamanho da página (default: 20)   |
| `sort`                            | String  | campo,direção                                         | Ordenação (ex: eventDate,desc)    |

**Exemplos de Uso:**

```bash
# Filtrar por tipo e nome
curl "http://localhost:8080/api/events?eventType=RUNNING&name=maratona" \
  -H "Authorization: Bearer TOKEN"

# Filtrar por cidade e status
curl "http://localhost:8080/api/events?city=São%20Paulo&status=PUBLISHED" \
  -H "Authorization: Bearer TOKEN"

# Combinar múltiplos filtros
curl "http://localhost:8080/api/events?organization=5&eventType=RUNNING&registrationOpen=true" \
  -H "Authorization: Bearer TOKEN"

# Paginação e ordenação
curl "http://localhost:8080/api/events?page=0&size=10&sort=eventDate,desc" \
  -H "Authorization: Bearer TOKEN"
```

**Outros Endpoints de Events:**

- `GET /api/events/{id}` - Buscar por ID
- `GET /api/events/organization/{organizationId}` - Listar por organização
- `GET /api/events/public` - Eventos públicos (sem autenticação)
- `GET /api/events/stats` - Estatísticas
- `POST /api/events` - Criar evento
- `PUT /api/events/{id}` - Atualizar evento
- `DELETE /api/events/{id}` - Deletar evento
- `GET /api/events/metadata` - Metadata automático com campos e traduções

---

###  Payments (`/api/payments`)

#### Listar Pagamentos (GET `/api/payments`)

**Filtros Disponíveis:**

| Parâmetro | Tipo    | Valores                              | Descrição            |
| --------- | ------- | ------------------------------------ | -------------------- |
| `status`  | Enum    | PENDING, COMPLETED, FAILED, REFUNDED | Status do pagamento  |
| `provider`| String  | stripe, mercadopago                  | Gateway de pagamento |
| `page`    | Integer | 0+                                   | Número da página     |
| `size`    | Integer | 1-100                                | Tamanho da página    |
| `sort`    | String  | campo,direção                        | Ordenação            |

**Exemplos de Uso:**

```bash
# Filtrar por status
curl "http://localhost:8080/api/payments?status=COMPLETED" \
  -H "Authorization: Bearer TOKEN"

# Filtrar por provider
  -H "Authorization: Bearer TOKEN"

# Filtrar por gateway
curl "http://localhost:8080/api/payments?provider=stripe&status=COMPLETED" \
  -H "Authorization: Bearer TOKEN"

# Ordenar por data de processamento
curl "http://localhost:8080/api/payments?sort=processedAt,desc&size=50" \
  -H "Authorization: Bearer TOKEN"
```

**Outros Endpoints de Payments:**

- `POST /api/payments/create` - Criar pagamento
- `POST /api/payments/process` - Processar pagamento
- `POST /api/payments/{id}/refund` - Reembolsar pagamento
- `GET /api/payments/metadata` - Metadata automático

---

### 🏢 Organizations (`/api/organizations`)

#### Listar Organizações (GET `/api/organizations`)

**Filtros Disponíveis:**

| Parâmetro | Tipo    | Valores       | Descrição                            |
| --------- | ------- | ------------- | ------------------------------------ |
| `search`  | String  | Texto         | Busca em nome, slug ou email         |
| `active`  | Boolean | true/false    | Filtrar organizações ativas/inativas |
| `page`    | Integer | 0+            | Número da página                     |
| `size`    | Integer | 1-100         | Tamanho da página                    |
| `sort`    | String  | campo,direção | Ordenação                            |

**Exemplos de Uso:**

```bash
# Buscar por nome
curl "http://localhost:8080/api/organizations?search=sport" \
  -H "Authorization: Bearer TOKEN"

# Filtrar organizações ativas
curl "http://localhost:8080/api/organizations?active=true" \
  -H "Authorization: Bearer TOKEN"

# Combinar filtros
curl "http://localhost:8080/api/organizations?search=club&active=true&sort=name,asc" \
  -H "Authorization: Bearer TOKEN"
```

**Outros Endpoints de Organizations:**

- `GET /api/organizations/{id}` - Buscar por ID
- `POST /api/organizations` - Criar organização
- `PUT /api/organizations/{id}` - Atualizar organização
- `DELETE /api/organizations/{id}` - Deletar organização
- `GET /api/organizations/metadata` - Metadata automático

---

## 🔍 Sistema de Metadata

Cada entidade possui um endpoint `/metadata` que retorna automaticamente:

- **Campos**: Lista de todos os campos com tipos, validações e traduções PT-BR
- **Enums**: Valores possíveis de cada enum com traduções
- **Relacionamentos**: Informações sobre campos de relacionamento
- **Configurações**: Largura de colunas, campos obrigatórios, etc.

### Exemplo de Uso

```bash
# Obter metadata de Events
curl http://localhost:8080/api/events/metadata \
  -H "Authorization: Bearer TOKEN"
```

**Resposta:**

```json
{
  "entityConfig": {
    "labelField": "name",
    "descriptionField": "description"
  },
  "fields": [
    {
      "name": "name",
      "type": "STRING",
      "label": "Nome do Evento",
      "required": true,
      "maxLength": 200,
      "visible": {
        "table": true,
        "form": true,
        "filter": true
      }
    },
    {
      "name": "eventType",
      "type": "ENUM",
      "label": "Tipo de Evento",
      "enumValues": [
        { "value": "RUNNING", "label": "Corrida" },
        { "value": "CYCLING", "label": "Ciclismo" },
        { "value": "SWIMMING", "label": "Natação" }
      ]
    }
  ]
}
```

---

## 📊 Paginação e Ordenação

### Parâmetros de Paginação

- `page`: Número da página (começa em 0)
- `size`: Quantidade de itens por página (default: 20, max: 100)
- `sort`: Campo e direção da ordenação

### Exemplos

```bash
# Página 2, 50 itens
?page=1&size=50

# Ordenar por data crescente
?sort=eventDate,asc

# Ordenar por múltiplos campos
?sort=eventDate,desc&sort=name,asc

# Combinar paginação e filtros
?page=0&size=20&eventType=RUNNING&sort=eventDate,desc
```

### Resposta Paginada

```json
{
  "content": [...],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": {...}
  },
  "totalElements": 100,
  "totalPages": 5,
  "last": false,
  "first": true,
  "numberOfElements": 20
}
```

---

## 🎨 Enums e Valores Permitidos

### EventType

- `RUNNING` - Corrida
- `CYCLING` - Ciclismo
- `SWIMMING` - Natação
- `TRIATHLON` - Triatlo
- `WALKING` - Caminhada
- `OTHER` - Outro

### EventStatus

- `DRAFT` - Rascunho
- `PUBLISHED` - Publicado
- `CANCELLED` - Cancelado
- `COMPLETED` - Concluído

### PaymentStatus

- `PENDING` - Pendente
- `COMPLETED` - Concluído
- `FAILED` - Falhou
- `REFUNDED` - Reembolsado

### PaymentMethod

- `CREDIT_CARD` - Cartão de Crédito
- `DEBIT_CARD` - Cartão de Débito
- `PIX` - PIX
- `BOLETO` - Boleto
- `PAYPAL` - PayPal

### Gender

- `MALE` - Masculino
- `FEMALE` - Feminino
- `MIXED` - Misto

### DistanceUnit

- `METERS` - Metros
- `KILOMETERS` - Quilômetros
- `MILES` - Milhas

---

## 🚀 Dicas de Uso

### 1. Filtros Combinados

Todos os filtros podem ser combinados usando lógica **AND**:

```bash
/api/events?eventType=RUNNING&city=São%20Paulo&status=PUBLISHED&registrationOpen=true
```

### 2. Busca Parcial

Campos de texto usam busca parcial e case-insensitive:

```bash
/api/events?name=marat  # Encontra "Maratona de SP", "Marathon NYC", etc.
```

### 3. Aceita Nomes Curtos e Longos

Os parâmetros de relacionamento aceitam ambas as formas:

```bash
/api/events?organization=5  # ✅
/api/events?organizationId=5  # ✅ (mesmo resultado)
```

### 4. Ordenação Múltipla

Você pode ordenar por múltiplos campos:

```bash
?sort=status,asc&sort=eventDate,desc&sort=name,asc
```

### 5. Metadata para Construir UI

Use os endpoints `/metadata` para construir interfaces dinâmicas:

- Gerar formulários automaticamente
- Criar filtros com base nos campos disponíveis
- Exibir traduções em português
- Aplicar validações do backend no frontend

---

## 📖 Exemplos Práticos

### Cenário 1: Buscar Eventos de Corrida em SP

```bash
curl "http://localhost:8080/api/events?eventType=RUNNING&city=São%20Paulo&status=PUBLISHED" \
  -H "Authorization: Bearer TOKEN"
```

### Cenário 2: Listar Inscrições Confirmadas de um Evento

```bash
curl "http://localhost:8080/api/registrations?event=10&status=CONFIRMED" \
  -H "Authorization: Bearer TOKEN"
```

### Cenário 3: Verificar Pagamentos Completados do Stripe

```bash
curl "http://localhost:8080/api/payments?provider=stripe&status=COMPLETED&sort=processedAt,desc" \
  -H "Authorization: Bearer TOKEN"
```

### Cenário 4: Buscar Organizações Ativas de Esporte

```bash
curl "http://localhost:8080/api/organizations?search=sport&active=true" \
  -H "Authorization: Bearer TOKEN"
```

---

## 🔧 Troubleshooting

### Erro 401 Unauthorized

- Verifique se o token JWT está correto
- Verifique se o token não expirou (validade: 5 horas)
- Certifique-se de incluir o header `Authorization: Bearer TOKEN`

### Erro 400 Bad Request

- Verifique se os valores dos enums estão corretos (RUNNING, não Running)
- Verifique se os UUIDs estão no formato correto
- Verifique se os tipos dos parâmetros estão corretos (Long, Boolean, etc.)

### Filtros Não Funcionam

- Verifique se o nome do parâmetro está correto
- Lembre-se que a busca em texto é case-insensitive e parcial
- Use `%20` ou `+` para espaços em URLs

### Paginação Vazia

- Verifique se o número da página não excede o total de páginas
- Verifique o campo `totalElements` na resposta para saber quantos itens existem

---

## 📞 Suporte

Para dúvidas ou problemas:

- **Email**: support@mvt-events.com
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI Docs**: http://localhost:8080/v3/api-docs
