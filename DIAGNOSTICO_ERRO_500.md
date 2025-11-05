# 🔍 DIAGNÓSTICO - ERRO 500 NO GET /api/organizations

## 📊 Status Atual

- ✅ Aplicação rodando em outro terminal (IntelliJ/VSCode)
- ❌ GET `/api/organizations?page=0&size=10` retorna HTTP 500
- ❌ Mensagem de erro: `null` (sem detalhes na resposta HTTP)

## 🔎 Erros Possíveis (baseado nas mudanças feitas)

### 1. **LazyInitializationException**

```
failed to lazily initialize a collection of role:
com.mvt.mvt_events.jpa.Organization.employmentContracts:
could not initialize proxy - no Session
```

**Causa:** `Hibernate.initialize()` não está funcionando no método `list()`

**Solução:** Verificar se o método está com `@Transactional(readOnly = true)`

---

### 2. **ConcurrentModificationException**

```
java.util.ConcurrentModificationException
    at java.base/java.util.ArrayList$Itr.checkForComodification
```

**Causa:** Iteração sobre `employmentContracts` ou `serviceContracts` que estão sendo modificados

**Solução:** Criar cópia da lista antes de iterar (já implementado, mas pode não ter sido recompilado)

---

### 3. **StackOverflowError**

```
java.lang.StackOverflowError
    at com.fasterxml.jackson.databind...
```

**Causa:** Referência circular ao serializar JSON (Organization → Contract → Organization → ...)

**Solução:** Verificar se `@JsonIgnore` está nos lugares corretos

---

### 4. **NullPointerException**

```
java.lang.NullPointerException
    at com.mvt.mvt_events.service.OrganizationService.list
```

**Causa:** Tentar inicializar coleção que está `null`

**Solução:** Adicionar verificação `if (org.getEmploymentContracts() != null)`

---

## 🛠️ O QUE VERIFICAR NOS LOGS

Procure por estas linhas no **console da aplicação**:

```
❌ Exception:
❌ Error:
❌ Caused by:
❌ at com.mvt.mvt_events.
```

## 📋 PRÓXIMOS PASSOS

1. **Copiar stack trace completo** do console da aplicação
2. **Identificar o erro específico** (LazyInit, Concurrent, StackOverflow, NPE)
3. **Aplicar correção apropriada**
4. **Recompilar** (se necessário) ou aplicar hot-reload
5. **Testar novamente**

## 🎯 CÓDIGO CRÍTICO PARA VERIFICAR

### OrganizationService.list() - Deve estar assim:

```java
@Transactional(readOnly = true)
public Page<Organization> list(Pageable pageable) {
    Page<Organization> organizations = repository.findAll(pageable);

    organizations.forEach(org -> {
        Hibernate.initialize(org.getCity());
        Hibernate.initialize(org.getEmploymentContracts());
        Hibernate.initialize(org.getServiceContracts());

        // CRÍTICO: Criar cópia para evitar ConcurrentModificationException
        if (org.getEmploymentContracts() != null) {
            new java.util.ArrayList<>(org.getEmploymentContracts()).forEach(ec -> {
                if (ec.getCourier() != null) {
                    Hibernate.initialize(ec.getCourier());
                }
            });
        }

        if (org.getServiceContracts() != null) {
            new java.util.ArrayList<>(org.getServiceContracts()).forEach(sc -> {
                if (sc.getClient() != null) {
                    Hibernate.initialize(sc.getClient());
                }
            });
        }
    });

    return organizations;
}
```

### OrganizationResponse - Deve estar assim:

```java
public OrganizationResponse(Organization organization) {
    this.id = organization.getId();
    this.name = organization.getName();
    // ... outros campos básicos

    // CRÍTICO: Verificar se coleções não são null
    if (organization.getEmploymentContracts() != null) {
        this.employmentContracts = organization.getEmploymentContracts().stream()
            .map(EmploymentContractResponse::new)
            .collect(Collectors.toList());
    }

    if (organization.getServiceContracts() != null) {
        this.serviceContracts = organization.getServiceContracts().stream()
            .map(ContractResponse::new)
            .collect(Collectors.toList());
    }
}
```

### Organization Entity - Verificar @JsonIgnore:

```java
@OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
@JsonIgnore  // ← CRÍTICO: Deve ter @JsonIgnore
private Set<EmploymentContract> employmentContracts = new HashSet<>();

@OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
@JsonIgnore  // ← CRÍTICO: Deve ter @JsonIgnore
private Set<Contract> serviceContracts = new HashSet<>();
```

---

## ⚡ AÇÃO IMEDIATA NECESSÁRIA

**Por favor, copie e cole aqui:**

1. As últimas 50 linhas do console onde a aplicação está rodando
2. Ou tire um screenshot do erro completo

Assim poderei identificar exatamente qual erro está acontecendo e aplicar a correção precisa! 🎯
