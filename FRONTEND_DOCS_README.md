# 📦 Documentação para Frontend - Resumo

## 📁 Arquivos Criados

1. **[FRONTEND_API_UPDATE_GUIDE.md](./FRONTEND_API_UPDATE_GUIDE.md)** - Guia completo e detalhado (COMEÇAR AQUI!)
2. **[QUICK_START_API.md](./QUICK_START_API.md)** - Quick start com exemplos práticos ⚡
3. **[REACT_EXAMPLE.md](./REACT_EXAMPLE.md)** - Exemplo completo em React/TypeScript 🎨
4. **[API_TESTING_CURL.md](./API_TESTING_CURL.md)** - Testes com cURL para validação 🧪

---

## 🎯 Mudança Principal

### Campo `city` agora funciona! ✅

**Antes:** ❌ Não funcionava

```json
{
  "cityId": 964
}
```

**Agora:** ✅ Funciona

```json
{
  "city": {
    "id": 964
  }
}
```

---

## 🚀 Ações Necessárias no Frontend

### 1️⃣ **Atualizar Payload de Update**

```typescript
// ❌ ANTES
const payload = {
  name: formData.name,
  cityId: formData.cityId, // Não funciona
  organizationId: formData.orgId, // Não funciona
};

// ✅ AGORA
const payload = {
  name: formData.name,
  city: { id: formData.cityId }, // ✅ Funciona!
  organization: { id: formData.orgId }, // ✅ Funciona!
};
```

### 2️⃣ **Implementar Autocomplete de Cidades**

```typescript
// Buscar cidades
const cities = await fetch(`/api/cities/search?q=${query}`).then((r) =>
  r.json()
);

// Ao selecionar
setFormData({
  ...formData,
  cityId: selectedCity.id,
  cityName: selectedCity.name, // Para exibição
});
```

### 3️⃣ **Remover Campos Internos do Payload**

```typescript
// Não enviar:
delete payload.createdAt;
delete payload.updatedAt;
delete payload.tenantId;
```

---

## 📊 Endpoints Atualizados

| Método | Endpoint                           | Aceita                              |
| ------ | ---------------------------------- | ----------------------------------- |
| `PUT`  | `/api/events/{id}`                 | **Event** (entidade genérica) ✅    |
| `PUT`  | `/api/events/{id}/with-categories` | EventUpdateRequest (DTO específico) |

**Recomendação:** Use o endpoint padrão (`PUT /api/events/{id}`) que agora é genérico.

---

## 🎨 Metadata API

A API de metadados continua funcionando normalmente:

```typescript
const metadata = await fetch("/api/metadata/event").then((r) => r.json());

// metadata.formFields = campos do formulário
// metadata.tableFields = campos da tabela
// metadata.filters = filtros disponíveis

// Cada campo ENUM tem options traduzidas:
metadata.formFields.find((f) => f.name === "eventType").options;
// [
//   { label: "Corrida", value: "RUNNING" },
//   { label: "Ciclismo", value: "CYCLING" },
//   ...
// ]
```

---

## ✅ Checklist de Implementação

- [ ] Ler [QUICK_START_API.md](./QUICK_START_API.md) para entender as mudanças
- [ ] Ver exemplo completo em [REACT_EXAMPLE.md](./REACT_EXAMPLE.md)
- [ ] Atualizar função de update para usar formato `{city: {id}}`
- [ ] Implementar autocomplete de cidades
- [ ] Remover campos internos do payload
- [ ] Testar criação de evento
- [ ] Testar edição de evento
- [ ] Testar mudança de cidade
- [ ] Validar que cidade está sendo salva
- [ ] Atualizar testes automatizados

---

## 🐛 Debug

Se algo não funcionar:

1. **Verificar payload no Network tab** - deve ter `{city: {id: 964}}`
2. **Verificar console do backend** - logs detalhados de erros
3. **Testar com curl** - ver exemplos em QUICK_START_API.md
4. **Validar IDs** - city.id e organization.id devem existir no banco

---

## 📞 Suporte

- Documentação completa: [FRONTEND_API_UPDATE_GUIDE.md](./FRONTEND_API_UPDATE_GUIDE.md)
- Exemplos rápidos: [QUICK_START_API.md](./QUICK_START_API.md)
- Código React: [REACT_EXAMPLE.md](./REACT_EXAMPLE.md)

---

## 🎉 Benefícios

✅ **API Genérica** - sem DTOs específicos  
✅ **Metadata-Driven** - formulários automáticos  
✅ **Type-Safe** - TypeScript completo  
✅ **Consistente** - mesmo padrão para todas entidades  
✅ **Manutenível** - mudanças na entidade refletem automaticamente

---

**Data:** 11 de outubro de 2025  
**Versão:** 1.0.0  
**Backend:** Spring Boot 3.5.6

🚀 **Pronto para usar!**
