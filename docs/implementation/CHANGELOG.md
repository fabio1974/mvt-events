# 📝 Changelog

Todas as mudanças notáveis deste projeto serão documentadas neste arquivo.

---

## [1.1.1] - 2025-10-15

### 🐛 Corrigido

- **MultipleBagFetchException** - Erro 500 no endpoint `/api/registrations/my-registrations`
  - Adicionado `@Fetch(FetchMode.SUBSELECT)` em `Event.categories`
  - Adicionado `@Fetch(FetchMode.SUBSELECT)` em `Registration.payments`
  - Documentação completa em `MULTIPLEBAGFETCH_FIX.md`

### 📚 Documentação

- **ANNOTATIONS_GUIDE.md** - Guia completo de annotations customizadas (@DisplayLabel, @Visible, @Computed)
  - 665 linhas com exemplos práticos
  - Matriz de visibilidade
  - Troubleshooting
  - Exercícios com soluções
- **CLEANUP_SUMMARY.md** - Resumo da reorganização de documentação
- **SESSION_SUMMARY.md** - Resumo executivo da sessão de 14-15/10
- **MULTIPLEBAGFETCH_FIX.md** - Fix detalhado do MultipleBagFetchException

### 🗑️ Limpeza

- Removido `HIDE_FROM_METADATA_EXAMPLES.md` (substituído por ANNOTATIONS_GUIDE.md)
- Movidos 6 arquivos .md da raiz para `docs/api/` e `docs/archive/`
- Raiz do projeto: 9 arquivos → 2 arquivos (redução de 77%)

---

## [1.1.0] - 2025-10-14

### ✨ Adicionado

- **CascadeUpdateHelper** - Helper genérico para relacionamentos 1:N
  - Suporta INSERT, UPDATE e DELETE em transação única
  - Reutilizável para qualquer relacionamento pai → filhos
  - Logs padronizados (📦, ➕, ✏️, 🗑️)
  - 3 métodos: `updateChildren()`, `updateChildrenWithInit()`, e versão simples
- **EventService.update()** - Agora usa CascadeUpdateHelper para categories
- **MetadataService** - Campo `labelField` adicionado ao metadata de cada entidade
- **@DisplayLabel** - Fix para garantir que campos apareçam sempre no formFields
- **Documentação completa reorganizada:**
  - `CASCADE_HELPER_README.md` - Quick reference
  - `CASCADE_UPDATE_HELPER_USAGE.md` - 5 exemplos completos
  - `CASCADE_UPDATE_1_N.md` - Detalhes técnicos
  - `DISPLAYLABEL_FORMFIELDS_FIX.md` - Correção do @DisplayLabel
  - `INDEX.md` - Índice completo de toda documentação
  - READMEs em cada subpasta (architecture/, api/, features/, etc.)

### 🔄 Modificado

- **EventService.update()** - Refatorado de ~80 linhas para ~15 usando helper
- **MetadataService** - Adicionado `findDisplayLabelField()` e `isDisplayLabelField()`
- **EntityMetadata** - Adicionado campo `labelField`
- **Documentação** - README principal completamente reescrito
- **Estrutura docs/** - Arquivos legados movidos para `docs/archive/`

### 🗂️ Organização

- Criada pasta `docs/archive/` com documentação legada
- Movidos 6 arquivos obsoletos da raiz para archive/
- Movida pasta `docs/metadata/` completa para archive/
- Criados READMEs em: architecture/, api/, features/, implementation/, backend/, archive/
- Criado INDEX.md com mapa completo da documentação

### 📚 Documentação

- **Consolidação:** Múltiplos docs sobre metadata unificados em `METADATA_ARCHITECTURE.md`
- **Novos guias:** 3 documentos completos sobre cascade updates
- **Índice:** Criado INDEX.md com guia de leitura por perfil (backend/frontend/PO/arquiteto)
- **Navegação:** Links cruzados entre documentos relacionados

### 🐛 Corrigido

- Campos com @DisplayLabel agora sempre aparecem no formFields
- labelField agora é incluído no metadata JSON
- Imports não utilizados removidos (java.util.Map em EventService)

---

## [1.0.0] - 2025-01-06

### ✨ Adicionado

- Sistema completo de Entity Filters
- @DisplayLabel annotation para autodiscovery
- EntityFilterHelper com reflection
- EntityFilterConfig DTO
- Metadata para Payment e EventCategory
- Documentação completa de arquitetura
- Guia de filtros da API

### 🔄 Modificado

- MetadataService totalmente refatorado
- FilterMetadata com campo entityConfig
- UserController retorna DTOs
- UserService com force loading de Organization
- Todos os repositories otimizados (40-50% menos código)

### 🗑️ Removido

- 60+ métodos redundantes em repositories
- Filtros desatualizados (search, eventType em Events)
- Métodos de query específicos substituídos por Specifications

### 🐛 Corrigido

- LazyInitializationException em /api/users
- Metadata desalinhado com Specifications
- Inconsistências em nomes de status

### ⚠️ Breaking Changes

- Events: `search` e `eventType` removidos
- Events: Adicionados `categoryId` e `city`
- Registrations: `CONFIRMED` → `ACTIVE`
- Registrations: Adicionados `eventId` e `userId`
- Users: `PARTICIPANT` → `USER`
- Users: Adicionados `role`, `organizationId`, `enabled`

---

## [0.9.0] - 2025-01-01

### ✨ Adicionado

- JPA Specifications pattern
- Metadata básico para Event, Registration, User
- PaginationConfig centralizado

### 🔄 Modificado

- Controllers usando Specifications
- Repositories com métodos customizados

---

## [0.8.0] - 2024-12-15

### ✨ Adicionado

- Entidades base (Event, User, Registration)
- Controllers REST básicos
- Authentication com Spring Security

---

## [Unreleased]

### 🚀 Planejado

- [ ] Campo renderAs em EntityFilterConfig
- [ ] Suporte a múltipla seleção em filtros
- [ ] Cache de metadata
- [ ] Filtros salvos por usuário
- [ ] Internacionalização (i18n)

---

## Tipos de Mudança

- **✨ Adicionado** - Novas features
- **🔄 Modificado** - Mudanças em funcionalidades existentes
- **🗑️ Removido** - Features removidas
- **🐛 Corrigido** - Bug fixes
- **⚠️ Breaking Changes** - Mudanças incompatíveis com versões anteriores
- **🚀 Planejado** - Features futuras
