# 📝 Changelog

Todas as mudanças notáveis deste projeto serão documentadas neste arquivo.

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
