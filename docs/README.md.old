# 📚 Documentação MVT Events# 📚 Documentação MVT Events

Sistema completo de gestão de eventos esportivos com multi-tenancy, metadata genérica e integração de pagamentos.## 📖 Índice

---### 🏗️ Arquitetura

## 📖 Índice- [Arquitetura de Metadata](./architecture/METADATA_ARCHITECTURE.md) - Sistema completo de metadata e filtros

- [Especificações JPA](./architecture/JPA_SPECIFICATIONS.md) - Padrão de queries dinâmicas

### 🚀 Início Rápido

- [Instalação e Setup](../README.md#-getting-started)### 🔧 API

- [Testes](./TESTING.md)

- [Segurança](./SECURITY.md)- [Guia de Filtros](./api/FILTERS_GUIDE.md) - Todos os filtros disponíveis por entidade

- [Breaking Changes](./api/BREAKING_CHANGES.md) - Mudanças de API e migração

### 🏗️ Arquitetura

- [**Sistema de Metadata Unificado**](./metadata/README.md) ⭐ **PRINCIPAL**### 💡 Features

  - [Endpoint Unificado](./metadata/UNIFIED_ENDPOINT.md)

  - [Extração via JPA](./metadata/JPA_EXTRACTION.md)- [Entity Filters](./features/ENTITY_FILTERS.md) - Sistema de filtros por relacionamento

  - [Antes vs Agora](./metadata/COMPARISON.md)- [Select vs Typeahead](./features/SELECT_VS_TYPEAHEAD.md) - Guia de decisão para UI

- [Entity Filters](./features/ENTITY_FILTERS.md)

- [Filtros Avançados](./api/FILTERS_GUIDE.md)### 🚀 Implementação

### 📡 API- [Status do Projeto](./implementation/STATUS.md) - Estado atual e próximos passos

- [Guia de Filtros](./api/FILTERS_GUIDE.md)- [Histórico de Mudanças](./implementation/CHANGELOG.md) - Log de implementações

- [Metadata API](./metadata/README.md)

---

### ⚙️ Features

- [Entity Filters](./features/ENTITY_FILTERS.md)## 🎯 Quick Start

- [Multi-tenancy](../README.md#-arquitetura)

- [Sistema de Pagamentos](../README.md#principais-features)### Para Desenvolvedores Backend

### 🔧 Implementação1. Leia [Arquitetura de Metadata](./architecture/METADATA_ARCHITECTURE.md)

- [Status do Projeto](./implementation/STATUS.md)2. Consulte [JPA Specifications](./architecture/JPA_SPECIFICATIONS.md)

- [Changelog](./implementation/CHANGELOG.md)3. Veja [Status do Projeto](./implementation/STATUS.md)

---### Para Desenvolvedores Frontend

## 🎯 O Que É Este Sistema?1. Leia [Guia de Filtros](./api/FILTERS_GUIDE.md)

2. Consulte [Breaking Changes](./api/BREAKING_CHANGES.md)

**MVT Events** é uma plataforma para gestão completa de eventos esportivos (corridas, ciclismo, triatlon, etc.) com foco em:3. Veja [Select vs Typeahead](./features/SELECT_VS_TYPEAHEAD.md)

### 1. ✨ Metadata Genérica### Para Product Owners

**Um único endpoint retorna tudo que o frontend precisa:**1. Leia [Entity Filters](./features/ENTITY_FILTERS.md)

2. Consulte [Status do Projeto](./implementation/STATUS.md)

````bash3. Veja [Histórico de Mudanças](./implementation/CHANGELOG.md)

GET /api/metadata/event

```---



```json## 📂 Estrutura de Pastas

{

  "name": "event",```

  "label": "Eventos",docs/

  "tableFields": [...]    // Campos para tabelas├── README.md                          # Este arquivo

  "formFields": [...]      // Campos para formulários com validações├── architecture/                      # Arquitetura e padrões

  "filters": [...]         // Filtros de busca│   ├── METADATA_ARCHITECTURE.md

  "pagination": {...}      // Configuração de paginação│   └── JPA_SPECIFICATIONS.md

}├── api/                               # Documentação de API

```│   ├── FILTERS_GUIDE.md

│   └── BREAKING_CHANGES.md

**Benefícios:**├── features/                          # Features específicas

- ✅ Frontend 100% dinâmico│   ├── ENTITY_FILTERS.md

- ✅ Mudanças no backend refletem automaticamente│   └── SELECT_VS_TYPEAHEAD.md

- ✅ Zero duplicação de código└── implementation/                    # Implementação e status

- ✅ Enums com options automáticas    ├── STATUS.md

- ✅ Relacionamentos nested completos    └── CHANGELOG.md

````

[📖 Saiba mais sobre Metadata](./metadata/README.md)

---

---

## 🔗 Links Úteis

### 2. 🔍 Filtros Dinâmicos

- [Repositório](https://github.com/seu-repo/mvt-events)

Sistema avançado de filtros com JPA Specifications:- [API Documentation](http://localhost:8080/swagger-ui.html)

- [Jira Board](#)

```bash
GET /api/events?status=PUBLISHED&city=São Paulo&organizationId=123
```

**Características:**

- ✅ Filtros por campo simples
- ✅ Filtros por relacionamento (entity filters)
- ✅ Busca full-text
- ✅ Ordenação e paginação
- ✅ Autodiscovery via annotations

[📖 Saiba mais sobre Filtros](./api/FILTERS_GUIDE.md)

---

### 3. 🏢 Multi-tenancy

Isolamento completo de dados por organização:

- ✅ RLS (Row Level Security) no PostgreSQL
- ✅ Tenant context automático
- ✅ Segurança por organização
- ✅ Compartilhamento opcional de dados

---

### 4. 💳 Pagamentos Integrados

Suporte a múltiplos gateways:

- ✅ Stripe
- ✅ MercadoPago
- ✅ PayPal
- ✅ Webhooks automáticos
- ✅ Gestão de transferências

---

## 🗂️ Estrutura da Documentação

```
docs/
├── README.md                           # 👈 Você está aqui
├── TESTING.md                          # Testes
├── SECURITY.md                         # Segurança
│
├── metadata/                           # ⭐ Sistema de Metadata
│   ├── README.md                       # Visão geral
│   ├── UNIFIED_ENDPOINT.md             # Endpoint unificado
│   ├── JPA_EXTRACTION.md               # Extração via JPA
│   └── COMPARISON.md                   # Antes vs Agora
│
├── api/                                # API Documentation
│   └── FILTERS_GUIDE.md                # Guia de filtros
│
├── features/                           # Features específicas
│   └── ENTITY_FILTERS.md               # Entity filters
│
└── implementation/                     # Status do projeto
    ├── STATUS.md                       # Status atual
    └── CHANGELOG.md                    # Histórico de mudanças
```

---

## 🚀 Começando

### 1. Instalação

Veja o [README principal](../README.md#-getting-started) para instruções completas.

### 2. Entenda a Metadata

O sistema de metadata é o coração da aplicação. Comece por aqui:

1. [**O que mudou**: Antes vs Agora](./metadata/COMPARISON.md)
2. [**Como funciona**: Endpoint Unificado](./metadata/UNIFIED_ENDPOINT.md)
3. [**Detalhes técnicos**: Extração JPA](./metadata/JPA_EXTRACTION.md)

### 3. Explore a API

- [Guia de Filtros](./api/FILTERS_GUIDE.md) - Como usar filtros avançados
- [Entity Filters](./features/ENTITY_FILTERS.md) - Filtros por relacionamento

### 4. Desenvolvimento

- [Testes](./TESTING.md) - Como executar testes
- [Status](./implementation/STATUS.md) - O que está implementado

---

## 🎯 Principais Conceitos

### Metadata Unificado

**Antes:** 2 endpoints separados

```
GET /api/metadata/event        → tableFields
GET /api/metadata/forms/event  → formFields
```

**Agora:** 1 endpoint completo

```
GET /api/metadata/event → tableFields + formFields + filters + pagination
```

[📖 Ver comparação detalhada](./metadata/COMPARISON.md)

---

### Extração Automática via JPA

Os campos de formulário são extraídos **automaticamente** das entidades JPA:

```java
@Entity
public class Event {
    @Column(nullable = false, length = 200)
    private String name;  // → required=true, maxLength=200

    @Enumerated(EnumType.STRING)
    private EventType eventType;  // → type="select" + options[]

    @OneToMany(mappedBy = "event")
    private List<EventCategory> categories;  // → type="nested" + relationship
}
```

[📖 Ver detalhes técnicos](./metadata/JPA_EXTRACTION.md)

---

## 📊 Arquitetura Visual

```
┌─────────────────────────────────────────────────────────┐
│                      Frontend                           │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  1 Request:                                             │
│     GET /api/metadata/event                             │
│           ↓                                             │
│     {                                                   │
│       "tableFields": [...],   ← Para renderizar tabela │
│       "formFields": [...],     ← Para renderizar form   │
│       "filters": [...],        ← Para busca/filtros     │
│       "pagination": {...}      ← Para paginação         │
│     }                                                   │
│                                                         │
├─────────────────────────────────────────────────────────┤
│                      Backend                            │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  MetadataService                                        │
│     ├─→ tableFields (manual)                            │
│     ├─→ formFields (JpaMetadataExtractor via reflection)│
│     ├─→ filters (manual)                                │
│     └─→ pagination (manual)                             │
│                                                         │
│  JpaMetadataExtractor                                   │
│     ├─→ Lê @Column → validações                         │
│     ├─→ Lê @Enumerated → options                        │
│     └─→ Lê @OneToMany → relacionamentos                 │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

---

## 🔗 Links Úteis

### Documentação Interna

- [Sistema de Metadata](./metadata/README.md)
- [Guia de Filtros](./api/FILTERS_GUIDE.md)
- [Testes](./TESTING.md)
- [Segurança](./SECURITY.md)

### Código

- [MetadataService.java](../src/main/java/com/mvt/mvt_events/metadata/MetadataService.java)
- [JpaMetadataExtractor.java](../src/main/java/com/mvt/mvt_events/metadata/JpaMetadataExtractor.java)
- [FormMetadataController.java](../src/main/java/com/mvt/mvt_events/metadata/FormMetadataController.java)

### Endpoints Principais

- `GET /api/metadata/{entity}` - Metadata completo
- `GET /api/events` - Lista eventos (com filtros)
- `GET /api/registrations` - Lista inscrições
- `GET /api/organizations` - Lista organizações

---

## 🤝 Contribuindo

1. Leia a documentação relevante
2. Faça suas alterações
3. Execute os testes: `./gradlew test`
4. Atualize a documentação se necessário
5. Abra um Pull Request

---

## 📞 Suporte

- **Issues:** Use GitHub Issues para reportar problemas
- **Documentação:** Toda doc técnica está nesta pasta
- **Arquitetura:** Veja [metadata/README.md](./metadata/README.md)

---

**💡 Dica:** Comece pela [Comparação Antes vs Agora](./metadata/COMPARISON.md) para entender rapidamente o que mudou no sistema!
