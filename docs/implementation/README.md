# 🔧 Implementação

Documentação técnica de implementações específicas.

---

## 📄 Documentos

### [MULTIPLEBAGFETCH_FIX.md](./MULTIPLEBAGFETCH_FIX.md) 🐛 **NOVO**

**Fix detalhado** do erro MultipleBagFetchException.

**Para:** Developers que encontram erro 500 em endpoints com múltiplos relacionamentos @OneToMany.

**Inclui:**

- Causa raiz do problema
- Solução implementada (@Fetch(FetchMode.SUBSELECT))
- Comparação de alternativas
- Impacto de performance
- Como testar

---

### [ANNOTATIONS_QUICK_REFERENCE.md](./ANNOTATIONS_QUICK_REFERENCE.md) ⚡ **NOVO**

**Cheat sheet** rápido para consulta durante desenvolvimento.

**Para:** Developers que já conhecem o básico e precisam de uma referência rápida.

**Inclui:**

- Matriz de uso de `@Visible`
- Template de nova entidade
- Combinações comuns
- Checklist rápido

---

### [ANNOTATIONS_GUIDE.md](./ANNOTATIONS_GUIDE.md) 📚 **NOVO**

**Guia completo** sobre annotations customizadas do metadata.

**Para:** Developers iniciantes no projeto que precisam entender como usar `@DisplayLabel`, `@Visible` e `@Computed`.

**Inclui:**

- Sintaxe e exemplos de cada annotation
- Casos de uso comuns
- Matriz de visibilidade
- Ordem de precedência
- Troubleshooting
- Exercícios práticos

---

### [CASCADE_HELPER_README.md](./CASCADE_HELPER_README.md) 🌟

**Quick reference** para usar o `CascadeUpdateHelper`.

**Para:** Developers que precisam implementar relacionamentos 1:N rapidamente.

---

### [CASCADE_UPDATE_HELPER_USAGE.md](./CASCADE_UPDATE_HELPER_USAGE.md)

**Guia completo** com exemplos práticos de uso do helper.

**Inclui:**

- Event → EventCategory
- Organization → User
- Order → OrderItem
- Post → Comment
- Playlist → Song

---

### [CASCADE_UPDATE_1_N.md](./CASCADE_UPDATE_1_N.md)

**Detalhes técnicos** de como funciona o cascade update.

**Inclui:**

- Regras de negócio (INSERT, UPDATE, DELETE)
- Implementação interna
- Transações
- Testes

---

### [DISPLAYLABEL_FORMFIELDS_FIX.md](./DISPLAYLABEL_FORMFIELDS_FIX.md)

Correção para garantir que campos com `@DisplayLabel` apareçam sempre no `formFields`.

---

### [STATUS.md](./STATUS.md)

**Status atual do projeto** - o que está implementado e o que falta.

---

### [CHANGELOG.md](./CHANGELOG.md)

**Histórico de mudanças** - log de implementações por data.

---

## 🚀 Quick Start - Cascade Update

### 1. Injetar o Helper

```java
@Service
@Transactional
public class MyService {
    private final CascadeUpdateHelper cascadeUpdateHelper;

    public MyService(CascadeUpdateHelper helper) {
        this.cascadeUpdateHelper = helper;
    }
}
```

### 2. Usar no Update

```java
public Parent update(Long id, Parent parentData) {
    Parent saved = repository.save(existing);

    if (parentData.getChildren() != null) {
        List<Child> existingChildren = childRepo.findByParentId(saved.getId());

        cascadeUpdateHelper.updateChildrenWithInit(
            saved,                    // Pai
            parentData.getChildren(), // Filhos do payload
            existingChildren,         // Filhos existentes
            Child::getId,             // ID extractor
            Child::setParent,         // Parent setter
            (e, p) -> {               // Update function
                e.setField1(p.getField1());
            },
            (c) -> {                  // Initializer
                c.setActive(true);
            },
            childRepo
        );
    }

    return saved;
}
```

📚 **Mais detalhes:** [CASCADE_HELPER_README.md](./CASCADE_HELPER_README.md)

---

## 🔗 Links Relacionados

- [Arquitetura](../architecture/METADATA_ARCHITECTURE.md) - Sistema geral
- [Backend](../backend/COMPUTED_FIELDS_IMPLEMENTATION.md) - Campos computados

---

**Voltar para:** [Documentação Principal](../README.md) | [Índice Completo](../INDEX.md)
