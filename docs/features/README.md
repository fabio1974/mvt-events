# 💡 Features

Documentação de features específicas do sistema.

---

## 📄 Documentos

### [ENTITY_FILTERS.md](./ENTITY_FILTERS.md)

**Filtros por relacionamento** (entity filters com typeahead).

**Tópicos:**

- Como funcionam os filtros por entidade
- Configuração de typeahead vs select
- Estrutura do `EntityFilterConfig`
- Exemplos de uso
- Integração com frontend

**Para quem:**

- Frontend developers (implementar UI)
- Backend developers (entender geração)
- UX designers (decisões de interface)

---

## 🔗 Links Relacionados

- [Arquitetura de Metadata](../architecture/METADATA_ARCHITECTURE.md) - Sistema completo
- [Guia de Filtros](../api/FILTERS_GUIDE.md) - Todos os filtros disponíveis

---

## 💡 Exemplo

### Filtro de Evento por Organização

**Metadata retornado:**

```json
{
  "name": "organization",
  "label": "Organização",
  "type": "entity",
  "field": "organization.id",
  "entityConfig": {
    "entityName": "organization",
    "endpoint": "/api/organizations",
    "labelField": "name",
    "valueField": "id",
    "renderAs": "typeahead",
    "searchable": true
  }
}
```

**Frontend implementa:**

- Input com autocomplete
- Busca dinâmica conforme usuário digita
- Seleção de opção com ID e label

---

**Voltar para:** [Documentação Principal](../README.md) | [Índice Completo](../INDEX.md)
