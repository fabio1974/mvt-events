# Correção de Payload - API Organizations

## ✅ RESOLVIDO NO BACKEND

O backend foi atualizado para aceitar **AMBOS os formatos**:
- `"owner": "uuid-string"` (formato flat)
- `"owner": { "id": "uuid-string" }` (formato objeto)

Isso se aplica a: `owner`, `courier` (em employmentContracts) e `client` (em clientContracts).

---

## Formatos Aceitos

### Formato 1: Objeto com ID (recomendado)

```json
{
  "name": "Grupo da Iracema",
  "commissionPercentage": 5,
  "status": "ACTIVE",
  "owner": {
    "id": "5e74daa4-3db5-4c53-adeb-e199a51f9f07"
  },
  "employmentContracts": [
    {
      "courier": {
        "id": "6186c7af-2311-4756-bfc6-ce98bd31ed27"
      },
      "linkedAt": "2025-12-29T23:57:01.285086",
      "isActive": true
    }
  ],
  "clientContracts": [
    {
      "client": {
        "id": "c2222222-2222-2222-2222-222222222222"
      },
      "isPrimary": true,
      "status": "ACTIVE",
      "contractDate": "2025-12-29",
      "startDate": "2025-12-29",
      "endDate": null
    }
  ]
}
```

### Formato 2: UUID String flat (também aceito)

```json
{
  "name": "Grupo da Iracema",
  "owner": "5e74daa4-3db5-4c53-adeb-e199a51f9f07",
  "employmentContracts": [
    {
      "courier": "6186c7af-2311-4756-bfc6-ce98bd31ed27",
      "linkedAt": "2025-12-29T23:57:01.285086",
      "isActive": true
    }
  ],
  "clientContracts": [
    {
      "client": "c2222222-2222-2222-2222-222222222222",
      "isPrimary": true,
      "status": "ACTIVE",
      "contractDate": "2025-12-29",
      "startDate": "2025-12-29"
    }
  ]
}
```

---

## Campos Flexíveis

| Campo | Localização | Aceita String | Aceita Objeto |
|-------|-------------|--------------|---------------|
| `owner` | Raiz do objeto | ✅ `"uuid"` | ✅ `{ "id": "uuid" }` |
| `courier` | `employmentContracts[]` | ✅ `"uuid"` | ✅ `{ "id": "uuid" }` |
| `client` | `clientContracts[]` | ✅ `"uuid"` | ✅ `{ "id": "uuid" }` |

---

## Observações

1. **startDate obrigatório**: O campo `startDate` em `clientContracts` é obrigatório

2. **Datas**: Use formato ISO 8601
   - DateTime: `"2025-12-29T23:57:01.285086"`
   - Date only: `"2025-12-29"`

3. **Null vs vazio**: Para campos opcionais, use `null` em vez de string vazia `""`

---

## Endpoints

- `POST /api/organizations` - Criar organização
- `PUT /api/organizations/{id}` - Atualizar organização
