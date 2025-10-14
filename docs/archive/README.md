# 📦 Arquivo - Documentação Legada

Esta pasta contém documentação de versões anteriores do sistema que foram **substituídas** por versões mais atualizadas e consolidadas.

**Manter para referência histórica apenas. NÃO use como fonte de verdade.**

---

## ⚠️ Status

Todos os documentos aqui são **LEGADOS** e foram **substituídos** pela documentação atual em:

- `docs/architecture/` - Arquitetura atual
- `docs/implementation/` - Implementações atuais
- `docs/api/` - API atual

---

## 📁 Conteúdo

### Arquivos Obsoletos da Raiz

| Arquivo                            | Substituído por                                                                     |
| ---------------------------------- | ----------------------------------------------------------------------------------- |
| `BACKEND_RELATIONSHIP_METADATA.md` | [`architecture/METADATA_ARCHITECTURE.md`](../architecture/METADATA_ARCHITECTURE.md) |
| `ENUM_OPTIONS_IMPLEMENTATION.md`   | [`architecture/METADATA_ARCHITECTURE.md`](../architecture/METADATA_ARCHITECTURE.md) |
| `FORM_METADATA_IMPLEMENTATION.md`  | [`architecture/METADATA_ARCHITECTURE.md`](../architecture/METADATA_ARCHITECTURE.md) |
| `METADATA_GENERIC_SUMMARY.md`      | [`README.md`](../README.md)                                                         |
| `METADATA_UNIFICADO_RESUMO.md`     | [`README.md`](../README.md)                                                         |
| `SOLUTION_METADATA_GENERICA.md`    | [`architecture/METADATA_ARCHITECTURE.md`](../architecture/METADATA_ARCHITECTURE.md) |
| `EXECUTIVE_SUMMARY.md`             | Mudança específica antiga (11/10/2025) - cidade em eventos                          |
| `FRONTEND_DOCS_README.md`          | Índice de docs antigas - ver [`api/README.md`](../api/README.md)                    |

### Pasta `metadata/` (Legada)

Esta pasta inteira foi substituída por:

| Documento Legado                      | Substituído por                                                                       |
| ------------------------------------- | ------------------------------------------------------------------------------------- |
| `metadata/README.md`                  | [`architecture/METADATA_ARCHITECTURE.md`](../architecture/METADATA_ARCHITECTURE.md)   |
| `metadata/JPA_EXTRACTION.md`          | Incorporado em [`METADATA_ARCHITECTURE.md`](../architecture/METADATA_ARCHITECTURE.md) |
| `metadata/UNIFIED_ENDPOINT.md`        | Incorporado em [`METADATA_ARCHITECTURE.md`](../architecture/METADATA_ARCHITECTURE.md) |
| `metadata/TRADUÇÕES_COMPLETAS.md`     | Incorporado em [`METADATA_ARCHITECTURE.md`](../architecture/METADATA_ARCHITECTURE.md) |
| `metadata/CORREÇÕES_IMPLEMENTADAS.md` | [`implementation/CHANGELOG.md`](../implementation/CHANGELOG.md)                       |
| `metadata/STATUS_FINAL.md`            | [`implementation/STATUS.md`](../implementation/STATUS.md)                             |
| `metadata/CHECKLIST_FINAL.md`         | [`implementation/STATUS.md`](../implementation/STATUS.md)                             |
| `metadata/COMPARISON.md`              | Referência histórica                                                                  |
| `metadata/RESUMO_CORREÇÕES.md`        | [`implementation/STATUS.md`](../implementation/STATUS.md)                             |

---

## 🔄 Migração Realizada

**Data:** 14 de outubro de 2025

**Motivo:** Consolidar documentação fragmentada em documentos únicos e atualizados.

**Mudanças principais:**

1. ✅ Todos os documentos sobre metadata foram **consolidados** em `METADATA_ARCHITECTURE.md`
2. ✅ Status e changelog foram **atualizados** em `implementation/`
3. ✅ Estrutura de pastas foi **reorganizada** por tipo de conteúdo
4. ✅ Duplicações foram **removidas**
5. ✅ Índice completo foi **criado** (`INDEX.md`)

---

## 📚 Onde Encontrar a Documentação Atual

### Início

**→ [`docs/README.md`](../README.md)** - Documentação principal (comece aqui)

### Por Tópico

- **Arquitetura:** [`architecture/METADATA_ARCHITECTURE.md`](../architecture/METADATA_ARCHITECTURE.md)
- **API:** [`api/FILTERS_GUIDE.md`](../api/FILTERS_GUIDE.md)
- **Cascade Updates:** [`implementation/CASCADE_HELPER_README.md`](../implementation/CASCADE_HELPER_README.md)
- **Status:** [`implementation/STATUS.md`](../implementation/STATUS.md)
- **Changelog:** [`implementation/CHANGELOG.md`](../implementation/CHANGELOG.md)

### Índice Completo

**→ [`docs/INDEX.md`](../INDEX.md)** - Mapa completo de toda documentação

---

## 🗑️ Posso Deletar Esta Pasta?

**Recomendação:** **NÃO delete** ainda.

Mantenha por pelo menos 6 meses para:

- Referência histórica
- Comparação de mudanças
- Recuperação de informações que possam ter sido perdidas

Após esse período, pode considerar deletar se nenhum documento for mais necessário.

---

**Voltar para:** [Documentação Principal](../README.md) | [Índice Completo](../INDEX.md)
