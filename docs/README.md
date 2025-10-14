# 📚 MVT Events - Documentação

> Sistema completo de gestão de eventos esportivos com multi-tenancy, metadata genérica e integração de pagamentos.

**Última atualização:** 14 de outubro de 2025

---

## 📋 Índice Rápido

| Você é...                 | Comece por...                                     |
| ------------------------- | ------------------------------------------------- |
| 👨‍💻 **Backend Developer**  | [Guia de Desenvolvimento Backend](#-guia-backend) |
| 🎨 **Frontend Developer** | [Guia de API](#-guia-de-api)                      |
| 📊 **Product Owner**      | [Features e Status](#-features-principais)        |
| 🏗️ **Arquiteto**          | [Arquitetura do Sistema](#-arquitetura)           |

---

## 🚀 Quick Start

### Rodar o Projeto

```bash
# 1. Subir banco de dados
docker-compose up -d

# 2. Rodar aplicação
./gradlew bootRun

# 3. Acessar
# API: http://localhost:8080
# Swagger: http://localhost:8080/swagger-ui/index.html
```

### Testar API

```bash
# Metadata de uma entidade
curl http://localhost:8080/api/metadata/event | jq

# Listar eventos
curl http://localhost:8080/api/events | jq
```

---

## 📂 Estrutura da Documentação

```
docs/
├── README.md                          # 📍 Você está aqui
│
├── 🏗️ architecture/                   # Arquitetura e Design Patterns
│   └── METADATA_ARCHITECTURE.md       # Sistema de metadata unificado
│
├── 📡 api/                             # Documentação de API
│   └── FILTERS_GUIDE.md               # Guia completo de filtros
│
├── 💡 features/                        # Features específicas
│   └── ENTITY_FILTERS.md              # Filtros por relacionamento
│
├── 🔧 implementation/                  # Implementações técnicas
│   ├── ANNOTATIONS_GUIDE.md           # 📚 Guia de annotations (@DisplayLabel, @Visible, @Computed)
│   ├── CASCADE_HELPER_README.md       # 🌟 Helper para relacionamentos 1:N
│   ├── CASCADE_UPDATE_1_N.md          # Detalhes de cascade update
│   ├── CASCADE_UPDATE_HELPER_USAGE.md # Exemplos de uso do helper
│   ├── DISPLAYLABEL_FORMFIELDS_FIX.md # Fix de @DisplayLabel
│   ├── STATUS.md                      # Status do projeto
│   └── CHANGELOG.md                   # Histórico de mudanças
│
├── 🗄️ backend/                         # Implementações backend
│   └── COMPUTED_FIELDS_IMPLEMENTATION.md # Campos computados
│
└── 📊 metadata/                        # Sistema de Metadata (LEGADO)
    ├── README.md                      # ⚠️ Documentação antiga
    ├── JPA_EXTRACTION.md
    ├── UNIFIED_ENDPOINT.md
    └── ... (arquivos legados)
```

---

## 🎯 Guia Backend

### 1. Entender a Arquitetura

**Leitura obrigatória:**

1. 📖 [Arquitetura de Metadata](./architecture/METADATA_ARCHITECTURE.md)
   - Como funciona o sistema de metadata
   - JPA annotations e extração automática
   - Tradução de campos e enums

### 2. Começar a Desenvolver

**🆕 Iniciantes no projeto:**

1. 📚 [Guia de Annotations Customizadas](./implementation/ANNOTATIONS_GUIDE.md) - **COMECE AQUI**
   - `@DisplayLabel` - Marca o campo principal da entidade
   - `@Visible` - Controla visibilidade em tabela/form/filtros
   - `@Computed` - Campos calculados automaticamente
   - Exemplos práticos e exercícios

### 3. Implementar Novas Features

**Relacionamentos 1:N (Event → Categories, Order → Items, etc.):**

1. 🌟 [CASCADE_HELPER_README.md](./implementation/CASCADE_HELPER_README.md) - **Quick Reference**
2. 📚 [CASCADE_UPDATE_HELPER_USAGE.md](./implementation/CASCADE_UPDATE_HELPER_USAGE.md) - Exemplos completos
3. 🔍 [CASCADE_UPDATE_1_N.md](./implementation/CASCADE_UPDATE_1_N.md) - Detalhes técnicos

**Campos Computados:**

- 📄 [COMPUTED_FIELDS_IMPLEMENTATION.md](./backend/COMPUTED_FIELDS_IMPLEMENTATION.md)

**Filtros:**

- 📄 [ENTITY_FILTERS.md](./features/ENTITY_FILTERS.md)
- 📄 [FILTERS_GUIDE.md](./api/FILTERS_GUIDE.md)

### 3. Padrões de Código

#### Criar Nova Entidade

```java
@Entity
@Table(name = "my_entities")
@Data
public class MyEntity extends BaseEntity {

    @DisplayLabel  // ← Campo principal para exibição
    @Column(nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "parent_id")
    @Visible(form = true, table = true, filter = true)
    private Parent parent;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Status status;
}
```

#### Implementar Cascade Update (1:N)

```java
@Service
@Transactional
public class MyEntityService {
    private final CascadeUpdateHelper cascadeUpdateHelper;

    public MyEntity update(Long id, MyEntity entityData) {
        MyEntity saved = repository.save(existing);

        if (entityData.getChildren() != null) {
            List<Child> existingChildren = childRepo.findByParentId(saved.getId());

            cascadeUpdateHelper.updateChildrenWithInit(
                saved,
                entityData.getChildren(),
                existingChildren,
                Child::getId,
                Child::setParent,
                (existing, payload) -> { /* update fields */ },
                (child) -> { /* init defaults */ },
                childRepo
            );
        }

        return saved;
    }
}
```

📚 **Mais exemplos:** [CASCADE_UPDATE_HELPER_USAGE.md](./implementation/CASCADE_UPDATE_HELPER_USAGE.md)

---

## 📡 Guia de API

### Endpoints Principais

| Endpoint                     | Descrição                      | Autenticação |
| ---------------------------- | ------------------------------ | ------------ |
| `GET /api/metadata/{entity}` | Metadata de uma entidade       | ❌ Não       |
| `GET /api/metadata`          | Metadata de todas as entidades | ❌ Não       |
| `GET /api/{entity}`          | Listar entidades (com filtros) | ✅ Sim       |
| `GET /api/{entity}/{id}`     | Buscar por ID                  | ✅ Sim       |
| `POST /api/{entity}`         | Criar                          | ✅ Sim       |
| `PUT /api/{entity}/{id}`     | Atualizar (com cascade)        | ✅ Sim       |
| `DELETE /api/{entity}/{id}`  | Deletar                        | ✅ Sim       |

### Sistema de Metadata

**O que é?** Um único endpoint que retorna **tudo** que o frontend precisa para renderizar tabelas, formulários e filtros.

```bash
curl http://localhost:8080/api/metadata/event | jq
```

**Retorna:**

```json
{
  "name": "event",
  "label": "Eventos",
  "endpoint": "/api/events",
  "labelField": "name",
  "tableFields": [
    {
      "name": "name",
      "label": "Nome",
      "type": "string",
      "visible": true,
      "sortable": true,
      "width": 200
    }
  ],
  "formFields": [
    {
      "name": "name",
      "label": "Nome",
      "type": "string",
      "required": true,
      "maxLength": 200
    }
  ],
  "filters": [
    {
      "name": "status",
      "label": "Status",
      "type": "select",
      "options": [
        { "value": "DRAFT", "label": "Rascunho" },
        { "value": "PUBLISHED", "label": "Publicado" }
      ]
    }
  ]
}
```

📚 **Documentação completa:** [METADATA_ARCHITECTURE.md](./architecture/METADATA_ARCHITECTURE.md)

### Filtros Dinâmicos

Todos os endpoints de listagem suportam filtros:

```bash
# Filtrar por status
GET /api/events?status=PUBLISHED

# Filtrar por relacionamento
GET /api/registrations?event.id=19

# Múltiplos filtros
GET /api/events?status=PUBLISHED&eventType=RUNNING&city=São+Paulo
```

📚 **Guia completo:** [FILTERS_GUIDE.md](./api/FILTERS_GUIDE.md)

### Atualização com Cascade (1:N)

```bash
# Atualizar Event com Categories
curl -X PUT http://localhost:8080/api/events/19 \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "Evento Atualizado",
    "categories": [
      {"id": 22, "name": "10KM", "price": 100},     // UPDATE
      {"name": "21KM", "price": 150}                 // INSERT (novo)
    ]
  }'
```

**Comportamento:**

- Categoria `{"id": 22}` → **UPDATE**
- Categoria sem `id` → **INSERT**
- Categorias no banco mas não no payload → **DELETE**

📚 **Documentação completa:** [CASCADE_HELPER_README.md](./implementation/CASCADE_HELPER_README.md)

---

## 💡 Features Principais

### ✅ Implementado

| Feature                  | Descrição                              | Documentação                                          |
| ------------------------ | -------------------------------------- | ----------------------------------------------------- |
| 🔐 **Multi-tenancy**     | Isolamento por organização             | -                                                     |
| 📊 **Metadata Genérica** | Sistema unificado de metadata          | [📄](./architecture/METADATA_ARCHITECTURE.md)         |
| 🔍 **Filtros Dinâmicos** | Filtros automáticos por tipo           | [📄](./api/FILTERS_GUIDE.md)                          |
| 🔗 **Entity Filters**    | Filtros por relacionamento (typeahead) | [📄](./features/ENTITY_FILTERS.md)                    |
| ♻️ **Cascade Update**    | Atualização 1:N em transação única     | [📄](./implementation/CASCADE_HELPER_README.md)       |
| 💻 **Campos Computados** | @Computed para cálculos automáticos    | [📄](./backend/COMPUTED_FIELDS_IMPLEMENTATION.md)     |
| 🏷️ **@DisplayLabel**     | Campo principal para UI                | [📄](./implementation/DISPLAYLABEL_FORMFIELDS_FIX.md) |
| 🌐 **Traduções**         | PT-BR para campos e enums              | [📄](./architecture/METADATA_ARCHITECTURE.md)         |

### 🚧 Em Desenvolvimento

- [ ] Sistema de pagamentos (integração completa)
- [ ] Upload de arquivos (fotos de eventos)
- [ ] Notificações por email
- [ ] Dashboard analytics

📊 **Status completo:** [STATUS.md](./implementation/STATUS.md)

---

## 🏗️ Arquitetura

### Stack Tecnológica

**Backend:**

- Java 21 + Spring Boot 3.x
- Spring Data JPA + Hibernate
- PostgreSQL 15
- Flyway (migrations)
- JWT (autenticação)

**Frontend (separado):**

- React + TypeScript
- TanStack Query + Table
- Axios

### Padrões de Projeto

| Padrão                         | Uso                           | Documentação                                    |
| ------------------------------ | ----------------------------- | ----------------------------------------------- |
| **Repository Pattern**         | Acesso a dados                | Spring Data JPA                                 |
| **Specification Pattern**      | Queries dinâmicas             | JPA Criteria API                                |
| **DTO Pattern**                | Transferência de dados        | -                                               |
| **Helper Pattern**             | Lógica reutilizável (Cascade) | [📄](./implementation/CASCADE_HELPER_README.md) |
| **Annotation-driven Metadata** | Extração automática           | [📄](./architecture/METADATA_ARCHITECTURE.md)   |

### Diagrama de Componentes

```
┌─────────────────────────────────────────────────────┐
│                    Frontend (React)                  │
└────────────────────┬────────────────────────────────┘
                     │ HTTP/REST
┌────────────────────▼────────────────────────────────┐
│                  Controllers                         │
│  ┌──────────┐  ┌──────────┐  ┌──────────────────┐ │
│  │  Event   │  │  Metadata│  │  Registration    │ │
│  │Controller│  │Controller│  │  Controller      │ │
│  └─────┬────┘  └────┬─────┘  └────────┬─────────┘ │
└────────┼────────────┼─────────────────┼───────────┘
         │            │                 │
┌────────▼────────────▼─────────────────▼───────────┐
│                    Services                        │
│  ┌──────────┐  ┌──────────┐  ┌──────────────────┐ │
│  │  Event   │  │ Metadata │  │ CascadeUpdate    │ │
│  │ Service  │  │ Service  │  │    Helper        │ │
│  └─────┬────┘  └────┬─────┘  └────────┬─────────┘ │
└────────┼────────────┼─────────────────┼───────────┘
         │            │                 │
┌────────▼────────────▼─────────────────▼───────────┐
│              Repositories (JPA)                    │
│  ┌──────────┐  ┌──────────┐  ┌──────────────────┐ │
│  │  Event   │  │EventCat. │  │ Registration     │ │
│  │   Repo   │  │   Repo   │  │     Repo         │ │
│  └─────┬────┘  └────┬─────┘  └────────┬─────────┘ │
└────────┼────────────┼─────────────────┼───────────┘
         │            │                 │
         └────────────▼─────────────────┘
                      │
              ┌───────▼────────┐
              │   PostgreSQL   │
              └────────────────┘
```

📚 **Detalhes:** [METADATA_ARCHITECTURE.md](./architecture/METADATA_ARCHITECTURE.md)

---

## 📝 Changelog

### 2025-10-14

**✨ Adicionado:**

- `CascadeUpdateHelper` - Helper genérico para relacionamentos 1:N
- Documentação completa de cascade updates
- Fix de `@DisplayLabel` no formFields

**🔧 Modificado:**

- `EventService.update()` - Agora usa `CascadeUpdateHelper`
- Reorganização completa da documentação

**📚 Documentação:**

- Novo `CASCADE_HELPER_README.md`
- Novo `CASCADE_UPDATE_HELPER_USAGE.md`
- Atualizado `README.md` principal

📜 **Histórico completo:** [CHANGELOG.md](./implementation/CHANGELOG.md)

---

## 🤝 Contribuindo

### Adicionar Nova Feature

1. Implemente no código
2. Adicione testes
3. Atualize documentação em `docs/implementation/`
4. Adicione entrada no `CHANGELOG.md`
5. Atualize `STATUS.md`

### Documentação

- **Novas features:** `docs/implementation/`
- **Guias de API:** `docs/api/`
- **Arquitetura:** `docs/architecture/`
- **Features específicas:** `docs/features/`

---

## 📞 Suporte

- **Issues:** GitHub Issues
- **Email:** moveltrack@gmail.com
- **Documentação:** Este README + subpastas

---

## 📄 Licença

Propriedade de Moveltrack Sistemas © 2025

---

## 🗂️ Documentação Legada

A pasta `docs/metadata/` contém documentação de versões anteriores do sistema de metadata. Mantenha para referência histórica, mas use as novas documentações em `docs/architecture/` e `docs/implementation/`.

**Migração:**

- ~~`metadata/README.md`~~ → `architecture/METADATA_ARCHITECTURE.md` ✅
- ~~`metadata/JPA_EXTRACTION.md`~~ → Incorporado em `METADATA_ARCHITECTURE.md` ✅
- ~~`metadata/UNIFIED_ENDPOINT.md`~~ → Incorporado em `METADATA_ARCHITECTURE.md` ✅
