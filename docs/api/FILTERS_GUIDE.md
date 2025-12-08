# 🔍 Guia de Filtros da API

## 📋 Visão Geral

Todos os endpoints de listagem suportam filtros dinâmicos via query parameters.

---

## 🎫 Events (`/api/events`)

### Filtros Disponíveis

| Filtro           | Tipo   | Descrição               | Exemplo                 |
| ---------------- | ------ | ----------------------- | ----------------------- |
| `name`           | text   | Nome do evento          | `?name=Festival`        |
| `organizationId` | entity | Organização responsável | `?organizationId=1`     |
| `categoryId`     | entity | Categoria do evento     | `?categoryId=2`         |
| `status`         | select | Status do evento        | `?status=ACTIVE`        |
| `city`           | text   | Cidade do evento        | `?city=São Paulo`       |
| `startDate`      | date   | Data de início          | `?startDate=2025-01-01` |

### Entity Filters

**organizationId**

- Endpoint: `/api/organizations`
- Label Field: `name`
- Searchable: ✅

**categoryId**

- Endpoint: `/api/event-categories`
- Label Field: `name`
- Searchable: ✅

### Status Options

- `DRAFT` - Rascunho
- `ACTIVE` - Ativo
- `CANCELLED` - Cancelado
- `COMPLETED` - Concluído

---

##  Users (`/api/users`)

### Filtros Disponíveis

| Filtro           | Tipo    | Descrição              | Exemplo                   |
| ---------------- | ------- | ---------------------- | ------------------------- |
| `name`           | text    | Nome do usuário        | `?name=João`              |
| `email`          | text    | E-mail do usuário      | `?email=joao@example.com` |
| `role`           | select  | Papel do usuário       | `?role=ADMIN`             |
| `organizationId` | entity  | Organização do usuário | `?organizationId=1`       |
| `enabled`        | boolean | Usuário ativo          | `?enabled=true`           |

### Entity Filters

**organizationId**

- Endpoint: `/api/organizations`
- Label Field: `name`
- Searchable: ✅

### Role Options

- `ADMIN` - Administrador
- `ORGANIZER` - Organizador
- `USER` - Usuário _(antigo: PARTICIPANT)_

---

## 💳 Payments (`/api/payments`)

### Filtros Disponíveis

| Filtro        | Tipo   | Descrição           | Exemplo                   |
| ------------- | ------ | ------------------- | ------------------------- |
| `status`      | select | Status do pagamento | `?status=COMPLETED`       |
| `paymentDate` | date   | Data do pagamento   | `?paymentDate=2025-01-01` |

### Status Options

- `PENDING` - Pendente
- `COMPLETED` - Concluído
- `FAILED` - Falhou
- `REFUNDED` - Reembolsado

---

## 🏷️ Event Categories (`/api/event-categories`)

### Filtros Disponíveis

| Filtro | Tipo | Descrição         | Exemplo            |
| ------ | ---- | ----------------- | ------------------ |
| `name` | text | Nome da categoria | `?name=Tecnologia` |

---

## 🔧 Operadores de Filtro

### Text Filters

- `equals`: Igualdade exata
- `contains`: Contém (case-insensitive)
- `startsWith`: Começa com
- `endsWith`: Termina com

**Exemplo:**

```
/api/events?name=Festival          # contains (padrão)
/api/events?name:equals=Festival   # exato
```

### Date Filters

- `equals`: Data exata
- `between`: Intervalo de datas
- `before`: Antes de
- `after`: Depois de

**Exemplo:**

```
/api/events?startDate=2025-01-01                          # exato
/api/events?startDate:between=2025-01-01,2025-12-31      # intervalo
```

### Entity Filters

- `equals`: ID exato (único operador)

**Exemplo:**

```
/api/events?organizationId=1
```

### Boolean Filters

- `equals`: true ou false (único operador)

**Exemplo:**

```
/api/users?enabled=true
```

---

## 📄 Paginação e Ordenação

### Paginação

| Parâmetro | Descrição                    | Padrão |
| --------- | ---------------------------- | ------ |
| `page`    | Número da página (0-indexed) | `0`    |
| `size`    | Itens por página             | `10`   |

**Exemplo:**

```
/api/events?page=0&size=20
```

### Ordenação

| Parâmetro | Descrição       | Formato           |
| --------- | --------------- | ----------------- |
| `sort`    | Campo e direção | `field,direction` |

**Exemplo:**

```
/api/events?sort=name,asc
/api/events?sort=startDate,desc
```

### Múltipla Ordenação

```
/api/events?sort=status,asc&sort=name,asc
```

---

## 🔗 Combinando Filtros

### Exemplo Completo

```
GET /api/events?
  organizationId=1&
  categoryId=2&
  status=ACTIVE&
  city=São Paulo&
  startDate:between=2025-01-01,2025-12-31&
  page=0&
  size=20&
  sort=startDate,asc
```

### Response

```json
{
  "content": [...],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 45,
  "totalPages": 3
}
```

---

## ⚠️ Notas Importantes

### Performance

- Filtros de entidade fazem JOIN com tabela relacionada
- Use índices em campos frequentemente filtrados
- Evite `contains` em tabelas muito grandes

### Validação

- IDs de entidade são validados (404 se não existir)
- Datas devem estar no formato ISO-8601
- Enums são case-sensitive

### Limites

- `size` máximo: 100 itens
- Timeout: 30 segundos
- Rate limit: 100 requests/minuto

---

## 🔗 Referências

- [Metadata Architecture](../architecture/METADATA_ARCHITECTURE.md)
- [Breaking Changes](./BREAKING_CHANGES.md)
- [Entity Filters](../features/ENTITY_FILTERS.md)
