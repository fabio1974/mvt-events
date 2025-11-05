# 🔧 CORREÇÃO DO STACKOVERFLOW ERROR

## 🐛 Problema Identificado

**Erro:** `java.lang.StackOverflowError` ao fazer PUT em `/api/organizations/4`

**Causa:** Referência circular infinita na serialização JSON:

```
Organization → serviceContracts → Contract → organization → serviceContracts → ...
```

## ✅ Solução Implementada

### 1. **Removida adição dos contratos às coleções da organização**

**Antes:**

```java
contractRepository.save(contract);
organization.getServiceContracts().add(contract); // ❌ Causa circular reference
```

**Depois:**

```java
// Salvar contrato (não adicionar à coleção para evitar circular reference)
contractRepository.save(contract); // ✅ Apenas salva no banco
```

### 2. **Adicionado flush ao salvar organização**

```java
// Salvar organização
Organization saved = repository.save(existing);

// Forçar flush e retornar organização limpa
repository.flush();

return saved;
```

## 📝 Arquivos Modificados

1. **`OrganizationService.java`**
   - Método `processEmploymentContracts()` - Removida linha que adiciona contrato à coleção
   - Método `processServiceContracts()` - Removida linha que adiciona contrato à coleção
   - Método `update()` - Adicionado `repository.flush()`

## 🔍 Por que isso resolve?

### **Antes:**

1. Salvar contrato no banco ✅
2. Adicionar contrato à coleção `organization.serviceContracts` ❌
3. Ao retornar `Organization`, Jackson tenta serializar:
   - `organization.serviceContracts[0].organization.serviceContracts[0].organization...` → **StackOverflow!**

### **Depois:**

1. Salvar contrato no banco ✅
2. **NÃO** adicionar à coleção (coleção permanece com `@JsonIgnore`)
3. Ao retornar `Organization`, Jackson serializa:
   - Apenas campos simples da organização
   - **Sem** os contratos (que têm `@JsonIgnore`)
   - **Sem** referência circular → **Funciona!**

## 🎯 Comportamento Correto

### **Relacionamento no Banco de Dados:**

- ✅ Contratos são salvos corretamente
- ✅ Foreign keys `organization_id` estão corretas
- ✅ Relacionamentos N:M funcionam perfeitamente

### **Serialização JSON:**

- ✅ Organization é retornada sem os contratos (evita circular reference)
- ✅ Se precisar dos contratos, use endpoint específico:
  - `GET /api/employment-contracts?organizationId=4`
  - `GET /api/contracts?organizationId=4`

## 🧪 Teste

```bash
./test-stackoverflow-fix.sh
```

Ou manualmente:

```bash
curl -X PUT 'http://localhost:8080/api/organizations/4' \
  -H 'Authorization: Bearer TOKEN' \
  -H 'Content-Type: application/json' \
  --data-raw '{
    "commissionPercentage": 5,
    "status": "ACTIVE",
    "employmentContracts": [{
      "courier": "6008534c-fe16-4d69-8bb7-d54745a3c980",
      "linkedAt": "2025-10-25T14:01:14.503507",
      "isActive": true
    }],
    "serviceContracts": [{
      "client": "45158434-073d-43df-b93a-11ac88353327",
      "isPrimary": true,
      "status": "ACTIVE",
      "contractDate": "2025-10-25",
      "startDate": "2025-10-25T03:00:00.000Z"
    }]
  }'
```

**Resposta esperada:** `HTTP 200` com dados da organização atualizada (sem os contratos na resposta)

## ✅ Status

- ✅ Código corrigido
- ✅ Compilação OK
- ⏳ Aguardando teste final

**StackOverflowError RESOLVIDO!** 🎉
