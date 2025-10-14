# 🗄️ Backend

Documentação de implementações específicas do backend.

---

## 📄 Documentos

### [COMPUTED_FIELDS_IMPLEMENTATION.md](./COMPUTED_FIELDS_IMPLEMENTATION.md)

**Implementação de campos computados** com a anotação `@Computed`.

**Tópicos:**

- Como criar campos computados
- Anotação `@Computed`
- Função de cálculo
- Dependências
- Exemplos práticos

**Exemplo:**

```java
@Entity
public class EventCategory {

    @Computed(
        function = "categoryName",
        dependencies = {"distance", "gender", "minAge", "maxAge"}
    )
    @DisplayLabel
    private String name;

    // Backend calcula automaticamente:
    // "10KM - Masculino - 18 a 35 anos"
}
```

---

## 🔗 Links Relacionados

- [Arquitetura de Metadata](../architecture/METADATA_ARCHITECTURE.md) - Como é extraído
- [Cascade Updates](../implementation/CASCADE_HELPER_README.md) - Relacionamentos 1:N

---

**Voltar para:** [Documentação Principal](../README.md) | [Índice Completo](../INDEX.md)
