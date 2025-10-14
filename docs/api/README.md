# 📡 API

Documentação de endpoints e uso da API.

---

## 📄 Documentos

### [FILTERS_GUIDE.md](./FILTERS_GUIDE.md) 🌟

**Guia completo de filtros** disponíveis para cada entidade.

**Tópicos:**

- Filtros por entidade (Event, Registration, etc.)
- Sintaxe de query parameters
- Filtros por tipo (text, select, date, entity)
- Exemplos práticos com curl

**Para quem:**

- Frontend developers
- QA/Testers
- Integradores de API

---

### [QUICK_START_API.md](./QUICK_START_API.md) ⚡

**Quick start** para usar a API de eventos.

**Tópicos:**

- Atualização de eventos (PUT)
- Payload com entidade genérica
- Relacionamentos (city, organization)
- Exemplos práticos

---

### [FRONTEND_API_UPDATE_GUIDE.md](./FRONTEND_API_UPDATE_GUIDE.md) 📚

**Guia detalhado** de atualização de entidades para frontend.

**Tópicos:**

- Endpoint de update genérico
- Como estruturar payloads
- Tratamento de erros
- Validação de campos

---

### [REACT_EXAMPLE.md](./REACT_EXAMPLE.md) 🎨

**Exemplo completo** de formulário React/TypeScript.

**Tópicos:**

- Event Form Component
- Integração com React Query
- TypeScript types
- Best practices

---

### [API_TESTING_CURL.md](./API_TESTING_CURL.md) 🧪

**Testes com cURL** para validação da API.

**Tópicos:**

- Listar eventos
- Criar e atualizar eventos
- Validação de relacionamentos
- Exemplos de payloads completos

---

## 🔗 Links Relacionados

- [Arquitetura de Metadata](../architecture/METADATA_ARCHITECTURE.md) - Como são gerados os filtros
- [Entity Filters](../features/ENTITY_FILTERS.md) - Filtros por relacionamento

---

## 💡 Exemplos Rápidos

### Listar Eventos Publicados

```bash
curl "http://localhost:8080/api/events?status=PUBLISHED" | jq
```

### Filtrar por Organização

```bash
curl "http://localhost:8080/api/events?organization.id=6" | jq
```

### Múltiplos Filtros

```bash
curl "http://localhost:8080/api/events?status=PUBLISHED&eventType=RUNNING&city=São+Paulo" | jq
```

---

**Voltar para:** [Documentação Principal](../README.md) | [Índice Completo](../INDEX.md)
